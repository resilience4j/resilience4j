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
        CircuitBreakerConfig.custom().maxFailures(0).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroWaitIntervalShouldFail() {
        CircuitBreakerConfig.custom().waitDuration(Duration.ofMillis(0)).build();
    }

    @Test()
    public void shouldSetMaxFailures() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom().maxFailures(5).build();
        then(circuitBreakerConfig.getMaxFailures()).isEqualTo(5);
    }

    @Test()
    public void shouldSetWaitInterval() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom().waitDuration(Duration.ofSeconds(1)).build();
        then(circuitBreakerConfig.getWaitDuration().getSeconds() * 1000).isEqualTo(1000);
    }

    @Test
    public void shouldUseTheDefaultExceptionPredicate() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .build();
        then(circuitBreakerConfig.getExceptionPredicate()).isNotNull();
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

    @Test
    public void shouldUseTheDefaultEventListener() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .build();
        then(circuitBreakerConfig.getCircuitBreakerEventListener()).isNotNull();
    }
}
