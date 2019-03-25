package io.github.resilience4j.circuitbreaker.monitoring.health;

import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED;
import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.HALF_OPEN;
import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.AbstractMap.SimpleEntry;

import org.junit.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

/**
 * @author bstorozhuk
 */
public class CircuitBreakerHealthIndicatorTest {
    @Test
    public void health() throws Exception {
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

        health = healthIndicator.health();
        then(health.getStatus()).isEqualTo(Status.DOWN);

        health = healthIndicator.health();
        then(health.getStatus()).isEqualTo(Status.UNKNOWN);

        health = healthIndicator.health();
        then(health.getStatus()).isEqualTo(Status.UP);

        then(health.getDetails())
            .contains(
                entry("failureRate", "0.2%"),
                entry("failureRateThreshold", "0.3%"),
                entry("bufferedCalls", 100),
                entry("failedCalls", 20),
                entry("notPermittedCalls", 0L),
                entry("maxBufferedCalls", 100),
                entry("state", CLOSED)
            );
    }

    private SimpleEntry<String, ?> entry(String key, Object value) {
        return new SimpleEntry<>(key, value);
    }
}