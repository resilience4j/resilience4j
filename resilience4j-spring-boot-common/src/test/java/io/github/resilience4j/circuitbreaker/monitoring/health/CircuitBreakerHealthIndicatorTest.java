package io.github.resilience4j.circuitbreaker.monitoring.health;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;

import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.*;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author bstorozhuk
 */
public class CircuitBreakerHealthIndicatorTest {

    @Test
    public void healthMetricsAndConfig() {
        // given
        CircuitBreakerConfig config = mock(CircuitBreakerConfig.class);
        CircuitBreaker.Metrics metrics = mock(CircuitBreaker.Metrics.class);
        CircuitBreaker circuitBreaker = mock(CircuitBreaker.class);
        CircuitBreakerHealthIndicator healthIndicator = new CircuitBreakerHealthIndicator(circuitBreaker);

        //when
        when(config.getFailureRateThreshold()).thenReturn(0.3f);

        when(metrics.getFailureRate()).thenReturn(0.2f);
        when(metrics.getMaxNumberOfBufferedCalls()).thenReturn(100);
        when(metrics.getNumberOfBufferedCalls()).thenReturn(100);
        when(metrics.getNumberOfFailedCalls()).thenReturn(20);
        when(metrics.getNumberOfNotPermittedCalls()).thenReturn(0L);

        when(circuitBreaker.getCircuitBreakerConfig()).thenReturn(config);
        when(circuitBreaker.getMetrics()).thenReturn(metrics);
        when(circuitBreaker.getState()).thenReturn(CLOSED, OPEN, HALF_OPEN, CLOSED);

        // then
        Health health = healthIndicator.health();
        then(health.getStatus()).isEqualTo(Status.UP);
        then(health.getDetails())
                .contains(
                        entry("failureRate", "0.2%"),
                        entry("setFailureRateThreshold", "0.3%"),
                        entry("bufferedCalls", 100),
                        entry("failedCalls", 20),
                        entry("notPermittedCalls", 0L),
                        entry("maxBufferedCalls", 100)
                );
    }

    @Test
    public void testHealthStatus() {
        Map<CircuitBreaker.State, Status> expectedStateToStatusMap = new HashMap<>();
        expectedStateToStatusMap.put(OPEN, Status.DOWN);
        expectedStateToStatusMap.put(HALF_OPEN, Status.UNKNOWN);
        expectedStateToStatusMap.put(CLOSED, Status.UP);

        // given
        CircuitBreakerConfig config = mock(CircuitBreakerConfig.class);
        CircuitBreaker.Metrics metrics = mock(CircuitBreaker.Metrics.class);
        CircuitBreaker circuitBreaker = mock(CircuitBreaker.class);

        when(circuitBreaker.getCircuitBreakerConfig()).thenReturn(config);
        when(circuitBreaker.getMetrics()).thenReturn(metrics);

        expectedStateToStatusMap.forEach((state, status) -> assertStatusForGivenState(circuitBreaker, state, status));
    }

    private void assertStatusForGivenState(CircuitBreaker circuitBreaker, CircuitBreaker.State givenState, Status expectedStatus) {
        // given
        when(circuitBreaker.getState()).thenReturn(givenState);
        CircuitBreakerHealthIndicator healthIndicator = new CircuitBreakerHealthIndicator(circuitBreaker);

        // when
        Health health = healthIndicator.health();

        // then
        then(health.getStatus()).isEqualTo(expectedStatus);
        then(health.getDetails())
                .contains(
                        entry("state", givenState)
                );
    }

    private SimpleEntry<String, ?> entry(String key, Object value) {
        return new SimpleEntry<>(key, value);
    }
}