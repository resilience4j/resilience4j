package io.github.resilience4j.circuitbreaker.autoconfigure;

import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfiguration;
import org.junit.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Method;

import static org.assertj.core.api.BDDAssertions.assertThat;

public class CircuitBreakerConfigurationOnMissingBeanTest {

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
}