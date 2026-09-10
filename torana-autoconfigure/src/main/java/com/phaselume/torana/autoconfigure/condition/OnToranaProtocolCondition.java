package com.phaselume.torana.autoconfigure.condition;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

/**
 * Condition evaluator for @ConditionalOnToranaProtocol.
 */
public class OnToranaProtocolCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnToranaProtocol.class.getName());
        if (attributes == null) {
            return ConditionOutcome.match();
        }

        String protocol = (String) attributes.get("value");
        String propertyName = "torana.protocols." + protocol.toLowerCase() + ".enabled";
        Boolean enabled = context.getEnvironment().getProperty(propertyName, Boolean.class, true);

        if (Boolean.TRUE.equals(enabled)) {
            return ConditionOutcome.match("Torana protocol [" + protocol + "] is enabled");
        } else {
            return ConditionOutcome.noMatch("Torana protocol [" + protocol + "] is disabled");
        }
    }
}
