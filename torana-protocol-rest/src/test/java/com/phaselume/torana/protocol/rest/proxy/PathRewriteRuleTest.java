package com.phaselume.torana.protocol.rest.proxy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathRewriteRuleTest {

    @Test
    void testAntPathRewrite() {
        PathRewriteRule rule = PathRewriteRule.ant("/api/v1/hr/**", "/internal/hr/**");

        assertTrue(rule.matches("/api/v1/hr/employees/123"));
        assertEquals("/internal/hr/employees/123", rule.rewrite("/api/v1/hr/employees/123"));
    }

    @Test
    void testPrefixStrip() {
        PathRewriteRule rule = PathRewriteRule.stripPrefix("/api/v1");

        assertTrue(rule.matches("/api/v1/chat/completions"));
        assertEquals("/chat/completions", rule.rewrite("/api/v1/chat/completions"));
    }

    @Test
    void testRegexPathRewrite() {
        PathRewriteRule rule = PathRewriteRule.regex("^/v([0-9]+)/users/(.+)$", "/legacy/v$1/accounts/$2");

        assertTrue(rule.matches("/v2/users/john_doe"));
        assertEquals("/legacy/v2/accounts/john_doe", rule.rewrite("/v2/users/john_doe"));
        assertFalse(rule.matches("/other/path"));
    }
}
