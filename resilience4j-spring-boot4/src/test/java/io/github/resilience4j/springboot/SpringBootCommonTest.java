/*
 * Copyright 2025 Mahmoud Romeh, Artur Havliukovskyi
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

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.spring6.bulkhead.configure.BulkheadConfigurationProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.springboot.bulkhead.autoconfigure.BulkheadAutoConfiguration;
import io.github.resilience4j.spring6.circuitbreaker.configure.CircuitBreakerConfigurationProperties;
import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.bulkhead.configuration.BulkheadConfigCustomizer;
import io.github.resilience4j.common.bulkhead.configuration.ThreadPoolBulkheadConfigCustomizer;
import io.github.resilience4j.common.bulkhead.configuration.CommonThreadPoolBulkheadConfigurationProperties;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigCustomizer;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.core.registry.CompositeRegistryEventConsumer;
import io.github.resilience4j.spring6.fallback.CompletionStageFallbackDecorator;
import io.github.resilience4j.spring6.fallback.FallbackDecorators;
import io.github.resilience4j.spring6.fallback.FallbackExecutor;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.springboot.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.spring6.ratelimiter.configure.RateLimiterConfigurationProperties;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.springboot.ratelimiter.autoconfigure.RateLimiterAutoConfiguration;
import io.github.resilience4j.spring6.retry.configure.RetryConfigurationProperties;
import io.github.resilience4j.spring6.spelresolver.DefaultSpelResolver;
import io.github.resilience4j.springboot.retry.autoconfigure.RetryAutoConfiguration;
import io.github.resilience4j.springboot.timelimiter.autoconfigure.TimeLimiterAutoConfiguration;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.github.resilience4j.spring6.timelimiter.configure.TimeLimiterConfigurationProperties;
import org.junit.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author romeh
 */
public class SpringBootCommonTest {

    @Test
    public void testBulkHeadCommonConfig() {
        BulkheadAutoConfiguration bulkheadAutoConfiguration = new BulkheadAutoConfiguration();
        assertThat(bulkheadAutoConfiguration
            .bulkheadRegistry(new BulkheadConfigurationProperties(),
                new DefaultEventConsumerRegistry<>(),
                new CompositeRegistryEventConsumer<>(Collections.emptyList()),
                new CompositeCustomizer<>(Collections.singletonList(BulkheadConfigCustomizer.of("backend", builder -> builder.maxConcurrentCalls(10)))))).isNotNull();
        assertThat(bulkheadAutoConfiguration
            .threadPoolBulkheadRegistry(new CommonThreadPoolBulkheadConfigurationProperties(),
                new DefaultEventConsumerRegistry<>(),
                new CompositeRegistryEventConsumer<>(Collections.emptyList()),
                new CompositeCustomizer<>(Collections.singletonList(
                    ThreadPoolBulkheadConfigCustomizer.of("backend", builder -> builder.coreThreadPoolSize(10).maxThreadPoolSize(11)))))).isNotNull();
        assertThat(bulkheadAutoConfiguration.reactorBulkHeadAspectExt()).isNotNull();
        assertThat(bulkheadAutoConfiguration.rxJava2BulkHeadAspectExt()).isNotNull();
        assertThat(bulkheadAutoConfiguration.rxJava3BulkHeadAspectExt()).isNotNull();
        final DefaultSpelResolver spelResolver = new DefaultSpelResolver(new SpelExpressionParser(), new StandardReflectionParameterNameDiscoverer(), new GenericApplicationContext());
        final FallbackDecorators fallbackDecorators = new FallbackDecorators(Collections.singletonList(new CompletionStageFallbackDecorator()));
        assertThat(bulkheadAutoConfiguration
            .bulkheadAspect(new BulkheadConfigurationProperties(),
                ThreadPoolBulkheadRegistry.ofDefaults(), BulkheadRegistry.ofDefaults(),
                Collections.emptyList(),
                new FallbackExecutor(spelResolver, fallbackDecorators),
                spelResolver)).isNotNull();
        assertThat(
            bulkheadAutoConfiguration.bulkheadRegistryEventConsumer(Optional.empty())).isNotNull();
    }

    @Test
    public void testCircuitBreakerCommonConfig() {
        CircuitBreakerAutoConfiguration circuitBreakerAutoConfiguration = new CircuitBreakerAutoConfiguration(
            new CircuitBreakerConfigurationProperties());
        assertThat(circuitBreakerAutoConfiguration.reactorCircuitBreakerAspect()).isNotNull();
        assertThat(circuitBreakerAutoConfiguration.rxJava2CircuitBreakerAspect()).isNotNull();
        assertThat(circuitBreakerAutoConfiguration.rxJava3CircuitBreakerAspect()).isNotNull();
        assertThat(circuitBreakerAutoConfiguration.circuitBreakerRegistry(new DefaultEventConsumerRegistry<>(),
            new CompositeRegistryEventConsumer<>(Collections.emptyList()),
            new CompositeCustomizer<>(Collections.singletonList(new TestCustomizer())))).isNotNull();
        final DefaultSpelResolver spelResolver = new DefaultSpelResolver(new SpelExpressionParser(), new StandardReflectionParameterNameDiscoverer(), new GenericApplicationContext());
        final FallbackDecorators fallbackDecorators = new FallbackDecorators(Collections.singletonList(new CompletionStageFallbackDecorator()));
        assertThat(circuitBreakerAutoConfiguration
            .circuitBreakerAspect(CircuitBreakerRegistry.ofDefaults(), Collections.emptyList(),
                new FallbackExecutor(spelResolver, fallbackDecorators),
                spelResolver)).isNotNull();
        assertThat(circuitBreakerAutoConfiguration.circuitBreakerRegistryEventConsumer(Optional.empty())).isNotNull();
    }

