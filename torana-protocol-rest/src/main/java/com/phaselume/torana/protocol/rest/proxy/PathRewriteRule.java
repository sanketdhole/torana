package com.phaselume.torana.protocol.rest.proxy;

import org.springframework.util.AntPathMatcher;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encapsulates a single path rewrite rule. Supports Ant-style path patterns and regular expressions.
 */
public class PathRewriteRule {

    private final String fromPattern;
    private final String toReplacement;
    private final boolean isRegex;
    private final Pattern compiledPattern;
    private final AntPathMatcher antPathMatcher;

    public PathRewriteRule(String fromPattern, String toReplacement, boolean isRegex) {
        this.fromPattern = fromPattern != null ? fromPattern : "";
        this.toReplacement = toReplacement != null ? toReplacement : "";
        this.isRegex = isRegex;
        if (isRegex) {
            this.compiledPattern = Pattern.compile(this.fromPattern);
            this.antPathMatcher = null;
        } else {
            this.compiledPattern = null;
            this.antPathMatcher = new AntPathMatcher();
        }
    }

    public static PathRewriteRule ant(String fromPattern, String toReplacement) {
        return new PathRewriteRule(fromPattern, toReplacement, false);
    }

    public static PathRewriteRule regex(String fromRegex, String toReplacement) {
        return new PathRewriteRule(fromRegex, toReplacement, true);
    }

    public static PathRewriteRule stripPrefix(String prefix) {
        String cleanPrefix = prefix.endsWith("/**") ? prefix.substring(0, prefix.length() - 3) : prefix;
        if (cleanPrefix.endsWith("/*")) {
            cleanPrefix = cleanPrefix.substring(0, cleanPrefix.length() - 2);
        }
        if (!cleanPrefix.startsWith("/")) {
            cleanPrefix = "/" + cleanPrefix;
        }
        return new PathRewriteRule(
                cleanPrefix.endsWith("/**") ? cleanPrefix : cleanPrefix + "/**",
                cleanPrefix,
                false
        );
    }

    /**
     * Checks if the incoming request path matches this rewrite rule.
     */
    public boolean matches(String path) {
        if (path == null) {
            return false;
        }
        if (isRegex) {
            return compiledPattern.matcher(path).find();
        }
        return antPathMatcher.match(fromPattern, path);
    }

    /**
     * Applies the rewrite rule to the given path.
     */
    public String rewrite(String path) {
        if (path == null || !matches(path)) {
            return path;
        }

        if (isRegex) {
            Matcher matcher = compiledPattern.matcher(path);
            return matcher.replaceAll(toReplacement);
        }

        // Ant-style rewriting
        if (fromPattern.endsWith("/**") && toReplacement.endsWith("/**")) {
            String fromBase = fromPattern.substring(0, fromPattern.length() - 3);
            String toBase = toReplacement.substring(0, toReplacement.length() - 3);
            if (path.startsWith(fromBase)) {
                String remainder = path.substring(fromBase.length());
                if (!remainder.startsWith("/") && !toBase.endsWith("/")) {
                    remainder = "/" + remainder;
                }
                return toBase + remainder;
            }
        }

        // Prefix strip mode
        if (fromPattern.endsWith("/**") && !toReplacement.contains("*")) {
            String fromBase = fromPattern.substring(0, fromPattern.length() - 3);
            if (path.startsWith(fromBase)) {
                String remainder = path.substring(fromBase.length());
                return remainder.startsWith("/") ? remainder : "/" + remainder;
            }
        }

        return path;
    }

    public String getFromPattern() {
        return fromPattern;
    }

    public String getToReplacement() {
        return toReplacement;
    }

    public boolean isRegex() {
        return isRegex;
    }
}
