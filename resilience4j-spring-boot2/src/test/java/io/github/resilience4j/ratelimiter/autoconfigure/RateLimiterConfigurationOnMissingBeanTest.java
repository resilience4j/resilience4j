package io.github.resilience4j.ratelimiter.autoconfigure;

import io.github.resilience4j.ratelimiter.configure.RateLimiterConfiguration;
import org.junit.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Method;

import static org.assertj.core.api.BDDAssertions.assertThat;

public class RateLimiterConfigurationOnMissingBeanTest {

    @Test
    public void testAllBeansFromCircuitBreakerConfigurationHasOnMissingBean() throws NoSuchMethodException {
        final Class<RateLimiterConfiguration> originalClass = RateLimiterConfiguration.class;
        final Class<RateLimiterConfigurationOnMissingBean> onMissingBeanClass = RateLimiterConfigurationOnMissingBean.class;

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