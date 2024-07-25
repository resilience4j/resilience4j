/*
 * Copyright 2019 Mahmoud Romeh
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
package io.github.resilience4j;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.bulkhead.autoconfigure.AbstractBulkheadConfigurationOnMissingBean;
import io.github.resilience4j.bulkhead.configure.BulkheadConfigurationProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.autoconfigure.AbstractCircuitBreakerConfigurationOnMissingBean;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfigurationProperties;
import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.bulkhead.configuration.BulkheadConfigCustomizer;
import io.github.resilience4j.common.bulkhead.configuration.ThreadPoolBulkheadConfigCustomizer;
import io.github.resilience4j.common.bulkhead.configuration.CommonThreadPoolBulkheadConfigurationProperties;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigCustomizer;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.core.registry.CompositeRegistryEventConsumer;
import io.github.resilience4j.fallback.CompletionStageFallbackDecorator;
import io.github.resilience4j.fallback.FallbackDecorators;
import io.github.resilience4j.fallback.FallbackExecutor;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.autoconfigure.AbstractRateLimiterConfigurationOnMissingBean;
import io.github.resilience4j.ratelimiter.configure.RateLimiterConfigurationProperties;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.autoconfigure.AbstractRetryConfigurationOnMissingBean;
import io.github.resilience4j.retry.configure.RetryConfigurationProperties;
import io.github.resilience4j.spelresolver.DefaultSpelResolver;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.github.resilience4j.timelimiter.autoconfigure.AbstractTimeLimiterConfigurationOnMissingBean;
import io.github.resilience4j.timelimiter.configure.TimeLimiterConfigurationProperties;
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
        BulkheadConfigurationOnMissingBean bulkheadConfigurationOnMissingBean = new BulkheadConfigurationOnMissingBean();
        assertThat(bulkheadConfigurationOnMissingBean
            .bulkheadRegistry(new BulkheadConfigurationProperties(),
                new DefaultEventConsumerRegistry<>(),
                new CompositeRegistryEventConsumer<>(Collections.emptyList()),
                new CompositeCustomizer<>(Collections.singletonList(BulkheadConfigCustomizer.of("backend", builder -> builder.maxConcurrentCalls(10)))))).isNotNull();
        assertThat(bulkheadConfigurationOnMissingBean
            .threadPoolBulkheadRegistry(new CommonThreadPoolBulkheadConfigurationProperties(),
                new DefaultEventConsumerRegistry<>(),
                new CompositeRegistryEventConsumer<>(Collections.emptyList()),
                new CompositeCustomizer<>(Collections.singletonList(
                    ThreadPoolBulkheadConfigCustomizer.of("backend", builder -> builder.maxThreadPoolSize(128)))))).isNotNull();
        assertThat(bulkheadConfigurationOnMissingBean.reactorBulkHeadAspectExt()).isNotNull();
        assertThat(bulkheadConfigurationOnMissingBean.rxJava2BulkHeadAspectExt()).isNotNull();
        assertThat(bulkheadConfigurationOnMissingBean.rxJava3BulkHeadAspectExt()).isNotNull();
        final DefaultSpelResolver spelResolver = new DefaultSpelResolver(new SpelExpressionParser(), new StandardReflectionParameterNameDiscoverer(), new GenericApplicationContext());
        final FallbackDecorators fallbackDecorators = new FallbackDecorators(Collections.singletonList(new CompletionStageFallbackDecorator()));
        assertThat(bulkheadConfigurationOnMissingBean
            .bulkheadAspect(new BulkheadConfigurationProperties(),
                ThreadPoolBulkheadRegistry.ofDefaults(), BulkheadRegistry.ofDefaults(),
                Collections.emptyList(),
                new FallbackExecutor(spelResolver, fallbackDecorators),
                spelResolver)).isNotNull();
        assertThat(
            bulkheadConfigurationOnMissingBean.bulkheadRegistryEventConsumer(Optional.empty())).isNotNull();
    }

    @Test
    public void testCircuitBreakerCommonConfig() {
        CircuitBreakerConfig circuitBreakerConfig = new CircuitBreakerConfig(
            new CircuitBreakerConfigurationProperties());
        assertThat(circuitBreakerConfig.reactorCircuitBreakerAspect()).isNotNull();
        assertThat(circuitBreakerConfig.rxJava2CircuitBreakerAspect()).isNotNull();
        assertThat(circuitBreakerConfig.rxJava3CircuitBreakerAspect()).isNotNull();
        assertThat(circuitBreakerConfig.circuitBreakerRegistry(new DefaultEventConsumerRegistry<>(),
            new CompositeRegistryEventConsumer<>(Collections.emptyList()),
            new CompositeCustomizer<>(Collections.singletonList(new TestCustomizer())))).isNotNull();
        final DefaultSpelResolver spelResolver = new DefaultSpelResolver(new SpelExpressionParser(), new StandardReflectionParameterNameDiscoverer(), new GenericApplicationContext());
        final FallbackDecorators fallbackDecorators = new FallbackDecorators(Collections.singletonList(new CompletionStageFallbackDecorator()));
        assertThat(circuitBreakerConfig
            .circuitBreakerAspect(CircuitBreakerRegistry.ofDefaults(), Collections.emptyList(),
                new FallbackExecutor(spelResolver, fallbackDecorators),
                spelResolver)).isNotNull();
        assertThat(circuitBreakerConfig.circuitBreakerRegistryEventConsumer(Optional.empty())).isNotNull();
    }

    @Test
    public void testRetryCommonConfig() {
        RetryConfigurationOnMissingBean retryConfigurationOnMissingBean = new RetryConfigurationOnMissingBean();
        assertThat(retryConfigurationOnMissingBean.reactorRetryAspectExt()).isNotNull();
        assertThat(retryConfigurationOnMissingBean.rxJava2RetryAspectExt()).isNotNull();
        assertThat(retryConfigurationOnMissingBean.rxJava3RetryAspectExt()).isNotNull();
        assertThat(retryConfigurationOnMissingBean
            .retryRegistry(new RetryConfigurationProperties(), new DefaultEventConsumerRegistry<>(),
                new CompositeRegistryEventConsumer<>(Collections.emptyList()),
                new CompositeCustomizer<>(Collections.emptyList()))).isNotNull();
        final DefaultSpelResolver spelResolver = new DefaultSpelResolver(new SpelExpressionParser(), new StandardReflectionParameterNameDiscoverer(), new GenericApplicationContext());
        final FallbackDecorators fallbackDecorators = new FallbackDecorators(Collections.singletonList(new CompletionStageFallbackDecorator()));
        assertThat(retryConfigurationOnMissingBean
            .retryAspect(new RetryConfigurationProperties(), RetryRegistry.ofDefaults(),
                Collections.emptyList(),
                new FallbackExecutor(spelResolver, fallbackDecorators),
                spelResolver, null)).isNotNull();
        assertThat(retryConfigurationOnMissingBean.retryRegistryEventConsumer(Optional.empty())).isNotNull();
    }

    @Test
    public void testRateLimiterCommonConfig() {
        RateLimiterConfigurationOnMissingBean rateLimiterConfigurationOnMissingBean = new RateLimiterConfigurationOnMissingBean();
        assertThat(rateLimiterConfigurationOnMissingBean.reactorRateLimiterAspectExt()).isNotNull();
        assertThat(rateLimiterConfigurationOnMissingBean.rxJava2RateLimiterAspectExt()).isNotNull();
        assertThat(rateLimiterConfigurationOnMissingBean.rxJava3RateLimiterAspectExt()).isNotNull();
        assertThat(rateLimiterConfigurationOnMissingBean
            .rateLimiterRegistry(new RateLimiterConfigurationProperties(),
                new DefaultEventConsumerRegistry<>(),
                new CompositeRegistryEventConsumer<>(Collections.emptyList()),
                new CompositeCustomizer<>(Collections.emptyList()))).isNotNull();
        final FallbackDecorators fallbackDecorators = new FallbackDecorators(Arrays.asList(new CompletionStageFallbackDecorator()));
        final DefaultSpelResolver spelResolver = new DefaultSpelResolver(new SpelExpressionParser(), new StandardReflectionParameterNameDiscoverer(), new GenericApplicationContext());
        assertThat(rateLimiterConfigurationOnMissingBean
            .rateLimiterAspect(new RateLimiterConfigurationProperties(),
                RateLimiterRegistry.ofDefaults(), Collections.emptyList(),
                new FallbackExecutor(spelResolver, fallbackDecorators),
                spelResolver)).isNotNull();
        assertThat(rateLimiterConfigurationOnMissingBean
            .rateLimiterRegistryEventConsumer(Optional.empty())).isNotNull();
    }

    @Test
    public void testTimeLimiterCommonConfig() {
        TimeLimiterConfigurationOnMissingBean timeLimiterConfigurationOnMissingBean = new TimeLimiterConfigurationOnMissingBean();
        assertThat(timeLimiterConfigurationOnMissingBean.reactorTimeLimiterAspectExt()).isNotNull();
        assertThat(timeLimiterConfigurationOnMissingBean.rxJava2TimeLimiterAspectExt()).isNotNull();
        assertThat(timeLimiterConfigurationOnMissingBean.rxJava3TimeLimiterAspectExt()).isNotNull();
        assertThat(timeLimiterConfigurationOnMissingBean
            .timeLimiterRegistry(new TimeLimiterConfigurationProperties(),
                new DefaultEventConsumerRegistry<>(),
                new CompositeRegistryEventConsumer<>(Collections.emptyList()),
                new CompositeCustomizer<>(Collections.singletonList(
                    TimeLimiterConfigCustomizer.of("backend", builder -> builder.timeoutDuration(
                        Duration.ofSeconds(10))))))).isNotNull();
        final DefaultSpelResolver spelResolver = new DefaultSpelResolver(new SpelExpressionParser(), new StandardReflectionParameterNameDiscoverer(), new GenericApplicationContext());
        final FallbackDecorators fallbackDecorators = new FallbackDecorators(Arrays.asList(new CompletionStageFallbackDecorator()));
        assertThat(timeLimiterConfigurationOnMissingBean
            .timeLimiterAspect(new TimeLimiterConfigurationProperties(),
                TimeLimiterRegistry.ofDefaults(), Collections.emptyList(),
                new FallbackExecutor(spelResolver, fallbackDecorators),
                spelResolver,
                null)).isNotNull();
        assertThat(timeLimiterConfigurationOnMissingBean
            .timeLimiterRegistryEventConsumer(Optional.empty())).isNotNull();
    }

    // testing config samples
    class BulkheadConfigurationOnMissingBean extends AbstractBulkheadConfigurationOnMissingBean {

    }

    class CircuitBreakerConfig extends AbstractCircuitBreakerConfigurationOnMissingBean {

        public CircuitBreakerConfig(
            CircuitBreakerConfigurationProperties circuitBreakerProperties) {
            super(circuitBreakerProperties);
        }
    }

    class TestCustomizer implements CircuitBreakerConfigCustomizer {

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

    class RetryConfigurationOnMissingBean extends AbstractRetryConfigurationOnMissingBean {

    }

    class RateLimiterConfigurationOnMissingBean extends
        AbstractRateLimiterConfigurationOnMissingBean {

    }

    class TimeLimiterConfigurationOnMissingBean extends AbstractTimeLimiterConfigurationOnMissingBean {
    }
}
