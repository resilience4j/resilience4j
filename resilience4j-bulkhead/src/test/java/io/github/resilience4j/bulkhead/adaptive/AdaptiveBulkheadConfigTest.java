package io.github.resilience4j.bulkhead.adaptive;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Java6Assertions.assertThat;

import org.junit.Test;

import io.github.resilience4j.bulkhead.adaptive.internal.config.MovingAverageConfig;
import io.github.resilience4j.bulkhead.adaptive.internal.config.PercentileConfig;

/**
 *
 */
public class AdaptiveBulkheadConfigTest {
	@Test
	public void testBuildCustom() {
		AdaptiveBulkheadConfig<MovingAverageConfig> config = AdaptiveBulkheadConfig.<MovingAverageConfig>builder().config(MovingAverageConfig.builder().concurrencyDropMultiplier(0.3).desirableAverageThroughput(3)
				.desirableOperationLatency(150).lowLatencyMultiplier(0.4).maxAcceptableRequestLatency(200)
				.windowForAdaptation(50).windowForReconfiguration(900).build()).build();

		assertThat(config).isNotNull();
		assertThat(config.getConfiguration().getConcurrencyDropMultiplier()).isEqualTo(0.3);
		assertThat(config.getConfiguration().getDesirableAverageThroughput()).isEqualTo(3);
		assertThat(config.getConfiguration().getDesirableOperationLatency()).isEqualTo(150);
		assertThat(config.getConfiguration().getLowLatencyMultiplier()).isEqualTo(0.4);
		assertThat(config.getConfiguration().getMaxAcceptableRequestLatency()).isEqualTo(200);
		assertThat(config.getConfiguration().getWindowForAdaptation()).isEqualTo(50);
		assertThat(config.getConfiguration().getWindowForReconfiguration()).isEqualTo(900);

	}

	@Test
	public void testNotSetLowLatencyMultiplierConfig() {
		assertThatThrownBy(() -> MovingAverageConfig.builder().lowLatencyMultiplier(0).build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("lowLatencyMultiplier must be a positive value greater than zero");
	}


	@Test
	public void testNotConcurrencyDropMultiplierConfig() {
		assertThatThrownBy(() -> MovingAverageConfig.builder().concurrencyDropMultiplier(0).build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("concurrencyDropMultiplier must be a positive value greater than zero");
	}

	@Test
	public void testNotSetDesirableOperationLatencyConfig() {
		assertThatThrownBy(() -> MovingAverageConfig.builder().desirableAverageThroughput(0).maxAcceptableRequestLatency(300).build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("desirableAverageThroughput must be a positive value greater than zero");
	}

	@Test
	public void testNotSetMaxAcceptableRequestLatencyConfig() {
		assertThatThrownBy(() -> MovingAverageConfig.builder().desirableOperationLatency(5).maxAcceptableRequestLatency(0)
				.build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("maxAcceptableRequestLatency must be a positive value greater than zero");
	}

	@Test
	public void testNonValidValuesConfig() {
		assertThatThrownBy(() -> MovingAverageConfig.builder().concurrencyDropMultiplier(0.3).desirableAverageThroughput(3)
				.desirableOperationLatency(150).lowLatencyMultiplier(0.4).maxAcceptableRequestLatency(200)
				.windowForAdaptation(50).windowForReconfiguration(50).build())
				.isInstanceOf(IllegalArgumentException.class).hasMessage("windowForReconfiguration is too small. windowForReconfiguration should be at least 15 times bigger than windowForAdaptation.");
	}


	@Test
	public void testNotConcurrencyDropMultiplierConfigPercentile() {
		assertThatThrownBy(() -> PercentileConfig.builder().concurrencyDropMultiplier(0).build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("concurrencyDropMultiplier must be a positive value greater than zero");
	}

	@Test
	public void testNotSetDesirableOperationLatencyConfigPercentile() {
		assertThatThrownBy(() -> PercentileConfig.builder().desirableAverageThroughput(0).maxAcceptableRequestLatency(300).build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("desirableAverageThroughput must be a positive value greater than zero");
	}

	@Test
	public void testNotSetMaxAcceptableRequestLatencyConfigPercentile() {
		assertThatThrownBy(() -> PercentileConfig.builder().desirableOperationLatency(5).maxAcceptableRequestLatency(0)
				.build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("maxAcceptableRequestLatency must be a positive value greater than zero");
	}

	@Test
	public void testNonValidValuesConfigPercentile() {
		assertThatThrownBy(() -> PercentileConfig.builder().concurrencyDropMultiplier(0.3).desirableAverageThroughput(3)
				.desirableOperationLatency(150).lowLatencyMultiplier(0.4).maxAcceptableRequestLatency(200)
				.windowForAdaptation(50).windowForReconfiguration(50).build())
				.isInstanceOf(IllegalArgumentException.class).hasMessage("windowForReconfiguration is too small. windowForReconfiguration should be at least 15 times bigger than windowForAdaptation.");
	}


	@Test
	public void testEqual() {
		MovingAverageConfig config = MovingAverageConfig.builder().concurrencyDropMultiplier(0.3).desirableAverageThroughput(3)
				.desirableOperationLatency(150).lowLatencyMultiplier(0.4).maxAcceptableRequestLatency(200)
				.windowForAdaptation(50).windowForReconfiguration(900).build();
		MovingAverageConfig config2 = MovingAverageConfig.builder().concurrencyDropMultiplier(0.3).desirableAverageThroughput(3)
				.desirableOperationLatency(150).lowLatencyMultiplier(0.4).maxAcceptableRequestLatency(200)
				.windowForAdaptation(50).windowForReconfiguration(900).build();
		assertThat(config.equals(config2)).isTrue();
		assertThat(config.hashCode() == config2.hashCode()).isTrue();
	}


	@Test
	public void testEqualPercentile() {
		PercentileConfig config = PercentileConfig.builder().concurrencyDropMultiplier(0.3).desirableAverageThroughput(3)
				.desirableOperationLatency(150).lowLatencyMultiplier(0.4).maxAcceptableRequestLatency(200)
				.windowForAdaptation(50).windowForReconfiguration(900).build();
		PercentileConfig config2 = PercentileConfig.builder().concurrencyDropMultiplier(0.3).desirableAverageThroughput(3)
				.desirableOperationLatency(150).lowLatencyMultiplier(0.4).maxAcceptableRequestLatency(200)
				.windowForAdaptation(50).windowForReconfiguration(900).build();
		assertThat(config.equals(config2)).isTrue();
		assertThat(config.hashCode() == config2.hashCode()).isTrue();
	}

}