# Torana Enterprise AI Gateway: Standard Architecture Blueprint

## Executive Overview
This blueprint establishes **Torana** as the open standard gateway for LLMs and AI Agents interacting with enterprise cloud networks. It extends the core stateless microservice architecture into a **multi-tenant, user-context-propagating, fine-grained access-controlled broker** for enterprise databases, object storage, internal APIs, and AI models.

---

## 1. High-Level Module Evolution & Architecture

### 1.1 Existing vs. Evolved Module Matrix

| Original Module | Evolved Role in Standard | Key Enhancements |
|---|---|---|
| `torana-core` | Standard Domain & Context Kernel | Houses `TenantContext`, `UserContext`, `SecurityContext`, immutable request models, and primary SPI definitions. |
| `torana-protocol-mcp` | **Primary LLM Interface** | Full JSON-RPC 2.0 / MCP spec compliance, tool filtering per user/tenant, context metadata extraction (`_meta.user_token`). |
| `torana-protocol-rest` / `ws` | Secondary Agent Ingress | OAuth2 On-Behalf-Of (OBO) token exchange, SSE streaming, WebSocket bidirectional user channels. |
| `torana-protocol-grpc` | High-Throughput Inter-Service Ingress | Protobuf contracts, gRPC metadata interceptors propagating tenant and user headers. |
| `torana-security-authn` | Multi-Tenant Authn & Token Exchange | Tenant IDP routing, JWT validation with multiple issuers, API key hashing, mTLS client cert validation. |
| `torana-security-authz-opa` | User-Aware ABAC / RBAC Engine | Evaluates policies based on `{tenant, user, agent, resource, action, context}`; outputs row filters and column masks. |
| `torana-security-vault` | Dynamic Credential Broker | Brokering ephemeral user-scoped credentials via Vault Dynamic Secrets and AWS STS `AssumeRole`. |
| `torana-connector-jdbc` | User-Scoped SQL Gateway | Reactive R2DBC connection wrapper setting transaction-scoped session variables (`SET LOCAL app.user_id = ?`) for RLS. |
| **`torana-tenant` (New)** | Tenant Lifecycle & Isolation Engine | Tenant resolution from subdomain/headers/claims, tenant quota management, dynamic tenant config loader. |
| **`torana-context-propagation` (New)**| Reactive Context Propagation | Propagates `UserContext` and `TenantContext` across Project Reactor threads, async steps, and external connectors. |
| **`torana-security-datamask` (New)** | Dynamic Data Masking & Redaction | AST-based query predicate injection, reactive stream column masking (hash, redact, regex, partial), S3 path restriction. |
| **`torana-sdk` (New)** | Enterprise Extension SPI | Public API contracts for third-party enterprises to build custom connectors, evaluators, and pipeline steps without forking. |

---

### 1.2 System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                             AI CLIENTS                                                 │
│   Claude Desktop / Cursor / IDEs         Custom Enterprise Agents         OpenAI / Anthropic Assistants│
└───────────────────────────────────────────────────┬────────────────────────────────────────────────────┘
                                                    │ MCP (JSON-RPC 2.0) / REST / WebSockets / gRPC
                                                    │ [Bearer User JWT / MCP _meta / Tenant Header]
