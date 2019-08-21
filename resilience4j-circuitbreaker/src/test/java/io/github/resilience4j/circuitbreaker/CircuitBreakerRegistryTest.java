/*
 *
 *  Copyright 2016 Robert Winkler
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
package io.github.resilience4j.circuitbreaker;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        assertThat(circuitBreakerRegistry.getAllCircuitBreakers()).hasSize(1);
    }

    @Test
    public void shouldBeNotTheSameCircuitBreaker() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker circuitBreaker2 = circuitBreakerRegistry.circuitBreaker("otherTestName");
        assertThat(circuitBreaker).isNotSameAs(circuitBreaker2);

        assertThat(circuitBreakerRegistry.getAllCircuitBreakers()).hasSize(2);
    }


	@Test
    public void testCreateWithDefaultConfiguration() {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(CircuitBreakerConfig.ofDefaults());
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        CircuitBreaker circuitBreaker2 = circuitBreakerRegistry.circuitBreaker("otherTestName");
        assertThat(circuitBreaker).isNotSameAs(circuitBreaker2);

        assertThat(circuitBreakerRegistry.getAllCircuitBreakers()).hasSize(2);
    }

    @Test
    public void testCreateWithCustomConfiguration() {
        float failureRate = 30f;
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom().failureRateThreshold(failureRate).build();

        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");

        assertThat(circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(failureRate);
    }

    @Test
    public void testCreateWithConfigurationMap() {
        Map<String, CircuitBreakerConfig> configs = new HashMap<>();
        configs.put("default", CircuitBreakerConfig.ofDefaults());
        configs.put("custom", CircuitBreakerConfig.ofDefaults());

        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(configs);

        assertThat(circuitBreakerRegistry.getDefaultConfig()).isNotNull();
        assertThat(circuitBreakerRegistry.getConfiguration("custom")).isNotNull();
    }

    @Test
    public void testAddConfiguration() {
        float failureRate = 30f;
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom().failureRateThreshold(failureRate).build();
        circuitBreakerRegistry.addConfiguration("someSharedConfig", circuitBreakerConfig);

        assertThat(circuitBreakerRegistry.getDefaultConfig()).isNotNull();
        assertThat(circuitBreakerRegistry.getConfiguration("someSharedConfig")).isNotNull();

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("name", "someSharedConfig");
        assertThat(circuitBreaker.getCircuitBreakerConfig()).isEqualTo(circuitBreakerConfig);
        assertThat(circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(failureRate);
    }

    @Test
    public void testCreateWithConfigurationMapWithoutDefaultConfig() {
        float failureRate = 30f;
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom().failureRateThreshold(failureRate).build();
        Map<String, CircuitBreakerConfig> configs = new HashMap<>();
        configs.put("someSharedConfig", circuitBreakerConfig);

        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(configs);

        assertThat(circuitBreakerRegistry.getDefaultConfig()).isNotNull();
        assertThat(circuitBreakerRegistry.getConfiguration("someSharedConfig")).isNotNull();

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("name", "someSharedConfig");

        assertThat(circuitBreaker.getCircuitBreakerConfig()).isEqualTo(circuitBreakerConfig);
        assertThat(circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(failureRate);
    }

    @Test
    public void testCreateWithNullConfig() {
        assertThatThrownBy(() -> CircuitBreakerRegistry.of((CircuitBreakerConfig)null)).isInstanceOf(NullPointerException.class).hasMessage("Config must not be null");
    }
}
