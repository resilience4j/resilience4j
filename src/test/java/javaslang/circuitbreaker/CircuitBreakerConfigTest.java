/*
 *
 *  Copyright 2015 Robert Winkler
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package javaslang.circuitbreaker;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import static org.assertj.core.api.BDDAssertions.then;

public class CircuitBreakerConfigTest {

    private static Logger LOG = LoggerFactory.getLogger(CircuitBreakerConfigTest.class);

    @Test(expected = IllegalArgumentException.class)
    public void zeroMaxFailuresShouldFail() {
        CircuitBreakerConfig.custom().failureRateThreshold(0).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroWaitIntervalShouldFail() {
        CircuitBreakerConfig.custom().waitDurationInOpenState(Duration.ofMillis(0)).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void ringBufferSizeInHalfOpenStateBelowOneShouldFail() {
        CircuitBreakerConfig.custom().ringBufferSizeInHalfOpenState(0).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void ringBufferSizeInClosedStateBelowOneThenShouldFail() {
        CircuitBreakerConfig.custom().ringBufferSizeInClosedState(0).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void failureRateThresholdShouldFail() {
        CircuitBreakerConfig.custom().failureRateThreshold(0).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void failureRateThresholdAboveHundredShouldFail() {
        CircuitBreakerConfig.custom().failureRateThreshold(101).build();
    }

    @Test()
    public void shouldSetDefaultSettings() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.ofDefaults();
        then(circuitBreakerConfig.getFailureRateThreshold()).isEqualTo(CircuitBreakerConfig.DEFAULT_MAX_FAILURE_THRESHOLD);
        then(circuitBreakerConfig.getRingBufferSizeInHalfOpenState()).isEqualTo(CircuitBreakerConfig.DEFAULT_RING_BUFFER_SIZE_IN_HALF_OPEN_STATE);
        then(circuitBreakerConfig.getRingBufferSizeInClosedState()).isEqualTo(CircuitBreakerConfig.DEFAULT_RING_BUFFER_SIZE_IN_CLOSED_STATE);
        then(circuitBreakerConfig.getWaitDurationInOpenState().getSeconds()).isEqualTo(CircuitBreakerConfig.DEFAULT_WAIT_DURATION_IN_OPEN_STATE);
        then(circuitBreakerConfig.getCircuitBreakerEventListener()).isNotNull();
        then(circuitBreakerConfig.getRecordFailurePredicate()).isNotNull();
    }

    @Test()
    public void shouldSetFailureRateThreshold() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom().failureRateThreshold(25).build();
        then(circuitBreakerConfig.getFailureRateThreshold()).isEqualTo(25);
    }

    @Test()
    public void shouldSetRingBufferSizeInClosedState() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom().ringBufferSizeInClosedState(1000).build();
        then(circuitBreakerConfig.getRingBufferSizeInClosedState()).isEqualTo(1000);
    }

    @Test()
    public void shouldSetRingBufferSizeInHalfOpenState() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom().ringBufferSizeInHalfOpenState(100).build();
        then(circuitBreakerConfig.getRingBufferSizeInHalfOpenState()).isEqualTo(100);
    }

    @Test()
    public void shouldSetWaitInterval() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom().waitDurationInOpenState(Duration.ofSeconds(1)).build();
        then(circuitBreakerConfig.getWaitDurationInOpenState().getSeconds()).isEqualTo(1);
    }

    @Test()
    public void shouldUseCustomExceptionPredicate() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .recordFailure((Throwable throwable) -> true).build();
        then(circuitBreakerConfig.getRecordFailurePredicate()).isNotNull();
    }

    @Test
    public void shouldUseCustomCircuitBreakerEventListener() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .onCircuitBreakerEvent((event) -> LOG.info(event.toString()))
                .build();
        then(circuitBreakerConfig.getCircuitBreakerEventListener()).isNotNull();
    }
}
