package io.github.resilience4j.ratelimiter;

import io.github.resilience4j.core.ConfigurationNotFoundException;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class RateLimiterRegistryTest {

    @Test
    public void testCreateWithConfigurationMap() {
        Map<String, RateLimiterConfig> configs = new HashMap<>();
        configs.put("default", RateLimiterConfig.ofDefaults());
        configs.put("custom", RateLimiterConfig.ofDefaults());

        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.of(configs);

        assertThat(rateLimiterRegistry.getDefaultConfig()).isNotNull();
        assertThat(rateLimiterRegistry.getConfiguration("custom")).isNotNull();
    }

    @Test
    public void testCreateWithConfigurationMapWithoutDefaultConfig() {
        Map<String, RateLimiterConfig> configs = new HashMap<>();
        configs.put("custom", RateLimiterConfig.ofDefaults());

        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.of(configs);

        assertThat(rateLimiterRegistry.getDefaultConfig()).isNotNull();
        assertThat(rateLimiterRegistry.getConfiguration("custom")).isNotNull();
    }

    @Test
    public void testAddConfiguration() {
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
        rateLimiterRegistry.addConfiguration("custom", RateLimiterConfig.custom().build());

        assertThat(rateLimiterRegistry.getDefaultConfig()).isNotNull();
        assertThat(rateLimiterRegistry.getConfiguration("custom")).isNotNull();
    }

    @Test
    public void testWithNotExistingConfig() {
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();

        assertThatThrownBy(() -> rateLimiterRegistry.rateLimiter("test", "doesNotExist"))
                .isInstanceOf(ConfigurationNotFoundException.class);
    }
}
