package com.phaselume.torana.security.authz.opa.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Map;

/**
 * Represents a single policy obligation returned by OPA alongside the access decision.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpaObligation {

    private String type; // e.g. "mask-field", "inject-header", "add-audit-tag", "row-filter"
    
    @Builder.Default
    private Map<String, Object> params = Collections.emptyMap();
}
