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

-- Remove events older than the sliding window
redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, now - window)

-- Count events in current window
local count = redis.call('ZCARD', KEYS[1])

if count < limit then
    -- Add this event
    redis.call('ZADD', KEYS[1], now, now .. ':' .. event_id)
    redis.call('PEXPIRE', KEYS[1], window)
    return {1, limit - count - 1, 0}  -- allowed (1), remaining, retry_after (0)
else
    -- Find oldest event in the active window to compute accurate retry-after seconds
    local oldest = redis.call('ZRANGE', KEYS[1], 0, 0, 'WITHSCORES')[2]
    local retry_after = 1
    if oldest then
        local oldest_ts = tonumber(oldest)
        retry_after = math.max(1, math.ceil((oldest_ts + window - now) / 1000))
    end
    return {0, 0, retry_after}  -- denied (0), 0 remaining, retry_after seconds
end
