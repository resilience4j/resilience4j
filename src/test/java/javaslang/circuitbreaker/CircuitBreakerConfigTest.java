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
    public void ringBufferSizeInHalfClosedStateBelowOneShouldFail() {
        CircuitBreakerConfig.custom().ringBufferSizeInHalfClosedState(0).build();
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
        then(circuitBreakerConfig.getFailureRateThreshold()).isEqualTo(50);
        then(circuitBreakerConfig.getRingBufferSizeInHalfClosedState()).isEqualTo(10);
        then(circuitBreakerConfig.getRingBufferSizeInClosedState()).isEqualTo(100);
        then(circuitBreakerConfig.getWaitDurationInOpenState().getSeconds()).isEqualTo(60);
        then(circuitBreakerConfig.getCircuitBreakerEventListener()).isNotNull();
        then(circuitBreakerConfig.getExceptionPredicate()).isNotNull();
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
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom().ringBufferSizeInHalfClosedState(100).build();
        then(circuitBreakerConfig.getRingBufferSizeInHalfClosedState()).isEqualTo(100);
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
        then(circuitBreakerConfig.getExceptionPredicate()).isNotNull();
    }

    @Test
    public void shouldUseCustomCircuitBreakerEventListener() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .onCircuitBreakerEvent((event) -> LOG.info(event.toString()))
                .build();
        then(circuitBreakerConfig.getCircuitBreakerEventListener()).isNotNull();
    }
}
