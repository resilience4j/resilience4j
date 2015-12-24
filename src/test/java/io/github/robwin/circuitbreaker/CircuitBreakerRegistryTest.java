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
package io.github.robwin.circuitbreaker;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.BDDAssertions.assertThat;


public class CircuitBreakerRegistryTest {

    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Before
    public void setUp(){
        circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
    }

    @Test
    public void shouldReturnTheCorrectName() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        assertThat(circuitBreaker).isNotNull();
        assertThat(circuitBreaker.getName()).isEqualTo("testName");
    }

    @Test
    public void shouldBeTheSameCircuitBreaker() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker circuitBreaker2 = circuitBreakerRegistry.circuitBreaker("testName");
        assertThat(circuitBreaker).isSameAs(circuitBreaker2);
    }

    @Test
    public void shouldBeNotTheSameCircuitBreaker() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker circuitBreaker2 = circuitBreakerRegistry.circuitBreaker("otherTestName");

        assertThat(circuitBreaker).isNotSameAs(circuitBreaker2);
    }
}
