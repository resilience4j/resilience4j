/*
 *
 * Copyright 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package io.github.resilience4j.common.circuitbreaker.configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class CommonCircuitBreakerConfigurationPropertiesTest {
    @Test
    void maxWaitDurationInHalfOpenStateLessThanSecondShouldFail() {
CommonCircuitBreakerConfigurationProperties.InstanceProperties instanceProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        assertThatThrownBy(() -> instanceProperties.setMaxWaitDurationInHalfOpenState(Duration.ofMillis(-1)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void maxWaitDurationInHalfOpenStateEqualZeroShouldPass() {
        CommonCircuitBreakerConfigurationProperties.InstanceProperties instanceProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        instanceProperties.setMaxWaitDurationInHalfOpenState(Duration.ofMillis(0));
        assertThat(instanceProperties.getMaxWaitDurationInHalfOpenState().getSeconds()).isZero();
    }

    @Test
    void transitionToStateAfterWaitDurationEqualOpenShouldPass() {
        CommonCircuitBreakerConfigurationProperties.InstanceProperties instanceProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        instanceProperties.setTransitionToStateAfterWaitDuration(CircuitBreaker.State.OPEN);
        assertThat(instanceProperties.getTransitionToStateAfterWaitDuration()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void transitionToStateAfterWaitDurationEqualClosedShouldPass() {
        CommonCircuitBreakerConfigurationProperties.InstanceProperties instanceProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        instanceProperties.setTransitionToStateAfterWaitDuration(CircuitBreaker.State.CLOSED);
        assertThat(instanceProperties.getTransitionToStateAfterWaitDuration()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void transitionToStateAfterWaitDurationEqualHalfOpenShouldFail() {
CommonCircuitBreakerConfigurationProperties.InstanceProperties instanceProperties = new CommonCircuitBreakerConfigurationProperties.InstanceProperties();
        assertThatThrownBy(() -> instanceProperties.setTransitionToStateAfterWaitDuration(CircuitBreaker.State.HALF_OPEN))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
