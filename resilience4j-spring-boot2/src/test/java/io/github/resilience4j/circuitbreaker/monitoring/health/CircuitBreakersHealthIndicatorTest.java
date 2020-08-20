package io.github.resilience4j.circuitbreaker.monitoring.health;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfigurationProperties;
import org.junit.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.SimpleStatusAggregator;
import org.springframework.boot.actuate.health.Status;

import java.util.AbstractMap.SimpleEntry;
import java.util.*;

import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.*;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author bstorozhuk
 */
public class CircuitBreakersHealthIndicatorTest {

    @Test
    public void healthMetricsAndConfig() {
        // given
        CircuitBreakerConfig config = mock(CircuitBreakerConfig.class);
        CircuitBreakerRegistry registry = mock(CircuitBreakerRegistry.class);
        CircuitBreaker.Metrics metrics = mock(CircuitBreaker.Metrics.class);
        CircuitBreaker circuitBreaker = mock(CircuitBreaker.class);
        CircuitBreakerConfigurationProperties.InstanceProperties instanceProperties =
            mock(CircuitBreakerConfigurationProperties.InstanceProperties.class);
        CircuitBreakerConfigurationProperties circuitBreakerProperties = mock(
            CircuitBreakerConfigurationProperties.class);
        CircuitBreakersHealthIndicator healthIndicator =
            new CircuitBreakersHealthIndicator(registry, circuitBreakerProperties, new SimpleStatusAggregator());

        //when
        when(config.getFailureRateThreshold()).thenReturn(30f);
        when(metrics.getFailureRate()).thenReturn(20f);
        when(metrics.getSlowCallRate()).thenReturn(20f);
        when(config.getSlowCallRateThreshold()).thenReturn(50f);
        when(metrics.getNumberOfBufferedCalls()).thenReturn(100);
        when(metrics.getNumberOfFailedCalls()).thenReturn(20);
        when(metrics.getNumberOfSlowCalls()).thenReturn(20);
        when(metrics.getNumberOfNotPermittedCalls()).thenReturn(0L);

        when(registry.getAllCircuitBreakers()).thenReturn(Set.of(circuitBreaker));
        when(circuitBreaker.getName()).thenReturn("test");
        when(circuitBreakerProperties.findCircuitBreakerProperties("test"))
            .thenReturn(Optional.of(instanceProperties));
        when(instanceProperties.getRegisterHealthIndicator()).thenReturn(true);
        when(instanceProperties.getAllowHealthIndicatorToFail()).thenReturn(true);
        when(circuitBreaker.getMetrics()).thenReturn(metrics);
        when(circuitBreaker.getCircuitBreakerConfig()).thenReturn(config);
        when(circuitBreaker.getState()).thenReturn(CLOSED, OPEN, HALF_OPEN, CLOSED);

        // then
        Health health = healthIndicator.health();
        then(health.getStatus()).isEqualTo(Status.UP);
        then(health.getDetails()).containsKey("test");
        then(health.getDetails().get("test")).isInstanceOf(Health.class);
        then(((Health) health.getDetails().get("test")).getDetails())
            .contains(
                entry("failureRate", "20.0%"),
                entry("slowCallRate", "20.0%"),
                entry("slowCallRateThreshold", "50.0%"),
                entry("failureRateThreshold", "30.0%"),
                entry("bufferedCalls", 100),
                entry("slowCalls", 20),
                entry("failedCalls", 20),
                entry("notPermittedCalls", 0L)
            );
    }

    @Test
    public void testHealthStatus() {
        CircuitBreaker openCircuitBreaker = mock(CircuitBreaker.class);
        CircuitBreaker halfOpenCircuitBreaker = mock(CircuitBreaker.class);
        CircuitBreaker closeCircuitBreaker = mock(CircuitBreaker.class);

        Map<CircuitBreaker.State, CircuitBreaker> expectedStateToCircuitBreaker = new HashMap<>();
        expectedStateToCircuitBreaker.put(OPEN, openCircuitBreaker);
        expectedStateToCircuitBreaker.put(HALF_OPEN, halfOpenCircuitBreaker);
        expectedStateToCircuitBreaker.put(CLOSED, closeCircuitBreaker);
        CircuitBreakerConfigurationProperties.InstanceProperties instanceProperties =
            mock(CircuitBreakerConfigurationProperties.InstanceProperties.class);
        CircuitBreakerConfigurationProperties circuitBreakerProperties = mock(
            CircuitBreakerConfigurationProperties.class);

        // given
        CircuitBreakerRegistry registry = mock(CircuitBreakerRegistry.class);
        CircuitBreakerConfig config = mock(CircuitBreakerConfig.class);
        CircuitBreaker.Metrics metrics = mock(CircuitBreaker.Metrics.class);

        // when
        when(registry.getAllCircuitBreakers()).thenReturn(new HashSet<>(expectedStateToCircuitBreaker.values()));
        boolean allowHealthIndicatorToFail = true;
        expectedStateToCircuitBreaker.forEach(
            (state, circuitBreaker) -> setCircuitBreakerWhen(state, circuitBreaker, config, metrics, instanceProperties, circuitBreakerProperties, allowHealthIndicatorToFail));

        CircuitBreakersHealthIndicator healthIndicator =
            new CircuitBreakersHealthIndicator(registry, circuitBreakerProperties, new SimpleStatusAggregator());

        // then
        Health health = healthIndicator.health();

        then(health.getStatus()).isEqualTo(Status.DOWN);
        then(health.getDetails()).containsKeys(OPEN.name(), HALF_OPEN.name(), CLOSED.name());

        assertState(OPEN, Status.DOWN, health.getDetails());
        assertState(HALF_OPEN, new Status("CIRCUIT_HALF_OPEN"), health.getDetails());
        assertState(CLOSED, Status.UP, health.getDetails());

    }

