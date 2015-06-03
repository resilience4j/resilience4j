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

import javaslang.control.Try;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class FunctionalTest {

    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Before
    public void setUp(){
        circuitBreakerRegistry = new InMemoryCircuitBreakerRegistry(new CircuitBreakerConfig.Builder().maxFailures(1).waitInterval(1000).build());
    }

    @Test
    public void shouldReturnFailureWithCircuitBreakerOpenException() {
        // Given
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        assertThat(circuitBreaker.isClosed()).isTrue();
        circuitBreaker.recordFailure();
        assertThat(circuitBreaker.isClosed()).isTrue();
        circuitBreaker.recordFailure();
        assertThat(circuitBreaker.isClosed()).isFalse();

        //When
        CircuitBreaker.CheckedSupplier<String> checkedSupplier = CircuitBreaker.CheckedSupplier.of(() -> {
            throw new RuntimeException("BAM!");
        }, circuitBreaker);
        Try<String> result = Try.of(checkedSupplier);

        //Then
        assertThat(result.isFailure()).isTrue();
        assertThat(circuitBreaker.isClosed()).isFalse();
        assertThat(result.failed().get()).isInstanceOf(CircuitBreakerOpenException.class);
    }

    @Test
    public void shouldReturnFailureWithRuntimeException() {
        // Given
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        assertThat(circuitBreaker.isClosed()).isTrue();

        //When
        CircuitBreaker.CheckedSupplier<String> checkedSupplier = CircuitBreaker.CheckedSupplier.of(() -> {
            throw new RuntimeException("BAM!");
        }, circuitBreaker);

        Try<String> result = Try.of(checkedSupplier);

        //Then
        assertThat(result.isFailure()).isTrue();
        assertThat(circuitBreaker.isClosed()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void shouldReturnSuccess() {
        // Given
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        assertThat(circuitBreaker.isClosed()).isTrue();

        //When
        CircuitBreaker.CheckedSupplier<String> checkedSupplier = CircuitBreaker.CheckedSupplier.of(() -> "Hello world", circuitBreaker);
        Try<String> result = Try.of(checkedSupplier);

        //Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("Hello world");
    }

    @Test
    public void shouldReturnWitRecovery() {
        // Given
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        assertThat(circuitBreaker.isClosed()).isTrue();
        circuitBreaker.recordFailure();
        assertThat(circuitBreaker.isClosed()).isTrue();
        circuitBreaker.recordFailure();
        assertThat(circuitBreaker.isClosed()).isFalse();

        //When
        CircuitBreaker.CheckedSupplier<String> checkedSupplier = CircuitBreaker.CheckedSupplier.of(() -> {
            throw new RuntimeException("BAM!");
        }, circuitBreaker);
        Try<String> result = Try.of(checkedSupplier)
                .recover((throwable) -> "Hello Recovery");

        //Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(circuitBreaker.isClosed()).isFalse();
        assertThat(result.get()).isEqualTo("Hello Recovery");
    }

    @Test
    public void shouldInvokeMap() {
        // Given
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        //When
        Try<String> result = Try.of(CircuitBreaker.CheckedSupplier.of(() -> "Hello", circuitBreaker))
                .map(value -> value + " world");

        //Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("Hello world");
    }

    @Test
    public void testReadmeExample() {
        // Given
        CircuitBreakerRegistry circuitBreakerRegistry = new InMemoryCircuitBreakerRegistry();

        // First parameter is maximum number of failures allowed
        // Second parameter is the wait interval [ms] and specifies how long the CircuitBreaker should stay OPEN
        CircuitBreakerConfig circuitBreakerConfig = new CircuitBreakerConfig.Builder().maxFailures(1).waitInterval(1000).build();

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("uniqueName", circuitBreakerConfig);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED); // CircuitBreaker is initially CLOSED
        circuitBreaker.recordFailure();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED); // CircuitBreaker is still CLOSED, because 1 failure is allowed
        circuitBreaker.recordFailure();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN); // CircuitBreaker is OPEN, because maxFailures > 1

        // When
        // Wrap a standard Java8 Supplier with a CircuitBreaker
        Try<String> result = Try.of(CircuitBreaker.CheckedSupplier.of(() -> "Hello", circuitBreaker))
                .map(value -> value + " world");

        // Then
        assertThat(result.isFailure()).isTrue(); // Call fails, because CircuitBreaker is OPEN
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN); // CircuitBreaker is OPEN, because maxFailures > 1
        assertThat(result.failed().get()).isInstanceOf(CircuitBreakerOpenException.class); // Exception was CircuitBreakerOpenException
    }

    @Test
    public void shouldReturnWithRecoveryAsync() throws ExecutionException, InterruptedException {
        // Given
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");

        //When
        CircuitBreaker.CheckedSupplier<String> checkedSupplier = CircuitBreaker.CheckedSupplier.of(() -> {
            Thread.sleep(1000);
            throw new RuntimeException("BAM!");
        }, circuitBreaker);
        CompletableFuture<Try<String>> future = CompletableFuture.supplyAsync(() -> Try.of(checkedSupplier)
                .recover((throwable) -> "Hello Recovery"));

        //Then
        Try<String> result = future.get();
        assertThat(result.isSuccess()).isTrue();
        assertThat(circuitBreaker.isClosed()).isTrue();
        assertThat(result.get()).isEqualTo("Hello Recovery");
    }

}
