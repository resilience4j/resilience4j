package io.github.resilience4j.springboot3.circuitbreaker.autoconfigure;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.spring6.circuitbreaker.configure.CircuitBreakerAspect;
import io.github.resilience4j.spring6.circuitbreaker.configure.CircuitBreakerAspectExt;
import io.github.resilience4j.spring6.circuitbreaker.configure.CircuitBreakerConfiguration;
import io.github.resilience4j.spring6.fallback.FallbackExecutor;
import io.github.resilience4j.spring6.spelresolver.SpelResolver;
import io.github.resilience4j.springboot3.TestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    HealthContributorAutoConfiguration.class,
    CircuitBreakerConfigurationOnMissingBeanTest.ConfigWithOverrides.class,
    CircuitBreakerAutoConfiguration.class,
    CircuitBreakerConfigurationOnMissingBean.class
})
@EnableConfigurationProperties(CircuitBreakerProperties.class)
class CircuitBreakerConfigurationOnMissingBeanTest {

    @Autowired
    private ConfigWithOverrides configWithOverrides;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private CircuitBreakerAspect circuitBreakerAspect;

    @Autowired
    private EventConsumerRegistry<CircuitBreakerEvent> circuitEventConsumerBreakerRegistry;

    @Test
    void testAllBeansFromCircuitBreakerConfigurationHasOnMissingBean()
        throws NoSuchMethodException {
        final Class<CircuitBreakerConfiguration> originalClass = CircuitBreakerConfiguration.class;
        final Class<CircuitBreakerConfigurationOnMissingBean> onMissingBeanClass = CircuitBreakerConfigurationOnMissingBean.class;
        TestUtils.assertAnnotations(originalClass, onMissingBeanClass);
    }

    @Test
    void testAllCircuitBreakerConfigurationBeansOverridden() {
        assertThat(configWithOverrides.circuitBreakerRegistry).isEqualTo(circuitBreakerRegistry);
        assertThat(configWithOverrides.circuitBreakerAspect).isEqualTo(circuitBreakerAspect);
        assertThat(configWithOverrides.circuitEventConsumerBreakerRegistry).isEqualTo(circuitEventConsumerBreakerRegistry);
    }

    @Configuration
    static class ConfigWithOverrides {

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
