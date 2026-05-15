package io.github.resilience4j.spring6.timelimiter.configure;

import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.ContextAwareScheduledThreadPoolExecutor;
import io.github.resilience4j.spring6.fallback.FallbackExecutor;
import io.github.resilience4j.spring6.spelresolver.SpelResolver;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.github.resilience4j.timelimiter.event.TimeLimiterEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        TimeLimiterConfigurationSpringTest.ConfigWithOverrides.class
})
class TimeLimiterConfigurationSpringTest {

    @Autowired
    private ConfigWithOverrides configWithOverrides;

    @Test
    void testAllCircuitBreakerConfigurationBeansOverridden() {
        assertThat(configWithOverrides.timeLimiterRegistry).isNotNull();
        assertThat(configWithOverrides.timeLimiterAspect).isNotNull();
        assertThat(configWithOverrides.timeLimiterEventEventConsumerRegistry).isNotNull();
        assertThat(configWithOverrides.timeLimiterConfigurationProperties).isNotNull();
        assertThat(configWithOverrides.timeLimiterConfigurationProperties.getConfigs()).hasSize(1);
    }

    @Configuration
    @ComponentScan({"io.github.resilience4j.spring6.timelimiter","io.github.resilience4j.spring6.fallback", "io.github.resilience4j.spring6.spelresolver"})
    static class ConfigWithOverrides {

        private TimeLimiterRegistry timeLimiterRegistry;

        private TimeLimiterAspect timeLimiterAspect;

        private EventConsumerRegistry<TimeLimiterEvent> timeLimiterEventEventConsumerRegistry;

        private TimeLimiterConfigurationProperties timeLimiterConfigurationProperties;

        private ContextAwareScheduledThreadPoolExecutor contextAwareScheduledThreadPoolExecutor;

        @Bean
        public TimeLimiterRegistry timeLimiterRegistry() {
            timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
            return timeLimiterRegistry;
        }

        @Bean
        public TimeLimiterAspect timeLimiterAspect(
            TimeLimiterRegistry timeLimiterRegistry,
            @Autowired(required = false) List<TimeLimiterAspectExt> timeLimiterAspectExtList,
            FallbackExecutor fallbackExecutor,
            SpelResolver spelResolver
        ) {
            timeLimiterAspect = new TimeLimiterAspect(timeLimiterRegistry, timeLimiterConfigurationProperties(), timeLimiterAspectExtList, fallbackExecutor, spelResolver, contextAwareScheduledThreadPoolExecutor);
            return timeLimiterAspect;
        }

        @Bean
        public EventConsumerRegistry<TimeLimiterEvent> eventConsumerRegistry() {
            timeLimiterEventEventConsumerRegistry = new DefaultEventConsumerRegistry<>();
            return timeLimiterEventEventConsumerRegistry;
        }

        @Bean
        public TimeLimiterConfigurationProperties timeLimiterConfigurationProperties() {
            timeLimiterConfigurationProperties = new TimeLimiterConfigurationPropertiesTest();
            return timeLimiterConfigurationProperties;
        }

        private static class TimeLimiterConfigurationPropertiesTest extends TimeLimiterConfigurationProperties {

            TimeLimiterConfigurationPropertiesTest() {
                InstanceProperties instanceProperties = new InstanceProperties();
                instanceProperties.setBaseConfig("sharedConfig");
                instanceProperties.setTimeoutDuration(Duration.ofSeconds(3));
                getConfigs().put("sharedBackend", instanceProperties);
            }

        }
    }
}
