package com.phaselume.torana.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Auto-configuration enabling root ToranaProperties.
 */
@AutoConfiguration
@EnableConfigurationProperties(ToranaProperties.class)
public class ToranaPropertiesAutoConfiguration {
}
