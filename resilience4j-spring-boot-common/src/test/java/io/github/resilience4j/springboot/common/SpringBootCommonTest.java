package io.github.resilience4j.springboot.common;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.Collections;

import org.junit.Test;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.configure.BulkheadConfigurationProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfigurationProperties;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.configure.RateLimiterConfigurationProperties;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.configure.RetryConfigurationProperties;
import io.github.resilience4j.springboot.common.bulkhead.autoconfigure.AbstractBulkheadConfigurationOnMissingBean;
import io.github.resilience4j.springboot.common.circuitbreaker.autoconfigure.AbstractCircuitBreakerConfigurationOnMissingBean;
import io.github.resilience4j.springboot.common.ratelimiter.autoconfigure.AbstractRateLimiterConfigurationOnMissingBean;
import io.github.resilience4j.springboot.common.retry.autoconfigure.AbstractRetryConfigurationOnMissingBean;

/**
 * @author romeh
 */
public class SpringBootCommonTest {

	@Test
	public void testBulkHeadCommonConfig() {
		BulkheadConfigurationOnMissingBean bulkheadConfigurationOnMissingBean = new BulkheadConfigurationOnMissingBean();
		assertThat(bulkheadConfigurationOnMissingBean.bulkheadRegistry(new BulkheadConfigurationProperties(), new DefaultEventConsumerRegistry<>())).isNotNull();
		assertThat(bulkheadConfigurationOnMissingBean.reactorBulkHeadAspectExt()).isNotNull();
		assertThat(bulkheadConfigurationOnMissingBean.rxJava2BulkHeadAspectExt()).isNotNull();
		assertThat(bulkheadConfigurationOnMissingBean.bulkheadAspect(new BulkheadConfigurationProperties(), BulkheadRegistry.ofDefaults(), Collections.emptyList()));
	}

	@Test
	public void testCircuitBreakerCommonConfig() {
		CircuitBreakerConfig circuitBreakerConfig = new CircuitBreakerConfig(new CircuitBreakerConfigurationProperties());
		assertThat(circuitBreakerConfig.reactorCircuitBreakerAspect()).isNotNull();
		assertThat(circuitBreakerConfig.rxJava2CircuitBreakerAspect()).isNotNull();
		assertThat(circuitBreakerConfig.circuitBreakerRegistry(new DefaultEventConsumerRegistry<>())).isNotNull();
		assertThat(circuitBreakerConfig.circuitBreakerAspect(CircuitBreakerRegistry.ofDefaults(), Collections.emptyList()));
	}

	@Test
	public void testRetryCommonConfig() {
		RetryConfigurationOnMissingBean retryConfigurationOnMissingBean = new RetryConfigurationOnMissingBean();
		assertThat(retryConfigurationOnMissingBean.reactorRetryAspectExt()).isNotNull();
		assertThat(retryConfigurationOnMissingBean.rxJava2RetryAspectExt()).isNotNull();
		assertThat(retryConfigurationOnMissingBean.retryRegistry(new RetryConfigurationProperties(), new DefaultEventConsumerRegistry<>())).isNotNull();
		assertThat(retryConfigurationOnMissingBean.retryAspect(new RetryConfigurationProperties(), RetryRegistry.ofDefaults(), Collections.emptyList()));
	}

	@Test
	public void testRateLimiterCommonConfig() {
		RateLimiterConfigurationOnMissingBean rateLimiterConfigurationOnMissingBean = new RateLimiterConfigurationOnMissingBean();
		assertThat(rateLimiterConfigurationOnMissingBean.reactorRateLimiterAspectExt()).isNotNull();
		assertThat(rateLimiterConfigurationOnMissingBean.rxJava2RateLimterAspectExt()).isNotNull();
		assertThat(rateLimiterConfigurationOnMissingBean.rateLimiterRegistry(new RateLimiterConfigurationProperties(), new DefaultEventConsumerRegistry<>())).isNotNull();
		assertThat(rateLimiterConfigurationOnMissingBean.rateLimiterAspect(new RateLimiterConfigurationProperties(), RateLimiterRegistry.ofDefaults(), Collections.emptyList()));
	}


	// testing config samples
	class BulkheadConfigurationOnMissingBean extends AbstractBulkheadConfigurationOnMissingBean {
	}

	class CircuitBreakerConfig extends AbstractCircuitBreakerConfigurationOnMissingBean {

		public CircuitBreakerConfig(CircuitBreakerConfigurationProperties circuitBreakerProperties) {
			super(circuitBreakerProperties);
		}

		@Override
		protected void createHeathIndicatorForCircuitBreaker(CircuitBreaker circuitBreaker) {

		}
	}

	class RetryConfigurationOnMissingBean extends AbstractRetryConfigurationOnMissingBean {
	}

	class RateLimiterConfigurationOnMissingBean extends AbstractRateLimiterConfigurationOnMissingBean {
	}
}
