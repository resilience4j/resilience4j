package io.github.resilience4j.circuitbreaker.autoconfigure;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerAspect;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerAspectExt;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfiguration;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.BDDAssertions.assertThat;
import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        CircuitBreakerConfigurationOnMissingBeanTest.ConfigWithOverrides.class,
        CircuitBreakerAutoConfiguration.class,
        CircuitBreakerConfigurationOnMissingBean.class
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

    @Configuration
    public static class ConfigWithOverrides {

        public CircuitBreakerRegistry circuitBreakerRegistry;

        public CircuitBreakerAspect circuitBreakerAspect;

        public EventConsumerRegistry<CircuitBreakerEvent> circuitEventConsumerBreakerRegistry;

        @Bean
        public CircuitBreakerRegistry circuitBreakerRegistry() {
            circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
            return circuitBreakerRegistry;
        }

        @Bean
        public CircuitBreakerAspect circuitBreakerAspect(CircuitBreakerRegistry circuitBreakerRegistry,
                                                         @Autowired(required = false) List<CircuitBreakerAspectExt> circuitBreakerAspectExtList) {
            circuitBreakerAspect = new CircuitBreakerAspect(new CircuitBreakerProperties(), circuitBreakerRegistry, circuitBreakerAspectExtList);
            return circuitBreakerAspect;
        }

        @Bean
        public EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry() {
            circuitEventConsumerBreakerRegistry = new DefaultEventConsumerRegistry<>();
            return circuitEventConsumerBreakerRegistry;
        }
    }

    @Test
    public void testAllBeansFromCircuitBreakerConfigurationHasOnMissingBean() throws NoSuchMethodException {
        final Class<CircuitBreakerConfiguration> originalClass = CircuitBreakerConfiguration.class;
        final Class<CircuitBreakerConfigurationOnMissingBean> onMissingBeanClass = CircuitBreakerConfigurationOnMissingBean.class;

        for (Method methodCircuitBreakerConfiguration : originalClass.getMethods()) {
            if (methodCircuitBreakerConfiguration.isAnnotationPresent(Bean.class)) {
                final Method methodOnMissing = onMissingBeanClass
                        .getMethod(methodCircuitBreakerConfiguration.getName(), methodCircuitBreakerConfiguration.getParameterTypes());

                assertThat(methodOnMissing.isAnnotationPresent(Bean.class)).isTrue();
                assertThat(methodOnMissing.isAnnotationPresent(ConditionalOnMissingBean.class)).isTrue();
            }
        }
    }

    @Test
    public void testAllCircuitBreakerConfigurationBeansOverridden() {
        assertEquals(circuitBreakerRegistry, configWithOverrides.circuitBreakerRegistry);
        assertEquals(circuitBreakerAspect, configWithOverrides.circuitBreakerAspect);
        assertEquals(circuitEventConsumerBreakerRegistry, configWithOverrides.circuitEventConsumerBreakerRegistry);
    }
}