package io.github.resilience4j.timelimiter;

import io.github.resilience4j.core.ConfigurationNotFoundException;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class TimeLimiterRegistryTest {

    @Test
    public void testCreateWithConfigurationMap() {
        Map<String, TimeLimiterConfig> configs = new HashMap<>();
        configs.put("default", TimeLimiterConfig.ofDefaults());
        configs.put("custom", TimeLimiterConfig.ofDefaults());

        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.of(configs);

        assertThat(timeLimiterRegistry.getDefaultConfig()).isNotNull();
        assertThat(timeLimiterRegistry.getConfiguration("custom")).isNotNull();
    }

    @Test
    public void testCreateWithConfigurationMapWithoutDefaultConfig() {
        Map<String, TimeLimiterConfig> configs = new HashMap<>();
        configs.put("custom", TimeLimiterConfig.ofDefaults());

        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.of(configs);

        assertThat(timeLimiterRegistry.getDefaultConfig()).isNotNull();
        assertThat(timeLimiterRegistry.getConfiguration("custom")).isNotNull();
    }

    @Test
    public void testAddConfiguration() {
        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
        timeLimiterRegistry.addConfiguration("custom", TimeLimiterConfig.custom().build());

        assertThat(timeLimiterRegistry.getDefaultConfig()).isNotNull();
        assertThat(timeLimiterRegistry.getConfiguration("custom")).isNotNull();
    }

    @Test
    public void testWithNotExistingConfig() {
        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();

        assertThatThrownBy(() -> timeLimiterRegistry.timeLimiter("test", "doesNotExist"))
                .isInstanceOf(ConfigurationNotFoundException.class);
    }
}
