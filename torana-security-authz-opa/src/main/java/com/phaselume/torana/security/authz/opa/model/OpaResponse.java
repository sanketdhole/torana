package com.phaselume.torana.security.authz.opa.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Top-level response returned by OPA's Data API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpaResponse {

    @JsonProperty("decision_id")
    @JsonAlias({"decisionId"})
    private String decisionId;

    private OpaResult result;

    public boolean isAllowed() {
        return result != null && result.isAllow();
    }

    public String getDenyReason() {
        return result != null ? result.getDenyReason() : null;
    }

    public List<OpaObligation> getObligations() {
        return result != null && result.getObligations() != null ? result.getObligations() : Collections.emptyList();
    }
}
