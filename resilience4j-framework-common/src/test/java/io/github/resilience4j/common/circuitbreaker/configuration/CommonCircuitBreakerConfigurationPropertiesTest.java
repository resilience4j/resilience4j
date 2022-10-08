package io.github.resilience4j.common.circuitbreaker.configuration;

import org.junit.Test;

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
}
