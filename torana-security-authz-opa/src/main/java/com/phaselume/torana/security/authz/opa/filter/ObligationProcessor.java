package com.phaselume.torana.security.authz.opa.filter;

import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.security.authz.opa.model.OpaObligation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Translates raw OPA policy obligations into strongly-typed {@link AgentContext.Obligations}.
 */
public class ObligationProcessor {

    /**
     * Converts a list of OPA obligations into {@link AgentContext.Obligations}.
     */
    public AgentContext.Obligations process(List<OpaObligation> opaObligations) {
        if (opaObligations == null || opaObligations.isEmpty()) {
            return AgentContext.Obligations.empty();
        }

        Map<String, String> rowFilters = new HashMap<>();
        Map<String, String> columnMasks = new HashMap<>();
        List<String> allowedPrefixes = new ArrayList<>();
        Map<String, Object> additional = new HashMap<>();
        Integer maxRows = null;

        for (OpaObligation ob : opaObligations) {
            if (ob == null || ob.getType() == null) {
                continue;
            }

            String type = ob.getType().toLowerCase();
            Map<String, Object> params = ob.getParams() != null ? ob.getParams() : Map.of();

            switch (type) {
                case "mask-field", "mask_field", "mask", "column-mask" -> {
                    String field = getString(params, "field", "column");
                    String mask = getString(params, "mask", "rule");
                    if (field != null) {
                        columnMasks.put(field, mask != null ? mask : "REDACT");
                    }
                }
                case "row-filter", "row_filter", "filter" -> {
                    String field = getString(params, "field", "column");
                    String value = getString(params, "value", "match");
                    if (field != null && value != null) {
                        rowFilters.put(field, value);
                    }
                }
                case "max-rows", "max_rows", "limit" -> {
                    Object val = params.get("limit");
                    if (val instanceof Number num) {
                        maxRows = num.intValue();
                    } else if (val instanceof String s) {
                        try {
                            maxRows = Integer.parseInt(s);
                        } catch (NumberFormatException ignored) {}
                    }
                }
                case "path-prefix", "allowed-prefix" -> {
                    String prefix = getString(params, "prefix", "path");
                    if (prefix != null) {
                        allowedPrefixes.add(prefix);
                    }
                }
                default -> {
                    // Custom obligation (e.g. inject-header, add-audit-tag)
                    additional.put(ob.getType(), params);
                }
            }
        }

        return AgentContext.Obligations.builder()
                .rowFilters(rowFilters)
                .columnMasks(columnMasks)
                .maxRows(maxRows)
                .allowedPathPrefixes(allowedPrefixes)
                .additionalObligations(additional)
                .build();
    }

    private static String getString(Map<String, Object> map, String key1, String key2) {
        Object val = map.get(key1);
        if (val == null && key2 != null) {
            val = map.get(key2);
        }
        return val != null ? val.toString() : null;
    }
}
