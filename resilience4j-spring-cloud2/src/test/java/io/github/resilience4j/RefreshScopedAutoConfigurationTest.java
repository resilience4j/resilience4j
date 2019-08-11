package io.github.resilience4j;

import io.github.resilience4j.bulkhead.autoconfigure.BulkheadAutoConfiguration;
import io.github.resilience4j.bulkhead.autoconfigure.RefreshScopedBulkheadAutoConfiguration;
import io.github.resilience4j.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.circuitbreaker.autoconfigure.RefreshScopedCircuitBreakerAutoConfiguration;
import io.github.resilience4j.ratelimiter.autoconfigure.RateLimiterAutoConfiguration;
import io.github.resilience4j.ratelimiter.autoconfigure.RefreshScopedRateLimiterAutoConfiguration;
import io.github.resilience4j.retry.autoconfigure.RefreshScopedRetryAutoConfiguration;
import io.github.resilience4j.retry.autoconfigure.RetryAutoConfiguration;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.type.MethodMetadata;

import static org.junit.Assert.assertTrue;

public class RefreshScopedAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    public void refreshScopedBulkheadRegistry() {
        contextRunner
                .withConfiguration(AutoConfigurations.of(
                        RefreshScopedBulkheadAutoConfiguration.class, BulkheadAutoConfiguration.class))
                .run(context -> {
                    testRefreshScoped(context, "bulkheadRegistry");
                    testRefreshScoped(context, "threadPoolBulkheadRegistry");
        });
    }

    @Test
    public void refreshScopedCircuitBreakerRegistry() {
        contextRunner
                .withConfiguration(AutoConfigurations.of(
                        RefreshScopedCircuitBreakerAutoConfiguration.class, CircuitBreakerAutoConfiguration.class))
                .run(context -> testRefreshScoped(context, "circuitBreakerRegistry"));
    }

    @Test
    public void refreshScopedRateLimiterRegistry() {
        contextRunner
                .withConfiguration(AutoConfigurations.of(
                        RefreshScopedRateLimiterAutoConfiguration.class, RateLimiterAutoConfiguration.class))
                .run(context -> testRefreshScoped(context, "rateLimiterRegistry"));
    }

    @Test
    public void refreshScopedRetryRegistry() {
        contextRunner
                .withConfiguration(AutoConfigurations.of(
                        RefreshScopedRetryAutoConfiguration.class, RetryAutoConfiguration.class))
                .run(context -> testRefreshScoped(context, "retryRegistry"));
    }

    private static void testRefreshScoped(AssertableApplicationContext context, String beanName) {
        BeanDefinition beanDefinition = context.getBeanFactory().getBeanDefinition(beanName);
        MethodMetadata beanMethod = (MethodMetadata) beanDefinition.getSource();

        assertTrue(beanMethod.isAnnotated(RefreshScope.class.getName()));
    }
}