┌───────────────────────────────────────────────────▼────────────────────────────────────────────────────┐
│                                           TORANA GATEWAY                                               │
│                                                                                                        │
│ ┌────────────────────────────────────────────────────────────────────────────────────────────────────┐ │
│ │ 1. INGRESS & CONTEXT EXTRACTION LAYER                                                              │ │
│ │  • MCP Protocol Adapter (Primary)  • REST / WebSocket Adapter  • gRPC Adapter                      │ │
│ │  • Context Resolver: Extracts Tenant ID + User ID + Delegated Claims from token / headers          │ │
│ │  • Reactor Context Binder: Sets TenantContext & UserContext into Reactive Mono/Flux pipeline       │ │
│ └─────────────────────────────────────────────────┬──────────────────────────────────────────────────┘ │
│                                                   │                                                    │
│ ┌─────────────────────────────────────────────────▼──────────────────────────────────────────────────┐ │
│ │ 2. MULTI-TENANT IDENTITY & SECURITY LAYER                                                          │ │
│ │  • Tenant IDP Resolver: Routes JWT verification to tenant-specific IdP (Okta, Keycloak, Azure AD)  │ │
│ │  • User Authentication & Agent Delegation Verification (RFC 8693 On-Behalf-Of validation)          │ │
│ │  • Distributed Tenant Rate Limiter: Per-tenant / per-user token and request quotas via Redis       │ │
│ └─────────────────────────────────────────────────┬──────────────────────────────────────────────────┘ │
│                                                   │                                                    │
│ ┌─────────────────────────────────────────────────▼──────────────────────────────────────────────────┐ │
│ │ 3. FINE-GRAINED USER-AWARE POLICY ENGINE                                                           │ │
│ │  • OPA / Rego Policy Engine evaluating: (Tenant, User, Agent, Tool/Resource, Environment)          │ │
│ │  • Policy Decision Output:                                                                         │ │
│ │    ├─ Allow / Deny Verdict                                                                         │ │
│ │    ├─ Row-Level Security Rules (SQL WHERE predicates / Tenant partition filters)                   │ │
│ │    ├─ Column Masking Directives (SHA-256, Redact, Partial Masking, Drop Fields)                   │ │
│ │    └─ S3/NFS Allowed Path Prefixes (e.g., s3://tenant-bucket/users/${user.id}/*)                   │ │
│ └─────────────────────────────────────────────────┬──────────────────────────────────────────────────┘ │
│                                                   │                                                    │
│ ┌─────────────────────────────────────────────────▼──────────────────────────────────────────────────┐ │
│ │ 4. PER-USER CREDENTIAL BROKERING & EXECUTION PIPELINE                                              │ │
│ │  • Ephemeral Credential Broker:                                                                    │ │
│ │    ├─ HashiCorp Vault: Generates dynamic, user-scoped DB credentials or lease tokens               │ │
│ │    ├─ AWS STS: AssumeRole with User Session Tags (TenantId, UserId, Department)                    │ │
│ │    └─ R2DBC Session Configurator: Injects session vars (SET LOCAL app.user_id = 'alice')           │ │
│ │  • Execution Pipeline:                                                                             │ │
│ │    ├─ Query Pre-Processing: AST SQL Rewriting / S3 Path Validation                                 │ │
│ │    ├─ Backend Connector Execution (JDBC, NoSQL, S3, NFS, HTTP, LiteLLM)                            │ │
│ │    └─ Data Masking & Post-Processing: Reactive stream masking of sensitive attributes              │ │
│ └─────────────────────────────────────────────────┬──────────────────────────────────────────────────┘ │
│                                                   │                                                    │
│ ┌─────────────────────────────────────────────────▼──────────────────────────────────────────────────┐ │
│ │ 5. ENTERPRISE EXTENSION SDK (SPI PLUGINS)                                                          │ │
│ │  • Custom Connector SPI  • Custom Policy Evaluator SPI  • Custom Context Resolver SPI              │ │
│ └────────────────────────────────────────────────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────┬────────────────────────────────────────────────────┘
                                                    │
             ┌──────────────────────┬───────────────┴───────┬──────────────────────┐
             ▼                      ▼                       ▼                      ▼
   ┌───────────────────┐  ┌───────────────────┐   ┌───────────────────┐  ┌───────────────────┐
   │ Enterprise RDBMS  │  │ Enterprise NoSQL  │   │ Cloud Storage     │  │ AI Models (LLMs)  │
   │ PostgreSQL / MySQL│  │ MongoDB/Cassandra │   │ AWS S3 / MinIO    │  │ LiteLLM Router    │
   │ (Postgres RLS)    │  │ (Tenant filtering)│   │ (User-prefix IAM) │  │ (OpenAI, Bedrock) │
   └───────────────────┘  └───────────────────┘   └───────────────────┘  └───────────────────┘
```

---

## 2. End-to-End User Identity & Context Propagation Flow

```
[LLM Provider / Agent]
        │
        │ 1. Request: MCP tools/call { "name": "query_orders", "arguments": { ... } }
        │    Headers / _meta: {
        │       "Authorization": "Bearer <user_delegation_jwt>",
        │       "X-Torana-Tenant": "acme-corp"
        │    }
        ▼
[Torana Ingress: torana-protocol-mcp]
        │
        │ 2. Extract Token & Metadata
        │    Delegates to UserContextResolver
        ▼
[User Context Resolver: torana-context-propagation]
        │
        │ 3. Resolve Tenant Configuration (from 'acme-corp' config)
        │    Validate JWT against Tenant IdP (JWKS: https://login.acmecorp.com/.well-known/jwks.json)
        │    Extract Claims:
        │      • sub: "usr_99214"
        │      • email: "alice@acme.com"
        │      • roles: ["sales_manager", "eu_region"]
        │      • tenant_id: "acme-corp"
        │    Build Immutable Context: UserContext + TenantContext
        │    Inject into Project Reactor context: Mono.deferContextual(...)
        ▼
[Policy Engine: torana-security-authz-opa]
        │
        │ 4. Send Evaluation Query to OPA:
        │    Input: {
        │      "tenant": "acme-corp",
        │      "user": { "id": "usr_99214", "roles": ["sales_manager", "eu_region"] },
        │      "agent": { "id": "sales-agent-v1", "client_id": "claude-desktop" },
        │      "resource": "database.orders",
        │      "action": "SELECT"
        │    }
        │
        │ 5. OPA Decision Returned:
        │    {
        │      "allow": true,
        │      "row_filter": "region = 'EU' AND tenant_id = 'acme-corp'",
        │      "column_masks": {
        │        "customer_credit_card": "REDACT",
        │        "customer_ssn": "PARTIAL_LAST_4",
        │        "customer_email": "HASH_SHA256"
        │      },
        │      "max_rows": 500
        │    }
        ▼
[Per-User Credential Brokering: torana-security-vault]
        │
        │ 6. Acquire Execution Context:
        │    Option A (PostgreSQL with Native RLS):
        │      Obtain pool connection, execute in transaction:
        │        SET LOCAL app.current_tenant = 'acme-corp';
        │        SET LOCAL app.current_user = 'usr_99214';
        │        SET LOCAL app.current_user_roles = 'sales_manager,eu_region';
        │    Option B (Vault Dynamic Role):
        │      Lease ephemeral DB user: 'v-token-alice-3h91k' with 5m TTL.
        ▼
[Execution & Query Rewriter: torana-connector-jdbc + torana-security-datamask]
        │
        │ 7. Execute Query against Enterprise DB with AST Rewriting / RLS:
        │    Original: SELECT order_id, customer_name, customer_ssn, customer_email, amount, region FROM orders;
        │    Rewritten (if non-RLS DB):
        │      SELECT order_id, customer_name, customer_ssn, customer_email, amount, region
        │      FROM orders
        │      WHERE (region = 'EU' AND tenant_id = 'acme-corp')
        │      LIMIT 500;
        ▼
[Data Masking Engine: torana-security-datamask]
        │
        │ 8. Stream Results through Reactive Field Masker:
        │    • customer_credit_card -> "[REDACTED]"
        │    • customer_ssn ("123-45-6789") -> "***-**-6789"
        │    • customer_email ("alice@corp.com") -> "e3b0c44298fc1c149afbf4c8996fb924..."
        ▼
[MCP Response Packaging: torana-protocol-mcp]
        │
        │ 9. Return JSON-RPC Tool Result to AI Agent:
        │    { "content": [ { "type": "text", "text": "[{\"order_id\":101,...}]" } ], "isError": false }
        ▼
[LLM Agent Receives Safe, Filtered, Masked Data]
```

---

## 3. Declarative Configuration Schema

### 3.1 `tenants.yaml` — Multi-Tenant Definition
```yaml
torana:
  multi-tenancy:
    enabled: true
    default-tenant: "shared-default"
    resolution-strategy: "HEADER_THEN_JWT_THEN_SUBDOMAIN"
    header-name: "X-Torana-Tenant"
    subdomain-domain-suffix: ".gateway.torana.ai"

  tenants:
    - id: "acme-corp"
      name: "Acme Corporation"
      enabled: true
      isolation-level: "LOGICAL_MULTI_TENANT" # Options: LOGICAL_MULTI_TENANT, DEDICATED_DATABASE, DEDICATED_INFRASTRUCTURE

      authn:
        issuer-uri: "https://login.acme.com/oauth2/v1"
        jwks-uri: "https://login.acme.com/oauth2/v1/keys"
        client-id: "torana-gateway-acme"
        user-id-claim: "sub"
        roles-claim: "groups"
        email-claim: "email"
        department-claim: "dept"

      quotas:
        requests-per-minute: 5000
        llm-tokens-per-month: 50000000
        max-concurrent-queries: 50

      allowed-llm-models:
        - "gpt-4o"
        - "claude-3-5-sonnet"
        - "llama-3.3-70b-instruct"

      credential-broker:
        provider: "vault" # Options: vault, aws-sts, static-secret-store
        vault:
          mount-path: "database"
          role-template: "acme-{{user.department}}-role"
        aws-sts:
          role-arn: "arn:aws:iam::123456789012:role/ToranaAcmeDataRole"
          session-duration-seconds: 900
          session-tags:
            TenantId: "acme-corp"
            UserId: "{{user.id}}"
            Department: "{{user.department}}"

    - id: "globex-corp"
      name: "Globex Industries"
      enabled: true
      isolation-level: "DEDICATED_DATABASE"
      authn:
        issuer-uri: "https://auth.globex.com"
        jwks-uri: "https://auth.globex.com/.well-known/jwks.json"
        user-id-claim: "preferred_username"
        roles-claim: "roles"
```

---

### 3.2 `user-policies.yaml` — Fine-Grained User-Aware Policies
```yaml
torana:
  policies:
    # -------------------------------------------------------------------------
    # Policy: Sales Orders Access Policy
    # -------------------------------------------------------------------------
    - name: "sales-orders-user-policy"
      description: "Restricts sales order querying based on user territory and masks PII"
      target:
        connectors: ["enterprise-postgres-jdbc"]
        resources: ["database.sales_orders", "table.orders"]
        tools: ["query_sales_db", "fetch_order_details"]

      rules:
        # Rule 1: Sales Representatives (can only see their own territory)
        - match:
            tenant: "acme-corp"
            user-roles-contain-any: ["sales_rep", "account_executive"]
          allow: true
          row-security:
            strategy: "PREDICATE_INJECTION"
            sql-predicate: "territory_id = '{{user.attributes.territory}}' AND tenant_id = '{{tenant.id}}'"
          data-masking:
            columns:
              customer_ssn:
                type: "REDACT"
                replacement: "[CONFIDENTIAL]"
              customer_credit_card:
                type: "PARTIAL_MASK"
                keep-last: 4
                mask-char: "*"
              revenue_amount:
                type: "PASS_THROUGH"
              profit_margin:
                type: "DROP_COLUMN"

        # Rule 2: Sales Directors (can see all EU data, unmasked revenue)
        - match:
            tenant: "acme-corp"
            user-roles-contain-any: ["sales_director", "vp_sales"]
          allow: true
          row-security:
            strategy: "DATABASE_RLS_SESSION"
            session-variables:
              "app.current_tenant": "{{tenant.id}}"
              "app.current_user": "{{user.id}}"
              "app.user_role": "director"
          data-masking:
            columns:
              customer_credit_card:
                type: "PARTIAL_MASK"
                keep-last: 4
              customer_ssn:
                type: "DROP_COLUMN"

    # -------------------------------------------------------------------------
    # Policy: Cloud Storage S3 User Path Scoping
    # -------------------------------------------------------------------------
    - name: "s3-user-document-policy"
      description: "Restricts S3 read/write tools to the user's home folder and shared department folders"
      target:
        connectors: ["aws-s3-connector"]
        resources: ["s3://*"]
        tools: ["read_s3_file", "list_s3_bucket", "upload_s3_file"]

      rules:
        - match:
            tenant: "acme-corp"
            authenticated: true
          allow: true
          path-restrictions:
            allowed-prefixes:
              - "s3://acme-enterprise-data/tenants/{{tenant.id}}/users/{{user.id}}/*"
              - "s3://acme-enterprise-data/tenants/{{tenant.id}}/departments/{{user.department}}/*"
              - "s3://acme-enterprise-data/public/*"
            denied-prefixes:
              - "s3://acme-enterprise-data/tenants/{{tenant.id}}/users/{{user.id}}/confidential_payroll/*"
```

---

### 3.3 `mcp-tools.yaml` — User-Aware MCP Tool Declarations
```yaml
torana:
  mcp:
    server-info:
      name: "Torana Enterprise Gateway"
      version: "1.0.0"

    tools:
      - name: "query_customer_database"
        description: "Executes parameterized analytics queries against the customer database. Access is scoped to your user permissions."
        connector-ref: "enterprise-postgres-jdbc"
        parameters:
          type: "object"
          properties:
            sql_query:
              type: "string"
              description: "The SQL SELECT statement to execute"
            max_results:
              type: "integer"
              default: 100
              maximum: 1000
          required: ["sql_query"]
        user-context-injection:
          inject-tenant-parameter: true
          inject-user-parameter: true
        security:
          require-user-authentication: true
          allowed-operations: ["SELECT_ONLY"]

      - name: "read_enterprise_document"
        description: "Reads a document from cloud storage. Path is validated against user entitlement."
        connector-ref: "aws-s3-connector"
        parameters:
          type: "object"
          properties:
            file_uri:
              type: "string"
              description: "Full S3 URI (e.g., s3://acme-enterprise-data/...)"
          required: ["file_uri"]
        security:
          require-user-authentication: true
```

---

## 4. Per-User Credential Brokering Deep Dive

### 4.1 HashiCorp Vault Integration
Torana interfaces with HashiCorp Vault using short-lived dynamic credentials rather than persistent service accounts:
1. **Dynamic DB User Lease**: When `UserContext` arrives with `department: finance`, Torana calls Vault endpoint `/v1/database/creds/acme-finance-role`.
2. **TTL Bound**: Vault provisions a temporary PostgreSQL user with a 15-minute lease and grants permissions mapped to that role.
3. **Automatic Revocation**: When the request pipeline finishes or lease expires, Vault drops the ephemeral user.

### 4.2 AWS IAM STS AssumeRole with Session Tags
For cloud resources (S3, DynamoDB, Bedrock, AWS Aurora):
1. Torana holds an IAM gateway identity with `sts:AssumeRole`.
2. Upon user request, Torana invokes `sts:AssumeRole` on the tenant target role, passing:
   - `RoleSessionName`: `Torana-User-${user.id}`
   - `Tags`: `[{"Key": "TenantId", "Value": "${tenant.id}"}, {"Key": "UserId", "Value": "${user.id}"}, {"Key": "Dept", "Value": "${user.department}"}]`
3. Target S3 Bucket Policy uses condition keys:
   ```json
   {
     "Effect": "Allow",
     "Action": ["s3:GetObject"],
     "Resource": "arn:aws:s3:::acme-data/tenants/${aws:PrincipalTag/TenantId}/users/${aws:PrincipalTag/UserId}/*"
   }
   ```

### 4.3 Database Native Row-Level Security (PostgreSQL / MySQL)
When using shared pooled database connections:
1. Borrow a physical connection from the R2DBC / JDBC connection pool.
2. Open a transaction and execute:
   ```sql
   SET LOCAL app.current_tenant = 'acme-corp';
   SET LOCAL app.current_user = 'usr_99214';
   SET LOCAL app.current_user_roles = 'sales_rep,eu_region';
   ```
3. PostgreSQL evaluates RLS policies attached to tables:
   ```sql
   CREATE POLICY order_user_isolation_policy ON orders
     FOR SELECT
     USING (tenant_id = current_setting('app.current_tenant') AND (
       user_id = current_setting('app.current_user') OR
       'vp_sales' = ANY(string_to_array(current_setting('app.current_user_roles'), ','))
     ));
   ```
4. On transaction commit/rollback, connection returns to pool in clean state.

---

## 5. Enterprise Extension Mechanism (SPI & Zero-Fork Plugin Architecture)

Torana provides a typed **Java SPI framework** (`torana-sdk`) so enterprises can add proprietary connectors, custom token validators, or in-house policy evaluators as JAR dependencies without touching the Torana core code.

### 5.1 Extension Contracts in `torana-sdk`

```java
package com.phaselume.torana.sdk.spi;

import com.phaselume.torana.sdk.model.*;
import reactor.core.publisher.Mono;

/**
 * SPI for creating custom backend connectors (e.g., SAP ERP, Salesforce, Mainframe).
 */
public interface ConnectorSPI {
    String getType(); // e.g., "sap-rfc", "salesforce-soql"
    Mono<ConnectorResponse> execute(ConnectorRequest request, ExecutionContext context);
    Mono<HealthStatus> healthCheck();
}

/**
 * SPI for resolving custom user identity tokens (e.g., Proprietary Enterprise Headers, SSO session cookies).
 */
public interface UserContextResolverSPI {
    int getOrder();
    Mono<UserContext> resolve(ClientRequest request, TenantContext tenantContext);
}

/**
 * SPI for external policy decision points (e.g., Custom Axiomatics, PlainID, PingAuthorize).
 */
public interface PolicyEvaluatorSPI {
    Mono<PolicyDecision> evaluate(PolicyEvaluationContext context);
}

/**
 * SPI for custom data masking transformations (e.g., Format-Preserving Encryption, Protegrity).
 */
public interface DataMaskingProviderSPI {
    String getMaskingType(); // e.g., "FPE_TOKENIZE", "REGEX_NAME"
    Object mask(Object rawValue, MaskingRule rule, UserContext userContext);
}
```

### 5.2 Zero-Fork Enterprise Plugin Registration
An enterprise builds a standard JAR (`acme-sap-connector-1.0.0.jar`):
1. Implements `ConnectorSPI`:
   ```java
   public class SapRfcConnector implements ConnectorSPI { ... }
   ```
2. Registers Spring AutoConfiguration in `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:
   ```
   com.acme.torana.plugin.SapConnectorAutoConfiguration
   ```
3. Drops the JAR into Torana's classpath / Docker image (`/plugins/`). Torana automatically detects and activates it.

---

## 6. Phased Implementation Roadmap to an Open Enterprise Standard

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                 PHASED EVOLUTION ROADMAP                                         │
├───────────────────┬───────────────────┬───────────────────┬───────────────────┬──────────────────┤
│     PHASE 1       │      PHASE 2      │      PHASE 3      │      PHASE 4      │     PHASE 5      │
│  Multi-Tenancy &  │  User-Aware AuthZ │ Per-User Secrets  │   Extension SDK   │  Open Standard   │
│   User Context    │  & Data Masking   │   & MCP Primary   │   & Plugin Model  │  & Distribution  │
├───────────────────┼───────────────────┼───────────────────┼───────────────────┼──────────────────┤
│ • TenantContext & │ • OPA policy AST  │ • HashiCorp Vault │ • torana-sdk      │ • Open Gateway   │
│   UserContext     │   generation      │   dynamic role    │   module with SPI │   Standard Spec  │
│ • Tenant-aware    │ • Row predicate   │   generation      │ • Plugin loader & │ • Conformance    │
│   JWT validator   │   injection       │ • AWS STS user    │   dynamic bean    │   Test Suite     │
│ • Reactive context│ • Column masking  │   session tags    │   registration    │ • Helm Charts,   │
│   propagation     │   pipeline        │ • PostgreSQL RLS  │ • Sample Custom   │   Docker Compose │
│ • Header/claim    │ • S3 path prefix  │   session vars    │   Connector (SAP/ │ • Docs & starter │
│   extraction      │   validator       │ • MCP JSON-RPC    │   Salesforce)     │   archetype      │
│                   │                   │   streamlined     │                   │                  │
└───────────────────┴───────────────────┴───────────────────┴───────────────────┴──────────────────┘
```

### Phase Breakdown

#### Phase 1: Multi-Tenancy & User Context Propagation
- Implement `TenantContext`, `UserContext`, and `SecurityContext` in `torana-core`.
- Implement `torana-context-propagation` using Reactor `ContextView` and SubscriberContext hooks.
- Create multi-tenant JWT decoder supporting dynamic tenant JWKS resolution.

#### Phase 2: User-Aware Authorization & Dynamic Data Masking
- Upgrade `torana-security-authz-opa` to receive combined `(Tenant, User, Agent, Action, Resource)` payload.
- Create `torana-security-datamask` with AST-based SQL filter generator and reactive column masker (Redact, Hash, Partial Mask).
- Implement S3 prefix evaluator matching `${tenant.id}` and `${user.id}`.

#### Phase 3: Per-User Credential Brokering & MCP-First Protocol
- Enhance `torana-security-vault` with dynamic user role leasing and AWS STS session tagging.
- Wire PostgreSQL connection pool wrapper in `torana-connector-jdbc` for `SET LOCAL app.current_user`.
- Elevate `torana-protocol-mcp` as the primary interface with user-aware tool schemas and dynamic permission filtering.

#### Phase 4: Enterprise Extension SDK (`torana-sdk`)
- Extract standalone `torana-sdk` module with pure interfaces (`ConnectorSPI`, `PolicyEvaluatorSPI`, `UserContextResolverSPI`, `DataMaskingProviderSPI`).
- Implement Spring Boot auto-configuration import scanning for 3rd-party JARs.
- Build reference plugins (e.g., Salesforce SOQL connector, Custom FPE encryption masker).

#### Phase 5: Open Standard, Conformance Suite & Community Ecosystem
- Publish open gateway formal specification (Markdown + OpenAPI + JSON Schema for MCP).
- Build automated Conformance Test Suite verifying multi-tenant isolation and context leak prevention.
- Create enterprise deployment packages (Helm, Kubernetes Operator, Docker Compose, Terraform).
- Add developer guide, contribution guidelines, and GitHub template repositories.
