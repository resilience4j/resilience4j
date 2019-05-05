/*
 *
 *  Copyright 2017 Oleksandr Goldobin
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
package io.github.resilience4j.prometheus;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.internal.InMemoryCircuitBreakerRegistry;
import io.prometheus.client.CollectorRegistry;
import io.vavr.Tuple;
import io.vavr.collection.HashMap;
import io.vavr.collection.HashSet;
import io.vavr.collection.Map;
import org.junit.Test;

import java.util.function.Supplier;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class CircuitBreakerExportsTest {

    @Test
    public void testExportsCircuitBreakerStates() {
        // Given
        final CollectorRegistry registry = new CollectorRegistry();

        final CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("foo");

        CircuitBreakerExports.ofIterable("boo_circuit_breaker", singletonList(circuitBreaker)).register(registry);

        final Supplier<Map<String, Double>> values = () -> HashSet
                .of("closed", "open", "half_open")
                .map(state ->
                    Tuple.of(state, registry.getSampleValue(
                        "boo_circuit_breaker_states",
                        new String[]{ "name", "state" },
                        new String[]{ "foo", state})))
                .toMap(t -> t);

        // When

        final Map<String, Double> closedStateValues = values.get();

        circuitBreaker.transitionToOpenState();

        final Map<String, Double> openStateValues = values.get();

        circuitBreaker.transitionToHalfOpenState();

        final Map<String, Double> halfOpenStateValues = values.get();

        circuitBreaker.transitionToClosedState();

        final Map<String, Double> closedStateValues2 = values.get();

        // Then

        assertThat(closedStateValues).isEqualTo(HashMap.of(
                "closed", 1.0,
                "open", 0.0,
                "half_open", 0.0));

        assertThat(openStateValues).isEqualTo(HashMap.of(
                "closed", 0.0,
                "open", 1.0,
                "half_open", 0.0));

        assertThat(halfOpenStateValues).isEqualTo(HashMap.of(
                "closed", 0.0,
                "open", 0.0,
                "half_open", 1.0));

        assertThat(closedStateValues2).isEqualTo(HashMap.of(
                "closed", 1.0,
                "open", 0.0,
                "half_open", 0.0));
    }

    @Test
    public void testExportsCircuitBreakerMetrics() {
        // Given
        final CollectorRegistry registry = new CollectorRegistry();

        final CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("foo");

        CircuitBreakerExports.ofIterable("boo_circuit_breaker", singletonList(circuitBreaker)).register(registry);

        final Supplier<Map<String, Double>> values = () -> HashSet
                .of("successful", "failed", "not_permitted", "buffered", "buffered_max")
                .map(callType ->
                        Tuple.of(callType, registry.getSampleValue(
                                "boo_circuit_breaker_calls",
                                new String[]{ "name", "call_result" },
                                new String[]{ "foo", callType})))
                .toMap(t -> t);

        // When

        final Map<String, Double> initialValues = values.get();

        circuitBreaker.executeRunnable(() -> {});

        final Map<String, Double> afterSuccessValues = values.get();

        try {
            circuitBreaker.executeRunnable(() -> {
                throw new SomeAppException("Some exception");
            });
        } catch (RuntimeException e) {
            // expected
        }

        final Map<String, Double> afterFailureValues = values.get();

        circuitBreaker.transitionToOpenState();

        try {
            circuitBreaker.executeRunnable(() -> {});
        } catch (CircuitBreakerOpenException e) {
            // expected
        }

        final Map<String, Double> afterDeclinedValues = values.get();

        // Then

        assertThat(initialValues).isEqualTo(HashMap.of(
                "successful", 0.0,
                "failed", 0.0,
                "not_permitted", 0.0,
                "buffered", 0.0,
                "buffered_max", 100.0));

        assertThat(afterSuccessValues).isEqualTo(HashMap.of(
                "successful", 1.0,
                "failed", 0.0,
                "not_permitted", 0.0,
                "buffered", 1.0,
                "buffered_max", 100.0));

        assertThat(afterFailureValues).isEqualTo(HashMap.of(
                "successful", 1.0,
                "failed", 1.0,
                "not_permitted", 0.0,
                "buffered", 2.0,
                "buffered_max", 100.0));

        assertThat(afterDeclinedValues).isEqualTo(HashMap.of(
                "successful", 1.0,
                "failed", 1.0,
                "not_permitted", 1.0,
                "buffered", 2.0,
                "buffered_max", 100.0));
    }

    @Test
    public void testConstructors() {
        final CircuitBreakerRegistry registry = new InMemoryCircuitBreakerRegistry();

        assertThat(CircuitBreakerExports.ofIterable("boo_breakers", singleton(CircuitBreaker.ofDefaults("foo")))).isNotNull();
        assertThat(CircuitBreakerExports.ofCircuitBreakerRegistry("boo_breakers", registry)).isNotNull();
        assertThat(CircuitBreakerExports.ofSupplier("boo_breakers", () -> singleton(CircuitBreaker.ofDefaults("foo")))).isNotNull();

        assertThat(CircuitBreakerExports.ofIterable(singleton(CircuitBreaker.ofDefaults("foo")))).isNotNull();
        assertThat(CircuitBreakerExports.ofCircuitBreakerRegistry(registry)).isNotNull();
        assertThat(CircuitBreakerExports.ofSupplier(() -> singleton(CircuitBreaker.ofDefaults("foo")))).isNotNull();
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullName() {
        CircuitBreakerExports.ofCircuitBreaker(null, CircuitBreaker.ofDefaults("foo"));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullSupplier() {
        CircuitBreakerExports.ofCircuitBreaker("boo_breakers", null);
    }

    private static class SomeAppException extends RuntimeException {
        SomeAppException(String message) {
            super(message);
        }
    }
}
