package com.phaselume.torana.autoconfigure.condition;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

/**
 * Condition evaluator for @ConditionalOnToranaAuthProvider.
 */
public class OnToranaAuthProviderCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnToranaAuthProvider.class.getName());
        if (attributes == null) {
            return ConditionOutcome.match();
        }

        String type = (String) attributes.get("type");
        String propertyName = "torana.security.authn.providers." + type.toLowerCase() + ".enabled";
        Boolean enabled = context.getEnvironment().getProperty(propertyName, Boolean.class);

        if (enabled == null) {
            // Check general auth enabled
            Boolean authEnabled = context.getEnvironment().getProperty("torana.security.authn.enabled", Boolean.class, true);
            if (Boolean.TRUE.equals(authEnabled)) {
                return ConditionOutcome.match("Torana auth is enabled by default for [" + type + "]");
            }
            return ConditionOutcome.noMatch("Torana auth is disabled");
        }

        if (Boolean.TRUE.equals(enabled)) {
            return ConditionOutcome.match("Torana auth provider [" + type + "] is enabled");
        } else {
            return ConditionOutcome.noMatch("Torana auth provider [" + type + "] is disabled");
        }
    }
}
