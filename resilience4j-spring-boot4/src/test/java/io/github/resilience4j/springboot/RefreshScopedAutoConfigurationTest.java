/*
 * Copyright 2025 Ingyu Hwang, Artur Havliukovskyi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.springboot;

import io.github.resilience4j.springboot.bulkhead.autoconfigure.BulkheadAutoConfiguration;
import io.github.resilience4j.springboot.bulkhead.autoconfigure.BulkheadRefreshScopedRegistryAutoConfiguration;
import io.github.resilience4j.springboot.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot.circuitbreaker.autoconfigure.CircuitBreakerRefreshScopedRegistryAutoConfiguration;
import io.github.resilience4j.springboot.ratelimiter.autoconfigure.RateLimiterAutoConfiguration;
import io.github.resilience4j.springboot.ratelimiter.autoconfigure.RateLimiterRefreshScopedRegistryAutoConfiguration;
import io.github.resilience4j.springboot.retry.autoconfigure.RetryRefreshScopedRegistryAutoConfiguration;
import io.github.resilience4j.springboot.retry.autoconfigure.RetryAutoConfiguration;
import io.github.resilience4j.springboot.timelimiter.autoconfigure.TimeLimiterRefreshScopedRegistryAutoConfiguration;
import io.github.resilience4j.springboot.timelimiter.autoconfigure.TimeLimiterAutoConfiguration;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.type.MethodMetadata;

import static org.assertj.core.api.Assertions.assertThat;

public class RefreshScopedAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    public void refreshScopedBulkheadRegistry() {
        contextRunner
            .withConfiguration(AutoConfigurations.of(
                BulkheadRefreshScopedRegistryAutoConfiguration.class,
                BulkheadAutoConfiguration.class,
                RefreshAutoConfiguration.class))
            .run(context -> {
                assertRefreshScoped(context, "bulkheadRegistry");
                assertRefreshScoped(context, "threadPoolBulkheadRegistry");
            });
    }

    @Test
    public void refreshScopedCircuitBreakerRegistry() {
        contextRunner
            .withConfiguration(AutoConfigurations.of(
                CircuitBreakerRefreshScopedRegistryAutoConfiguration.class,
                CircuitBreakerAutoConfiguration.class,
                RefreshAutoConfiguration.class))
            .run(context -> assertRefreshScoped(context, "circuitBreakerRegistry"));
    }

    @Test
    public void refreshScopedRateLimiterRegistry() {
        contextRunner
            .withConfiguration(AutoConfigurations.of(
                RateLimiterRefreshScopedRegistryAutoConfiguration.class,
                RateLimiterAutoConfiguration.class,
                RefreshAutoConfiguration.class))
            .run(context -> assertRefreshScoped(context, "rateLimiterRegistry"));
    }

    @Test
    public void refreshScopedRetryRegistry() {
        contextRunner
            .withConfiguration(AutoConfigurations.of(
                RetryRefreshScopedRegistryAutoConfiguration.class,
                RetryAutoConfiguration.class,
                RefreshAutoConfiguration.class))
            .run(context -> assertRefreshScoped(context, "retryRegistry"));
    }

    @Test
    public void refreshScopedTimeLimiterRegistry() {
        contextRunner
            .withConfiguration(AutoConfigurations.of(
                TimeLimiterRefreshScopedRegistryAutoConfiguration.class,
                TimeLimiterAutoConfiguration.class,
                RefreshAutoConfiguration.class))
            .run(context -> assertRefreshScoped(context, "timeLimiterRegistry"));
    }

    @Test
    public void registriesNotRefreshableIfDisabled() {
        contextRunner
                .withConfiguration(AutoConfigurations.of(
                    BulkheadRefreshScopedRegistryAutoConfiguration.class,
                    BulkheadAutoConfiguration.class,
                    CircuitBreakerRefreshScopedRegistryAutoConfiguration.class,
                    CircuitBreakerAutoConfiguration.class,
                    RateLimiterRefreshScopedRegistryAutoConfiguration.class,
                    RateLimiterAutoConfiguration.class,
                    RetryRefreshScopedRegistryAutoConfiguration.class,
                    RetryAutoConfiguration.class,
                    TimeLimiterRefreshScopedRegistryAutoConfiguration.class,
                    TimeLimiterAutoConfiguration.class,
                    RefreshAutoConfiguration.class))
                .withPropertyValues(RefreshAutoConfiguration.REFRESH_SCOPE_ENABLED + ":false")
                .run(context -> {
                    testNotRefreshScoped(context, "bulkheadRegistry");
                    testNotRefreshScoped(context, "threadPoolBulkheadRegistry");
                    testNotRefreshScoped(context, "circuitBreakerRegistry");
                    testNotRefreshScoped(context, "rateLimiterRegistry");
                    testNotRefreshScoped(context, "retryRegistry");
                    testNotRefreshScoped(context, "timeLimiterRegistry");
                });

    }

    private static void assertRefreshScoped(AssertableApplicationContext context, String beanName) {
        BeanDefinition beanDefinition = context.getBeanFactory().getBeanDefinition(beanName);
        MethodMetadata beanMethod = (MethodMetadata) beanDefinition.getSource();

        assertThat(beanMethod)
                .isNotNull()
                .withFailMessage("%s.%s must be annotated with @RefreshScope".formatted(beanMethod.getDeclaringClassName(), beanMethod.getMethodName()))
                .extracting(bm -> bm.isAnnotated(RefreshScope.class.getName()), InstanceOfAssertFactories.BOOLEAN)
                .isTrue();
    }

    private static void testNotRefreshScoped(AssertableApplicationContext context, String beanName) {
        BeanDefinition beanDefinition = context.getBeanFactory().getBeanDefinition(beanName);
        MethodMetadata beanMethod = (MethodMetadata) beanDefinition.getSource();

        assertThat(beanMethod)
                .isNotNull()
                .withFailMessage("%s.%s must not be annotated with @RefreshScope".formatted(beanMethod.getDeclaringClassName(), beanMethod.getMethodName()))
                .extracting(bm -> bm.isAnnotated(RefreshScope.class.getName()), InstanceOfAssertFactories.BOOLEAN)
                .isFalse();
    }
}
