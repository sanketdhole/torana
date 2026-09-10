package com.phaselume.torana.connector.jdbc.query;

import com.phaselume.torana.core.exception.ConnectorException;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validates and guards SQL queries against disallowed statement types (e.g. DDL, destructive DML).
 */
public class SqlOperationGuard {

    private static final Pattern DISALLOWED_KEYWORDS = Pattern.compile(
            "\\b(DROP|ALTER|TRUNCATE|GRANT|REVOKE|EXEC|EXECUTE|SHUTDOWN)\\b",
            Pattern.CASE_INSENSITIVE
    );

    public void validate(String sql, List<String> allowedOperations) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new ConnectorException("jdbc", "SQL query cannot be null or empty");
        }

        String trimmedSql = sql.trim().toUpperCase(Locale.ROOT);

        // Disallow dangerous keywords
        if (DISALLOWED_KEYWORDS.matcher(trimmedSql).find()) {
            throw new ConnectorException("jdbc", "SQL statement contains prohibited keywords (DDL/destructive commands not allowed)");
        }

        List<String> allowedOps = (allowedOperations != null && !allowedOperations.isEmpty())
                ? allowedOperations
                : List.of("SELECT");

        String primaryCommand = extractCommand(trimmedSql);
        boolean isAllowed = allowedOps.stream().anyMatch(op -> op.equalsIgnoreCase(primaryCommand));

        if (!isAllowed) {
            throw new ConnectorException("jdbc", "SQL operation '" + primaryCommand + "' is not permitted. Allowed operations: " + allowedOps);
        }
    }

    private String extractCommand(String sql) {
        // Strip leading comments or whitespace
        String cleanSql = sql.replaceAll("^/\\*.*?\\*/", "").trim();
        String[] tokens = cleanSql.split("\\s+");
        return tokens.length > 0 ? tokens[0].toUpperCase(Locale.ROOT) : "";
    }
}
