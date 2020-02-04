package io.github.resilience4j;

import io.github.resilience4j.bulkhead.autoconfigure.BulkheadAutoConfiguration;
import io.github.resilience4j.bulkhead.autoconfigure.RefreshScopedBulkheadAutoConfiguration;
import io.github.resilience4j.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.circuitbreaker.autoconfigure.RefreshScopedCircuitBreakerAutoConfiguration;
import io.github.resilience4j.ratelimiter.autoconfigure.RateLimiterAutoConfiguration;
import io.github.resilience4j.ratelimiter.autoconfigure.RefreshScopedRateLimiterAutoConfiguration;
import io.github.resilience4j.retry.autoconfigure.RefreshScopedRetryAutoConfiguration;
import io.github.resilience4j.retry.autoconfigure.RetryAutoConfiguration;
import io.github.resilience4j.timelimiter.autoconfigure.RefreshScopedTimeLimiterAutoConfiguration;
import io.github.resilience4j.timelimiter.autoconfigure.TimeLimiterAutoConfiguration;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.type.MethodMetadata;

import static org.junit.Assert.assertTrue;

public class RefreshScopedAutoConfigurationTest {

    private AnnotationConfigApplicationContext context;

    @After
    public void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    public void refreshScopedBulkheadRegistry() {
        load(RefreshScopedBulkheadAutoConfiguration.class, BulkheadAutoConfiguration.class);
        testRefreshScoped("bulkheadRegistry");
        testRefreshScoped("threadPoolBulkheadRegistry");
    }

    @Test
    public void refreshScopedCircuitBreakerRegistry() {
        load(RefreshScopedCircuitBreakerAutoConfiguration.class,
            CircuitBreakerAutoConfiguration.class);
        testRefreshScoped("circuitBreakerRegistry");
    }

    @Test
    public void refreshScopedRateLimiterRegistry() {
        load(RefreshScopedRateLimiterAutoConfiguration.class, RateLimiterAutoConfiguration.class);
        testRefreshScoped("rateLimiterRegistry");
    }

    @Test
    public void refreshScopedRetryRegistry() {
        load(RefreshScopedRetryAutoConfiguration.class, RetryAutoConfiguration.class);
        testRefreshScoped("retryRegistry");
    }

    @Test
    public void refreshScopedTimeLimiterRegistry() {
        load(RefreshScopedTimeLimiterAutoConfiguration.class, TimeLimiterAutoConfiguration.class);
        testRefreshScoped("timeLimiterRegistry");
    }

    public void load(Class<?>... configs) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        if (configs != null) {
            context.register(configs);
        }

        context.refresh();
        this.context = context;
    }


    private void testRefreshScoped(String beanName) {
        BeanDefinition beanDefinition = context.getBeanFactory().getBeanDefinition(beanName);
        MethodMetadata beanMethod = (MethodMetadata) beanDefinition.getSource();

        assertTrue(beanMethod.isAnnotated(RefreshScope.class.getName()));
    }
}
