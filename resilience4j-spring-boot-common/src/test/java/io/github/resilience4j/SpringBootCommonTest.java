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
import io.github.resilience4j.common.bulkhead.configuration.ThreadPoolBulkheadConfigurationProperties;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.fallback.CompletionStageFallbackDecorator;
import io.github.resilience4j.fallback.FallbackDecorators;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.autoconfigure.AbstractRateLimiterConfigurationOnMissingBean;
import io.github.resilience4j.ratelimiter.configure.RateLimiterConfigurationProperties;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.autoconfigure.AbstractRetryConfigurationOnMissingBean;
import io.github.resilience4j.retry.configure.RetryConfigurationProperties;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author romeh
 */
public class SpringBootCommonTest {

	@Test
	public void testBulkHeadCommonConfig() {
		BulkheadConfigurationOnMissingBean bulkheadConfigurationOnMissingBean = new BulkheadConfigurationOnMissingBean();
		assertThat(bulkheadConfigurationOnMissingBean.bulkheadRegistry(new BulkheadConfigurationProperties(), new DefaultEventConsumerRegistry<>())).isNotNull();
		assertThat(bulkheadConfigurationOnMissingBean.threadPoolBulkheadRegistry(new ThreadPoolBulkheadConfigurationProperties(), new DefaultEventConsumerRegistry<>())).isNotNull();
		assertThat(bulkheadConfigurationOnMissingBean.reactorBulkHeadAspectExt()).isNotNull();
		assertThat(bulkheadConfigurationOnMissingBean.rxJava2BulkHeadAspectExt()).isNotNull();
		assertThat(bulkheadConfigurationOnMissingBean.bulkheadAspect(new BulkheadConfigurationProperties(), ThreadPoolBulkheadRegistry.ofDefaults(), BulkheadRegistry.ofDefaults(), Collections.emptyList(), new FallbackDecorators(Arrays.asList(new CompletionStageFallbackDecorator()))));
	}

	@Test
	public void testCircuitBreakerCommonConfig() {
		CircuitBreakerConfig circuitBreakerConfig = new CircuitBreakerConfig(new CircuitBreakerConfigurationProperties());
		assertThat(circuitBreakerConfig.reactorCircuitBreakerAspect()).isNotNull();
		assertThat(circuitBreakerConfig.rxJava2CircuitBreakerAspect()).isNotNull();
		assertThat(circuitBreakerConfig.circuitBreakerRegistry(new DefaultEventConsumerRegistry<>())).isNotNull();
		assertThat(circuitBreakerConfig.circuitBreakerAspect(CircuitBreakerRegistry.ofDefaults(), Collections.emptyList(), new FallbackDecorators(Arrays.asList(new CompletionStageFallbackDecorator()))));
	}

	@Test
	public void testRetryCommonConfig() {
		RetryConfigurationOnMissingBean retryConfigurationOnMissingBean = new RetryConfigurationOnMissingBean();
		assertThat(retryConfigurationOnMissingBean.reactorRetryAspectExt()).isNotNull();
		assertThat(retryConfigurationOnMissingBean.rxJava2RetryAspectExt()).isNotNull();
		assertThat(retryConfigurationOnMissingBean.retryRegistry(new RetryConfigurationProperties(), new DefaultEventConsumerRegistry<>())).isNotNull();
		assertThat(retryConfigurationOnMissingBean.retryAspect(new RetryConfigurationProperties(), RetryRegistry.ofDefaults(), Collections.emptyList(), new FallbackDecorators(Arrays.asList(new CompletionStageFallbackDecorator()))));
	}

	@Test
	public void testRateLimiterCommonConfig() {
		RateLimiterConfigurationOnMissingBean rateLimiterConfigurationOnMissingBean = new RateLimiterConfigurationOnMissingBean();
		assertThat(rateLimiterConfigurationOnMissingBean.reactorRateLimiterAspectExt()).isNotNull();
		assertThat(rateLimiterConfigurationOnMissingBean.rxJava2RateLimiterAspectExt()).isNotNull();
		assertThat(rateLimiterConfigurationOnMissingBean.rateLimiterRegistry(new RateLimiterConfigurationProperties(), new DefaultEventConsumerRegistry<>())).isNotNull();
		assertThat(rateLimiterConfigurationOnMissingBean.rateLimiterAspect(new RateLimiterConfigurationProperties(), RateLimiterRegistry.ofDefaults(), Collections.emptyList(), new FallbackDecorators(Arrays.asList(new CompletionStageFallbackDecorator()))));
	}


	// testing config samples
	class BulkheadConfigurationOnMissingBean extends AbstractBulkheadConfigurationOnMissingBean {
	}

	class CircuitBreakerConfig extends AbstractCircuitBreakerConfigurationOnMissingBean {

		public CircuitBreakerConfig(CircuitBreakerConfigurationProperties circuitBreakerProperties) {
			super(circuitBreakerProperties);
		}

	}

	class RetryConfigurationOnMissingBean extends AbstractRetryConfigurationOnMissingBean {
	}

	class RateLimiterConfigurationOnMissingBean extends AbstractRateLimiterConfigurationOnMissingBean {
	}
}