    @Test
    public void testRetryCommonConfig() {
        RetryAutoConfiguration retryAutoConfiguration = new RetryAutoConfiguration();
        assertThat(retryAutoConfiguration.reactorRetryAspectExt()).isNotNull();
        assertThat(retryAutoConfiguration.rxJava2RetryAspectExt()).isNotNull();
        assertThat(retryAutoConfiguration.rxJava3RetryAspectExt()).isNotNull();
        assertThat(retryAutoConfiguration
            .retryRegistry(new RetryConfigurationProperties(), new DefaultEventConsumerRegistry<>(),
                new CompositeRegistryEventConsumer<>(Collections.emptyList()),
                new CompositeCustomizer<>(Collections.emptyList()))).isNotNull();
        final DefaultSpelResolver spelResolver = new DefaultSpelResolver(new SpelExpressionParser(), new StandardReflectionParameterNameDiscoverer(), new GenericApplicationContext());
        final FallbackDecorators fallbackDecorators = new FallbackDecorators(Collections.singletonList(new CompletionStageFallbackDecorator()));
        assertThat(retryAutoConfiguration
            .retryAspect(new RetryConfigurationProperties(), RetryRegistry.ofDefaults(),
                Collections.emptyList(),
                new FallbackExecutor(spelResolver, fallbackDecorators),
                spelResolver, null)).isNotNull();
        assertThat(retryAutoConfiguration.retryRegistryEventConsumer(Optional.empty())).isNotNull();
    }

    @Test
    public void testRateLimiterCommonConfig() {
        RateLimiterAutoConfiguration rateLimiterAutoConfiguration = new RateLimiterAutoConfiguration();
        assertThat(rateLimiterAutoConfiguration.reactorRateLimiterAspectExt()).isNotNull();
        assertThat(rateLimiterAutoConfiguration.rxJava2RateLimiterAspectExt()).isNotNull();
        assertThat(rateLimiterAutoConfiguration.rxJava3RateLimiterAspectExt()).isNotNull();
        assertThat(rateLimiterAutoConfiguration
            .rateLimiterRegistry(new RateLimiterConfigurationProperties(),
                new DefaultEventConsumerRegistry<>(),
                new CompositeRegistryEventConsumer<>(Collections.emptyList()),
                new CompositeCustomizer<>(Collections.emptyList()))).isNotNull();
        final FallbackDecorators fallbackDecorators = new FallbackDecorators(Arrays.asList(new CompletionStageFallbackDecorator()));
        final DefaultSpelResolver spelResolver = new DefaultSpelResolver(new SpelExpressionParser(), new StandardReflectionParameterNameDiscoverer(), new GenericApplicationContext());
        assertThat(rateLimiterAutoConfiguration
            .rateLimiterAspect(new RateLimiterConfigurationProperties(),
                RateLimiterRegistry.ofDefaults(), Collections.emptyList(),
                new FallbackExecutor(spelResolver, fallbackDecorators),
                spelResolver)).isNotNull();
        assertThat(rateLimiterAutoConfiguration
            .rateLimiterRegistryEventConsumer(Optional.empty())).isNotNull();
    }

    @Test
    public void testTimeLimiterCommonConfig() {
        TimeLimiterAutoConfiguration timeLimiterAutoConfiguration = new TimeLimiterAutoConfiguration();
        assertThat(timeLimiterAutoConfiguration.reactorTimeLimiterAspectExt()).isNotNull();
        assertThat(timeLimiterAutoConfiguration.rxJava2TimeLimiterAspectExt()).isNotNull();
        assertThat(timeLimiterAutoConfiguration.rxJava3TimeLimiterAspectExt()).isNotNull();
        assertThat(timeLimiterAutoConfiguration
            .timeLimiterRegistry(new TimeLimiterConfigurationProperties(),
                new DefaultEventConsumerRegistry<>(),
                new CompositeRegistryEventConsumer<>(Collections.emptyList()),
                new CompositeCustomizer<>(Collections.singletonList(
                    TimeLimiterConfigCustomizer.of("backend", builder -> builder.timeoutDuration(
                        Duration.ofSeconds(10))))))).isNotNull();
        final DefaultSpelResolver spelResolver = new DefaultSpelResolver(new SpelExpressionParser(), new StandardReflectionParameterNameDiscoverer(), new GenericApplicationContext());
        final FallbackDecorators fallbackDecorators = new FallbackDecorators(Arrays.asList(new CompletionStageFallbackDecorator()));
        assertThat(timeLimiterAutoConfiguration
            .timeLimiterAspect(new TimeLimiterConfigurationProperties(),
                TimeLimiterRegistry.ofDefaults(), Collections.emptyList(),
                new FallbackExecutor(spelResolver, fallbackDecorators),
                spelResolver,
                null)).isNotNull();
        assertThat(timeLimiterAutoConfiguration
            .timeLimiterRegistryEventConsumer(Optional.empty())).isNotNull();
    }

    static class TestCustomizer implements CircuitBreakerConfigCustomizer {

        @Override
        public void customize(
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.Builder builder) {
            builder.slidingWindowSize(3000);
        }

        @Override
        public String name() {
            return "backendCustom";
        }
    }
}
