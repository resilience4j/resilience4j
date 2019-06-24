package io.github.resilience4j.bulkhead.adaptive;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Java6Assertions.assertThat;

import java.time.Duration;

import org.junit.Test;

/**
 *
 */
public class AdaptiveBulkheadConfigTest {
	@Test
	public void testBuildCustom() {
		AdaptiveBulkheadConfig config = AdaptiveBulkheadConfig.builder().concurrencyDropMultiplier(0.3).desirableAverageThroughput(3)
				.desirableOperationLatency(150).lowLatencyMultiplier(0.4).maxAcceptableRequestLatency(200)
				.windowForAdaptation(Duration.ofSeconds(50)).windowForReconfiguration(Duration.ofSeconds(750)).build();

		assertThat(config).isNotNull();
		assertThat(config.getConcurrencyDropMultiplier()).isEqualTo(0.3);
		assertThat(config.getDesirableAverageThroughput()).isEqualTo(3);
		assertThat(config.getDesirableOperationLatency()).isEqualTo(150);
		assertThat(config.getLowLatencyMultiplier()).isEqualTo(0.4);
		assertThat(config.getMaxAcceptableRequestLatency()).isEqualTo(200);
		assertThat(config.getWindowForAdaptation().getSeconds()).isEqualTo(50);
		assertThat(config.getWindowForReconfiguration().getSeconds()).isEqualTo(750);

	}

	@Test
	public void testNotSetLowLatencyMultiplierConfig() {
		assertThatThrownBy(() -> AdaptiveBulkheadConfig.builder().lowLatencyMultiplier(0).build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("lowLatencyMultiplier must be a positive value greater than zero");
	}


	@Test
	public void testNotConcurrencyDropMultiplierConfig() {
		assertThatThrownBy(() -> AdaptiveBulkheadConfig.builder().concurrencyDropMultiplier(0).build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("concurrencyDropMultiplier must be a positive value greater than zero");
	}

	@Test
	public void testNotSetDesirableOperationLatencyConfig() {
		assertThatThrownBy(() -> AdaptiveBulkheadConfig.builder().desirableAverageThroughput(0).maxAcceptableRequestLatency(300).build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("desirableAverageThroughput must be a positive value greater than zero");
	}

	@Test
	public void testNotSetMaxAcceptableRequestLatencyConfig() {
		assertThatThrownBy(() -> AdaptiveBulkheadConfig.builder().desirableOperationLatency(5).maxAcceptableRequestLatency(0)
				.build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("maxAcceptableRequestLatency must be a positive value greater than zero");
	}

	@Test
	public void testNonValidValuesConfig() {
		assertThatThrownBy(() -> AdaptiveBulkheadConfig.builder().concurrencyDropMultiplier(0.3).desirableAverageThroughput(3)
				.desirableOperationLatency(150).lowLatencyMultiplier(0.4).maxAcceptableRequestLatency(200)
				.windowForAdaptation(Duration.ofSeconds(50)).windowForReconfiguration(Duration.ofSeconds(50)).build())
				.isInstanceOf(IllegalArgumentException.class).hasMessage("windowForReconfiguration is too small. windowForReconfiguration should be at least 15 times bigger than windowForAdaptation.");
	}

	@Test
	public void testEqual() {
		AdaptiveBulkheadConfig config = AdaptiveBulkheadConfig.builder().concurrencyDropMultiplier(0.3).desirableAverageThroughput(3)
				.desirableOperationLatency(150).lowLatencyMultiplier(0.4).maxAcceptableRequestLatency(200)
				.windowForAdaptation(Duration.ofSeconds(50)).windowForReconfiguration(Duration.ofSeconds(750)).build();
		AdaptiveBulkheadConfig config2 = AdaptiveBulkheadConfig.builder().concurrencyDropMultiplier(0.3).desirableAverageThroughput(3)
				.desirableOperationLatency(150).lowLatencyMultiplier(0.4).maxAcceptableRequestLatency(200)
				.windowForAdaptation(Duration.ofSeconds(50)).windowForReconfiguration(Duration.ofSeconds(750)).build();
		assertThat(config.equals(config2)).isTrue();
		assertThat(config.hashCode() == config2.hashCode()).isTrue();
	}

}