    @Test
    public void healthIndicatorMaxImpactCanBeOverridden() {
        CircuitBreaker openCircuitBreaker = mock(CircuitBreaker.class);
        CircuitBreaker halfOpenCircuitBreaker = mock(CircuitBreaker.class);
        CircuitBreaker closeCircuitBreaker = mock(CircuitBreaker.class);

        Map<CircuitBreaker.State, CircuitBreaker> expectedStateToCircuitBreaker = new HashMap<>();
        expectedStateToCircuitBreaker.put(OPEN, openCircuitBreaker);
        expectedStateToCircuitBreaker.put(HALF_OPEN, halfOpenCircuitBreaker);
        expectedStateToCircuitBreaker.put(CLOSED, closeCircuitBreaker);
        CircuitBreakerConfigurationProperties.InstanceProperties instanceProperties =
            mock(CircuitBreakerConfigurationProperties.InstanceProperties.class);
        CircuitBreakerConfigurationProperties circuitBreakerProperties = mock(CircuitBreakerConfigurationProperties.class);

        // given
        CircuitBreakerRegistry registry = mock(CircuitBreakerRegistry.class);
        CircuitBreakerConfig config = mock(CircuitBreakerConfig.class);
        CircuitBreaker.Metrics metrics = mock(CircuitBreaker.Metrics.class);

        // when
        when(registry.getAllCircuitBreakers()).thenReturn(new HashSet<>(expectedStateToCircuitBreaker.values()));
        boolean allowHealthIndicatorToFail = false;  // do not allow health indicator to fail
        expectedStateToCircuitBreaker.forEach(
            (state, circuitBreaker) -> setCircuitBreakerWhen(state, circuitBreaker, config, metrics, instanceProperties, circuitBreakerProperties, allowHealthIndicatorToFail));

        CircuitBreakersHealthIndicator healthIndicator =
            new CircuitBreakersHealthIndicator(registry, circuitBreakerProperties, new SimpleStatusAggregator());


        // then
        Health health = healthIndicator.health();

        then(health.getStatus()).isEqualTo(Status.UP);
        then(health.getDetails()).containsKeys(OPEN.name(), HALF_OPEN.name(), CLOSED.name());

        assertState(OPEN, new Status("CIRCUIT_OPEN"), health.getDetails());
        assertState(HALF_OPEN, new Status("CIRCUIT_HALF_OPEN"), health.getDetails());
        assertState(CLOSED, Status.UP, health.getDetails());

    }

    private void setCircuitBreakerWhen(CircuitBreaker.State givenState, CircuitBreaker circuitBreaker,
                                       CircuitBreakerConfig config, CircuitBreaker.Metrics metrics,
                                       io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties instanceProperties,
                                       CircuitBreakerConfigurationProperties circuitBreakerProperties,
                                       boolean allowHealthIndicatorToFail) {

        when(circuitBreaker.getName()).thenReturn(givenState.name());
        when(circuitBreaker.getState()).thenReturn(givenState);
        when(circuitBreaker.getCircuitBreakerConfig()).thenReturn(config);
        when(circuitBreaker.getMetrics()).thenReturn(metrics);
        when(circuitBreakerProperties.findCircuitBreakerProperties(givenState.name()))
            .thenReturn(Optional.of(instanceProperties));
        when(instanceProperties.getRegisterHealthIndicator()).thenReturn(true);
        when(instanceProperties.getAllowHealthIndicatorToFail()).thenReturn(allowHealthIndicatorToFail);
    }

    private void assertState(CircuitBreaker.State givenState, Status expectedStatus,
                             Map<String, Object> details) {

        then(details.get(givenState.name())).isInstanceOf(Health.class);
        Health health = (Health) details.get(givenState.name());
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
