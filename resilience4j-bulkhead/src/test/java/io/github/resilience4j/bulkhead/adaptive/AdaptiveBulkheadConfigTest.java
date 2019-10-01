package io.github.resilience4j.bulkhead.adaptive;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Java6Assertions.assertThat;

import org.junit.Test;

import io.github.resilience4j.bulkhead.adaptive.internal.config.AbstractConfig;
import io.github.resilience4j.bulkhead.adaptive.internal.config.AimdConfig;


/**
 *
 */
public class AdaptiveBulkheadConfigTest {
	@Test
	public void testBuildCustom() {
		AdaptiveBulkheadConfig<AimdConfig> config = AdaptiveBulkheadConfig.<AimdConfig>builder()
				.config(AimdConfig.builder()
						.concurrencyDropMultiplier(0.3)
						.minConcurrentRequestsLimit(3)
						.maxConcurrentRequestsLimit(3)
						.slowCallDurationThreshold(150)
						.slowCallRateThreshold(50)
						.failureRateThreshold(50)
						.slidingWindowTime(5)
						.slidingWindowSize(100)
						.slidingWindowType(AbstractConfig.SlidingWindow.TIME_BASED)
						.build()).build();

		assertThat(config).isNotNull();
		assertThat(config.getConfiguration().getConcurrencyDropMultiplier()).isEqualTo(0.85);
		assertThat(config.getConfiguration().getMinLimit()).isEqualTo(3);
		assertThat(config.getConfiguration().getMaxLimit()).isEqualTo(3);
		assertThat(config.getConfiguration().getDesirableLatency().toMillis()).isEqualTo(150);
		assertThat(config.getConfiguration().getSlidingWindowSize()).isEqualTo(100);
		assertThat(config.getConfiguration().getSlidingWindowTime()).isEqualTo(5);
		assertThat(config.getConfiguration().getSlowCallRateThreshold()).isEqualTo(50);
		assertThat(config.getConfiguration().getFailureRateThreshold()).isEqualTo(50);
		assertThat(config.getConfiguration().getSlidingWindowType()).isEqualTo(AbstractConfig.SlidingWindow.TIME_BASED);

	}


	@Test
	public void tesConcurrencyDropMultiplierConfig() {
		final AimdConfig build = AimdConfig.builder().concurrencyDropMultiplier(0.0).build();
		assertThat(build.getConcurrencyDropMultiplier()).isEqualTo(0.85d);
	}

	@Test
	public void testNotSetDesirableOperationLatencyConfig() {
		assertThatThrownBy(() -> AimdConfig.builder().slowCallDurationThreshold(0).build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("slowCallDurationThreshold must be a positive value greater than zero");
	}

	@Test
	public void testNotSetMaxAcceptableRequestLatencyConfig() {
		assertThatThrownBy(() -> AimdConfig.builder().maxConcurrentRequestsLimit(0)
				.build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("maxConcurrentRequestsLimit must be a positive value greater than zero");
	}

	@Test
	public void testNotSetMinAcceptableRequestLatencyConfig() {
		assertThatThrownBy(() -> AimdConfig.builder().minConcurrentRequestsLimit(0)
				.build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("minConcurrentRequestsLimit must be a positive value greater than zero");
	}


	@Test
	public void testLimitIncrementInflightFactor() {
		final AimdConfig build = AimdConfig.builder().limitIncrementInflightFactor(1).build();
		assertThat(build.getLimitIncrementInflightFactor()).isEqualTo(1);
	}


	@Test
	public void testEqual() {
		AimdConfig config = AimdConfig.builder()
				.concurrencyDropMultiplier(0.6)
				.minConcurrentRequestsLimit(3)
				.maxConcurrentRequestsLimit(3)
				.slowCallDurationThreshold(150)
				.slowCallRateThreshold(50)
				.failureRateThreshold(50)
				.slidingWindowTime(5)
				.slidingWindowSize(100)
				.slidingWindowType(AbstractConfig.SlidingWindow.TIME_BASED)
				.build();
		AimdConfig config2 = AimdConfig.builder()
				.concurrencyDropMultiplier(0.6)
				.minConcurrentRequestsLimit(3)
				.maxConcurrentRequestsLimit(3)
				.slowCallDurationThreshold(150)
				.slowCallRateThreshold(50)
				.failureRateThreshold(50)
				.slidingWindowTime(5)
				.slidingWindowSize(100)
				.slidingWindowType(AbstractConfig.SlidingWindow.TIME_BASED)
				.build();

		final AimdConfig build = AimdConfig.from(config2).build();
		assertThat(build.equals(config2));
		assertThat(AimdConfig.ofDefaults().getMaxLimit()).isEqualTo(200);
		assertThat(config.equals(config2)).isTrue();
		assertThat(config.hashCode() == config2.hashCode()).isTrue();
	}


}