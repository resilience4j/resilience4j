package io.github.resilience4j.timelimiter.configure;

import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.fallback.FallbackDecorators;
import io.github.resilience4j.spelresolver.SpelResolver;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.github.resilience4j.timelimiter.event.TimeLimiterEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Duration;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        TimeLimiterConfigurationSpringTest.ConfigWithOverrides.class
})
public class TimeLimiterConfigurationSpringTest {

    @Autowired
    private ConfigWithOverrides configWithOverrides;


    @Test
    public void testAllCircuitBreakerConfigurationBeansOverridden() {
        assertNotNull(configWithOverrides.timeLimiterRegistry);
        assertNotNull(configWithOverrides.timeLimiterAspect);
        assertNotNull(configWithOverrides.timeLimiterEventEventConsumerRegistry);
        assertNotNull(configWithOverrides.timeLimiterConfigurationProperties);
        assertEquals(1, configWithOverrides.timeLimiterConfigurationProperties.getConfigs().size());
    }


    @Configuration
    @ComponentScan({"io.github.resilience4j.timelimiter","io.github.resilience4j.fallback", "io.github.resilience4j.spelresolver"})
    public static class ConfigWithOverrides {

        private TimeLimiterRegistry timeLimiterRegistry;

        private TimeLimiterAspect timeLimiterAspect;

        private EventConsumerRegistry<TimeLimiterEvent> timeLimiterEventEventConsumerRegistry;

        private TimeLimiterConfigurationProperties timeLimiterConfigurationProperties;

        @Bean
        public TimeLimiterRegistry timeLimiterRegistry() {
            timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
            return timeLimiterRegistry;
        }

        @Bean
        public TimeLimiterAspect timeLimiterAspect(
            TimeLimiterRegistry timeLimiterRegistry,
            @Autowired(required = false) List<TimeLimiterAspectExt> timeLimiterAspectExtList,
            FallbackDecorators fallbackDecorators,
            SpelResolver spelResolver
        ) {
            timeLimiterAspect = new TimeLimiterAspect(timeLimiterRegistry, timeLimiterConfigurationProperties(), timeLimiterAspectExtList, fallbackDecorators, spelResolver);
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
