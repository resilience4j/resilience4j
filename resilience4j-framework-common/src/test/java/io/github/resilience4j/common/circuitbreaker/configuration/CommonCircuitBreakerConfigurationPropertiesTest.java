package io.github.resilience4j.common.circuitbreaker.configuration;

import org.junit.Test;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;


public class CommonCircuitBreakerConfigurationPropertiesTest {
    @Test(expected = IllegalArgumentException.class)
    public void maxWaitDurationInHalfOpenStateLessThanSecondShouldFail() {
        CommonCircuitBreakerConfigurationProperties.InstanceProperties instanceProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        instanceProperties.setMaxWaitDurationInHalfOpenState(Duration.ofMillis(-1));
    }

    @Test
    public void maxWaitDurationInHalfOpenStateEqualZeroShouldPass() {
        CommonCircuitBreakerConfigurationProperties.InstanceProperties instanceProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        instanceProperties.setMaxWaitDurationInHalfOpenState(Duration.ofMillis(0));
        assertThat(instanceProperties.getMaxWaitDurationInHalfOpenState().getSeconds()).isEqualTo(0);
    }

    @Test
    public void transitionToStateAfterWaitDurationEqualOpenShouldPass() {
        CommonCircuitBreakerConfigurationProperties.InstanceProperties instanceProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        instanceProperties.setTransitionToStateAfterWaitDuration(CircuitBreaker.State.OPEN);
        assertThat(instanceProperties.getTransitionToStateAfterWaitDuration()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    public void transitionToStateAfterWaitDurationEqualClosedShouldPass() {
        CommonCircuitBreakerConfigurationProperties.InstanceProperties instanceProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        instanceProperties.setTransitionToStateAfterWaitDuration(CircuitBreaker.State.CLOSED);
        assertThat(instanceProperties.getTransitionToStateAfterWaitDuration()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void transitionToStateAfterWaitDurationEqualHalfOpenShouldFail() {
        CommonCircuitBreakerConfigurationProperties.InstanceProperties instanceProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        instanceProperties.setTransitionToStateAfterWaitDuration(CircuitBreaker.State.HALF_OPEN);
    }
}
