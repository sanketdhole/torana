package com.phaselume.torana.autoconfigure.condition;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

/**
 * Condition evaluator for @ConditionalOnToranaConnector.
 */
public class OnToranaConnectorCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnToranaConnector.class.getName());
        if (attributes == null) {
            return ConditionOutcome.match();
        }

        String connectorType = (String) attributes.get("type");
        String propertyName = "torana.connectors." + connectorType.toLowerCase() + ".enabled";
        Boolean enabled = context.getEnvironment().getProperty(propertyName, Boolean.class, true);

        if (Boolean.TRUE.equals(enabled)) {
            return ConditionOutcome.match("Torana connector [" + connectorType + "] is enabled");
        } else {
            return ConditionOutcome.noMatch("Torana connector [" + connectorType + "] is disabled");
        }
    }
}
