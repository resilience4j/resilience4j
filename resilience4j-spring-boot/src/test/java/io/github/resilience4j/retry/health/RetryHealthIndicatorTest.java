package io.github.resilience4j.retry.health;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.AbstractMap.SimpleEntry;

import org.junit.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.monitoring.health.RetryHealthIndicator;

/**
 * @author mahmoud romeh
 */
public class RetryHealthIndicatorTest {
	@Test
	public void health() throws Exception {
		// given
		RetryConfig config = mock(RetryConfig.class);
		Retry.Metrics metrics = mock(Retry.Metrics.class);
		Retry retry = mock(Retry.class);
		RetryHealthIndicator healthIndicator = new RetryHealthIndicator(retry);

		//when
		when(config.getMaxAttempts()).thenReturn(3);

		when(metrics.getNumberOfFailedCallsWithRetryAttempt()).thenReturn(20L);
		when(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).thenReturn(100L);
		when(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).thenReturn(100L);
		when(metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt()).thenReturn(20L);


		when(retry.getRetryConfig()).thenReturn(config);
		when(retry.getMetrics()).thenReturn(metrics);


		// then
		Health health = healthIndicator.health();
		then(health.getStatus()).isEqualTo(Status.UP);


		then(health.getDetails())
				.contains(
						entry("successCalls", 20L)
				);
	}

	private SimpleEntry<String, ?> entry(String key, Object value) {
		return new SimpleEntry<>(key, value);
	}
}