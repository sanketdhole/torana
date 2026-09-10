package com.phaselume.torana.autoconfigure.condition;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

/**
 * Condition evaluator for @ConditionalOnToranaAuthzEngine.
 */
public class OnToranaAuthzEngineCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnToranaAuthzEngine.class.getName());
        if (attributes == null) {
            return ConditionOutcome.match();
        }

        String expectedEngine = (String) attributes.get("value");
        String actualEngine = context.getEnvironment().getProperty("torana.security.authz.engine", "opa");

        if (expectedEngine.equalsIgnoreCase(actualEngine)) {
            return ConditionOutcome.match("Torana authz engine matches [" + expectedEngine + "]");
        } else {
            return ConditionOutcome.noMatch("Torana authz engine is [" + actualEngine + "], expected [" + expectedEngine + "]");
        }
    }
}
