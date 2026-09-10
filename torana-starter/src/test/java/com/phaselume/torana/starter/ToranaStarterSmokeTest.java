package com.phaselume.torana.starter;

import com.phaselume.torana.autoconfigure.ToranaAutoConfiguration;
import com.phaselume.torana.autoconfigure.ToranaProperties;
import com.phaselume.torana.autoconfigure.ToranaPropertiesAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ToranaStarterSmokeTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ToranaPropertiesAutoConfiguration.class,
                    ToranaAutoConfiguration.class
            ));

    @Test
    void testStarterAutoConfigurationContext() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ToranaProperties.class);
            ToranaProperties props = context.getBean(ToranaProperties.class);
            assertThat(props.getProtocols().getMcp().isEnabled()).isTrue();
            assertThat(props.getSecurity().getAuthn().isEnabled()).isTrue();
        });
    }
}
