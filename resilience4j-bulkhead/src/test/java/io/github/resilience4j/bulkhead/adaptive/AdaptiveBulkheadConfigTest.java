package io.github.resilience4j.bulkhead.adaptive;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Java6Assertions.assertThat;

import org.junit.Test;

import io.github.resilience4j.bulkhead.adaptive.internal.config.AIMDConfig;
import io.github.resilience4j.bulkhead.adaptive.internal.config.AbstractConfig;


/**
 *
 */
public class AdaptiveBulkheadConfigTest {
	@Test
	public void testBuildCustom() {
		AdaptiveBulkheadConfig<AIMDConfig> config = AdaptiveBulkheadConfig.<AIMDConfig>builder()
				.config(AIMDConfig.builder()
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
		final AIMDConfig build = AIMDConfig.builder().concurrencyDropMultiplier(0.0).build();
		assertThat(build.getConcurrencyDropMultiplier()).isEqualTo(0.85d);
	}

	@Test
	public void testNotSetDesirableOperationLatencyConfig() {
		assertThatThrownBy(() -> AIMDConfig.builder().slowCallDurationThreshold(0).build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("slowCallDurationThreshold must be a positive value greater than zero");
	}

	@Test
	public void testNotSetMaxAcceptableRequestLatencyConfig() {
		assertThatThrownBy(() -> AIMDConfig.builder().maxConcurrentRequestsLimit(0)
				.build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("maxConcurrentRequestsLimit must be a positive value greater than zero");
	}

	@Test
	public void testNotSetMinAcceptableRequestLatencyConfig() {
		assertThatThrownBy(() -> AIMDConfig.builder().minConcurrentRequestsLimit(0)
				.build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("minConcurrentRequestsLimit must be a positive value greater than zero");
	}

	@Test
	public void testlimitIncrementInflightFactorConfig() {
		assertThatThrownBy(() -> AIMDConfig.builder().limitIncrementInflightFactor(0)
				.build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("limitIncrementInflightFactor must be a positive value greater than zero");
	}


	@Test
	public void testEqual() {
		AIMDConfig config = AIMDConfig.builder()
				.concurrencyDropMultiplier(0.3)
				.minConcurrentRequestsLimit(3)
				.maxConcurrentRequestsLimit(3)
				.slowCallDurationThreshold(150)
				.slowCallRateThreshold(50)
				.failureRateThreshold(50)
				.slidingWindowTime(5)
				.slidingWindowSize(100)
				.slidingWindowType(AbstractConfig.SlidingWindow.TIME_BASED)
				.build();
		AIMDConfig config2 = AIMDConfig.builder()
				.concurrencyDropMultiplier(0.3)
				.minConcurrentRequestsLimit(3)
				.maxConcurrentRequestsLimit(3)
				.slowCallDurationThreshold(150)
				.slowCallRateThreshold(50)
				.failureRateThreshold(50)
				.slidingWindowTime(5)
				.slidingWindowSize(100)
				.slidingWindowType(AbstractConfig.SlidingWindow.TIME_BASED)
				.build();
		assertThat(config.equals(config2)).isTrue();
		assertThat(config.hashCode() == config2.hashCode()).isTrue();
	}


}