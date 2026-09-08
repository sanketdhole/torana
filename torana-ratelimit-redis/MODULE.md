# torana-ratelimit-redis — Distributed Rate Limiter

## Responsibility

Implements **distributed, multi-strategy rate limiting** using Redis as the shared counter store. Rate limiting is evaluated **before** OPA authorization and **before** pipeline execution — it is the first line of defense against overload, ensuring that even unauthenticated flood attempts are shed cheaply.

The implementation uses **atomic Lua scripts** executed on Redis to ensure correctness under concurrent distributed load (no race conditions between check and increment).

## Algorithm: Sliding Window Counter

The sliding window algorithm avoids the "burst at boundary" problem of fixed windows:

```
Current window count = COUNT(events in last N seconds)
If count < limit → allow (increment counter, set TTL)
If count >= limit → reject (return Retry-After)
```

Implemented as a Redis Sorted Set per key, where each member is `{timestamp}:{uuid}` and the score is the Unix timestamp in milliseconds. Expired members are pruned atomically in the same Lua script.

## Key Strategies

| Strategy | Redis Key Pattern | Use Case |
|----------|-------------------|----------|
| `by-user` | `torana:rl:{route}:{userId}` | Per-user quotas (most common) |
| `by-api-key` | `torana:rl:{route}:{keyHash}` | Service-to-service API key quotas |
| `by-tenant` | `torana:rl:{route}:{tenantId}` | Tenant-level resource quotas |
| `by-ip` | `torana:rl:{route}:{clientIp}` | Abuse prevention (anonymous traffic) |
| `by-route` | `torana:rl:{route}:global` | Global route capacity cap |

Multiple strategies can be stacked: e.g., `by-user` AND `by-tenant` both checked — request allowed only if both pass.

## Key Classes

### `model/`

| Class | Description |
|-------|-------------|
| `RateLimitPolicy` | YAML-bound policy: `requests-per-minute`, `burst`, `key-by` strategies list, `on-exceed` action, header exposure config, `window-size` (default 60s). |
| `RateLimitResult` | Check result: `allowed` boolean, `remaining` count, `retryAfterSeconds` long, `keyUsed` string. |
| `RateLimitKey` | Computed key: strategy name + route ID + discriminator value (user ID, tenant ID, etc.). |

### `redis/`

| Class | Description |
|-------|-------------|
| `RedisRateLimiter` | Implements `RateLimiter` SPI. For each strategy in the policy, calls `SlidingWindowLuaScript.execute()`. Returns `Mono<RateLimitDecision>` — the most restrictive result across all strategies. |
| `SlidingWindowLuaScript` | Encapsulates the Lua script that atomically: (1) removes expired members from the sorted set, (2) counts current members, (3) if under limit adds new member + sets TTL, (4) returns `[allowed, remaining, ttl]`. |
| `RateLimitKeyResolver` | Resolves the discriminator for each strategy from `AgentContext`: extracts `userId` from `ToranaAuthentication`, `tenantId`, `clientIp` from request headers. |
| `RateLimitPolicyRegistry` | Map of `policyName → RateLimitPolicy`. Built from `ToranaProperties.rateLimits`. |

### `filter/`

| Class | Description |
|-------|-------------|
| `RateLimitWebFilter` | Spring `WebFilter`. Runs before `AuthenticationWebFilter` (to shed unauthenticated flood). Resolves the route's `rate-limit-ref` policy. Calls `RedisRateLimiter.check()`. On `allowed=false`: returns 429 with `Retry-After` header. On `allowed=true`: adds `X-RateLimit-*` headers and continues. |
| `RateLimitResponseWriter` | Writes the 429 response body: `{"error": "rate_limit_exceeded", "retryAfter": N, "limit": M}`. Also writes `Retry-After: N` and `X-RateLimit-Limit: M`, `X-RateLimit-Remaining: 0` headers. |

## Lua Script (sliding window — stored in `src/main/resources/scripts/`)

```lua
-- sliding_window_rate_limit.lua
-- KEYS[1] = rate limit key (sorted set)
-- ARGV[1] = window size in milliseconds
-- ARGV[2] = max requests per window
-- ARGV[3] = current timestamp in milliseconds
-- ARGV[4] = unique event ID

local now = tonumber(ARGV[3])
local window = tonumber(ARGV[1])
local limit = tonumber(ARGV[2])
local event_id = ARGV[4]

-- Remove events older than the window
redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, now - window)

-- Count events in current window
local count = redis.call('ZCARD', KEYS[1])

if count < limit then
    -- Add this event
    redis.call('ZADD', KEYS[1], now, now .. ':' .. event_id)
    redis.call('PEXPIRE', KEYS[1], window)
    return {1, limit - count - 1, 0}  -- allowed, remaining, retry_after
else
    -- Find oldest event to compute retry-after
    local oldest = redis.call('ZRANGE', KEYS[1], 0, 0, 'WITHSCORES')[2]
    local retry_after = math.ceil((tonumber(oldest) + window - now) / 1000)
    return {0, 0, retry_after}  -- denied, 0 remaining, retry_after seconds
end
```

## YAML Configuration

```yaml
torana:
  rate-limits:
    standard:
      requests-per-minute: 120
      burst: 20                  # first N requests in a fresh window skip counting
      window-size: 60s
      key-by:
        - by-user
        - by-route              # global cap even if user quota not exceeded
      on-exceed: reject          # reject | queue (Phase 2)
      headers:
        expose: true

    llm-tier:
      requests-per-minute: 10
      key-by:
        - by-tenant
      on-exceed: reject

    public:
      requests-per-minute: 5
      key-by:
        - by-ip
      on-exceed: reject
```

## Development Phases

### Phase 1D — Core Rate Limiting
- [ ] Implement `RateLimitPolicy`, `RateLimitResult`, `RateLimitKey` model
- [ ] Write `sliding_window_rate_limit.lua` script
- [ ] Implement `SlidingWindowLuaScript` (load + execute via Reactive Redis `execute(RedisScript)`)
- [ ] Implement `RateLimitKeyResolver`
- [ ] Implement `RedisRateLimiter`
- [ ] Implement `RateLimitWebFilter` + `RateLimitResponseWriter`
- [ ] Integration test (Testcontainers Redis): 10 rpm limit → 11th request → 429 with correct `Retry-After`

### Phase 2A — Multi-strategy Stacking
- [ ] Support multiple `key-by` strategies in a single policy
- [ ] Return most restrictive result across all strategies

### Phase 4 — Token-level Limiting
- [ ] Add `tokens-per-minute` policy field (LLM token consumption tracking)
- [ ] `LlmCallStep` reports token usage back to `RateLimitWebFilter` after response completes
- [ ] Post-request token counter update (async, non-blocking)

## Package Layout

```
com.phaselume.torana.ratelimit
├── model/
│   ├── RateLimitPolicy.java
│   ├── RateLimitResult.java
│   └── RateLimitKey.java
├── redis/
│   ├── RedisRateLimiter.java
│   ├── SlidingWindowLuaScript.java
│   ├── RateLimitKeyResolver.java
│   └── RateLimitPolicyRegistry.java
└── filter/
    ├── RateLimitWebFilter.java
    └── RateLimitResponseWriter.java
```
