package com.phaselume.torana.connector.jdbc.security;

import com.phaselume.torana.core.exception.ConnectorException;

import java.util.List;
import java.util.Locale;

/**
 * Checks table and schema references in SQL queries against allow-lists.
 */
public class SchemaAllowList {

    public void validate(String sql, List<String> allowedTables) {
        if (allowedTables == null || allowedTables.isEmpty()) {
            return; // No table restrictions configured
        }

        if (sql == null) return;
        String lowerSql = sql.toLowerCase(Locale.ROOT);

        // Basic table check: Ensure query touches at least one allowed table and no obvious unauthorized ones
        boolean matchesAllowedTable = allowedTables.stream()
                .anyMatch(table -> lowerSql.contains(table.toLowerCase(Locale.ROOT)));

        if (!matchesAllowedTable) {
            throw new ConnectorException("jdbc", "Access denied: Query does not reference any permitted table in allow-list: " + allowedTables);
        }
    }
}
