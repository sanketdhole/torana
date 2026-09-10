package com.phaselume.torana.security.authz.opa.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Encapsulates the JSON payload sent to OPA's Data API (POST /v1/data/{package}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpaRequest {

    private Map<String, Object> input;
}
