package torana.authz

import future.keywords.in

default allow = false

# Default allow for authenticated agents with valid tenant context
allow {
    input.subject.authenticated == true
    input.resource.routeId != ""
    not is_blacklisted
}

# Blacklisted operations check
is_blacklisted {
    input.action.name == "DANGEROUS_OP"
}

# Return obligations (rate limits, audits, headers)
obligations["rate_limit"] = 100 {
    allow
}

obligations["audit_required"] = true {
    allow
}
