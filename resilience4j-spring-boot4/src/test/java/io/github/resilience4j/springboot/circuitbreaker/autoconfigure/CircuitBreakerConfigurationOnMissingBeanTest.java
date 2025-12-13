package io.github.resilience4j.springboot.circuitbreaker.autoconfigure;

import io.github.resilience4j.springboot.TestUtils;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.spring6.circuitbreaker.configure.CircuitBreakerAspect;
import io.github.resilience4j.spring6.circuitbreaker.configure.CircuitBreakerAspectExt;
import io.github.resilience4j.spring6.circuitbreaker.configure.CircuitBreakerConfiguration;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.spring6.fallback.FallbackExecutor;
import io.github.resilience4j.spring6.spelresolver.SpelResolver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    HealthContributorAutoConfiguration.class,
    CircuitBreakerConfigurationOnMissingBeanTest.ConfigWithOverrides.class,
    CircuitBreakerAutoConfiguration.class,
})
@EnableConfigurationProperties(CircuitBreakerProperties.class)
public class CircuitBreakerConfigurationOnMissingBeanTest {

    @Autowired
    private ConfigWithOverrides configWithOverrides;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private CircuitBreakerAspect circuitBreakerAspect;

    @Autowired
    private EventConsumerRegistry<CircuitBreakerEvent> circuitEventConsumerBreakerRegistry;

    @Test
    public void testAllBeansFromCircuitBreakerConfigurationHasOnMissingBean()
        throws NoSuchMethodException {
        TestUtils.assertAnnotations(CircuitBreakerConfiguration.class, CircuitBreakerAutoConfiguration.class);
    }

    @Test
    public void testAllCircuitBreakerConfigurationBeansOverridden() {
        assertEquals(circuitBreakerRegistry, configWithOverrides.circuitBreakerRegistry);
        assertEquals(circuitBreakerAspect, configWithOverrides.circuitBreakerAspect);
        assertEquals(circuitEventConsumerBreakerRegistry,
            configWithOverrides.circuitEventConsumerBreakerRegistry);
    }

    @Configuration
    public static class ConfigWithOverrides {

        CircuitBreakerRegistry circuitBreakerRegistry;

        CircuitBreakerAspect circuitBreakerAspect;

        EventConsumerRegistry<CircuitBreakerEvent> circuitEventConsumerBreakerRegistry;

        @Bean
        public CircuitBreakerRegistry circuitBreakerRegistry() {
            circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
            return circuitBreakerRegistry;
        }

        @Bean
        public CircuitBreakerAspect circuitBreakerAspect(
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Autowired(required = false) List<CircuitBreakerAspectExt> circuitBreakerAspectExtList,
            FallbackExecutor fallbackExecutor,
            SpelResolver spelResolver) {
            circuitBreakerAspect = new CircuitBreakerAspect(new CircuitBreakerProperties(),
                circuitBreakerRegistry, circuitBreakerAspectExtList, fallbackExecutor, spelResolver);
            return circuitBreakerAspect;
        }

        @Bean
        public EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry() {
            circuitEventConsumerBreakerRegistry = new DefaultEventConsumerRegistry<>();
            return circuitEventConsumerBreakerRegistry;
        }
    }
}
