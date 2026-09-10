package torana.v1

import future.keywords.in
import future.keywords.if

# ── Default deny ────────────────────────────────────────────────────
default allow := false

# ── Main allow rule ─────────────────────────────────────────────────
allow if {
    not route_in_deny_list
    not tenant_mismatch
    has_required_scope
    has_required_role
}

# ── Scope check ─────────────────────────────────────────────────────
has_required_scope if {
    count(input.route.required_scopes) == 0
}

has_required_scope if {
    count(input.route.required_scopes) > 0
    some scope in input.route.required_scopes
    scope in input.principal.scopes
}

# ── Role check ──────────────────────────────────────────────────────
has_required_role if {
    count(input.route.required_roles) == 0
}

has_required_role if {
    count(input.route.required_roles) > 0
    some role in input.route.required_roles
    role in input.principal.roles
}

# ── Route deny list ─────────────────────────────────────────────────
route_in_deny_list if {
    data.deny_list
    input.route.id in data.deny_list
}

# ── Tenant isolation ────────────────────────────────────────────────
tenant_mismatch if {
    input.route.tenant_id != ""
    input.principal.tenant_id != ""
    input.route.tenant_id != input.principal.tenant_id
}

# ── Obligations ─────────────────────────────────────────────────────
obligations := obs if {
    allow
    obs := [
        {"type": "mask-field", "params": {"field": "ssn", "mask": "REDACT"}},
        {"type": "inject-header", "params": {"header": "X-Torana-Policy", "value": "v1-evaluated"}}
    ]
} else := []

# ── Deny reason ─────────────────────────────────────────────────────
deny_reason := msg if {
    not allow
    route_in_deny_list
    msg := "Route is disabled"
} else := msg if {
    not allow
    tenant_mismatch
    msg := "Tenant mismatch"
} else := "Insufficient permissions"
