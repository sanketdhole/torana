package com.phaselume.torana.security.authz.opa.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Inner result payload returned by OPA's Data API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpaResult {

    @JsonProperty("allow")
    @Builder.Default
    private boolean allow = false;

    @JsonProperty("deny_reason")
    @JsonAlias({"denyReason", "reason", "deny_msg"})
    private String denyReason;

    @Builder.Default
    private List<OpaObligation> obligations = Collections.emptyList();

    @Builder.Default
    private Map<String, Object> metadata = Collections.emptyMap();
}
