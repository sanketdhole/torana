package com.phaselume.torana.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ToranaAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ToranaPropertiesAutoConfiguration.class,
                    ToranaAutoConfiguration.class
            ));

    @Test
    void testAutoConfigurationLoadsDefaultBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ToranaProperties.class);
            assertThat(context).hasSingleBean(ToranaStartupValidator.class);

            ToranaProperties props = context.getBean(ToranaProperties.class);
            assertThat(props.getProtocols().getMcp().isEnabled()).isTrue();
            assertThat(props.getProtocols().getWebsocket().isEnabled()).isTrue();
        });
    }

    @Test
    void testCustomPropertiesBinding() {
        contextRunner
                .withPropertyValues(
                        "torana.protocols.mcp.enabled=false",
                        "torana.security.authz.engine=custom-engine"
                )
                .run(context -> {
                    ToranaProperties props = context.getBean(ToranaProperties.class);
                    assertThat(props.getProtocols().getMcp().isEnabled()).isFalse();
                    assertThat(props.getSecurity().getAuthz().getEngine()).isEqualTo("custom-engine");
                });
    }
}
