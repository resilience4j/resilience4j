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
package io.github.resilience4j.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class CircuitBreakerOperatorTest {

    @Test
    public void shouldReturnOnCompleteUsingSingle() {
        //Given
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        Single.just(1)
            .lift(CircuitBreakerOperator.of(circuitBreaker))
                .test()
                .assertValueCount(1)
                .assertValues(1)
                .assertComplete();

        //Then
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
    }

    @Test
    public void shouldReturnOnErrorUsingUsingSingle() {
        //Given
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        Single.fromCallable(() -> {throw new IOException("BAM!");})
            .lift(CircuitBreakerOperator.of(circuitBreaker))
                .test()
                .assertError(IOException.class)
                .assertNotComplete()
                .assertSubscribed();

        //Then
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
    }

    @Test
    public void shouldReturnOnCompleteUsingObservable() {
        //Given
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        //When
        Observable.fromArray("Event 1", "Event 2")
            .lift(CircuitBreakerOperator.of(circuitBreaker))
            .test()
                .assertValueCount(2)
                .assertValues("Event 1", "Event 2")
                .assertComplete();

        //Then
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
    }

    @Test
    public void shouldReturnOnCompleteUsingFlowable() {
        //Given
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        //When
        Flowable.fromArray("Event 1", "Event 2")
                .lift(CircuitBreakerOperator.of(circuitBreaker))
                .test()
                .assertValueCount(2)
                .assertValues("Event 1", "Event 2")
                .assertComplete();

        //Then
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
    }

    @Test
    public void shouldReturnOnErrorUsingObservable() {
        //Given
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        //When
        Observable.fromCallable(() -> {throw new IOException("BAM!");})
            .lift(CircuitBreakerOperator.of(circuitBreaker))
            .test()
                .assertError(IOException.class)
                .assertNotComplete()
                .assertSubscribed();

        //Then
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
    }

    @Test
    public void shouldReturnOnErrorUsingFlowable() {
        //Given
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        //When
        Flowable.fromCallable(() -> {throw new IOException("BAM!");})
                .lift(CircuitBreakerOperator.of(circuitBreaker))
                .test()
                .assertError(IOException.class)
                .assertNotComplete()
                .assertSubscribed();

        //Then
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
    }

    @Test
    public void shouldReturnOnErrorWithCircuitBreakerOpenExceptionUsingObservable() {
        // Given
        // Create a custom configuration for a CircuitBreaker
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .ringBufferSizeInClosedState(2)
                .ringBufferSizeInHalfOpenState(2)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .build();

        // Create a CircuitBreakerRegistry with a custom global configuration
        CircuitBreaker circuitBreaker = CircuitBreaker.of("testName", circuitBreakerConfig);
        circuitBreaker.onError(Duration.ZERO, new RuntimeException("Bla"));
        circuitBreaker.onError(Duration.ZERO, new RuntimeException("Bla"));

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        Observable.fromArray("Event 1", "Event 2")
                .lift(CircuitBreakerOperator.of(circuitBreaker))
                .test()
                .assertError(CircuitBreakerOpenException.class)
                .assertNotComplete()
                .assertSubscribed();

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(2);
    }

    @Test
    public void shouldReturnOnErrorWithCircuitBreakerOpenExceptionFlowable() {
        // Given
        // Create a custom configuration for a CircuitBreaker
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .ringBufferSizeInClosedState(2)
                .ringBufferSizeInHalfOpenState(2)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .build();

        // Create a CircuitBreakerRegistry with a custom global configuration
        CircuitBreaker circuitBreaker = CircuitBreaker.of("testName", circuitBreakerConfig);
        circuitBreaker.onError(Duration.ZERO, new RuntimeException("Bla"));
        circuitBreaker.onError(Duration.ZERO, new RuntimeException("Bla"));

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        Flowable.fromArray("Event 1", "Event 2")
                .lift(CircuitBreakerOperator.of(circuitBreaker))
                .test()
                .assertError(CircuitBreakerOpenException.class)
                .assertNotComplete()
                .assertSubscribed();

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(2);
    }
    @Test
    public void shouldReturnOnErrorAndWithIOExceptionUsingObservable() {
        // Given
        // Create a custom configuration for a CircuitBreaker
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .ringBufferSizeInClosedState(2)
                .ringBufferSizeInHalfOpenState(2)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .build();

        // Create a CircuitBreakerRegistry with a custom global configuration
        CircuitBreaker circuitBreaker = CircuitBreaker.of("testName", circuitBreakerConfig);
        circuitBreaker.onError(Duration.ZERO, new RuntimeException("Bla"));

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        Observable.fromCallable(() -> {throw new IOException("BAM!");})
                .lift(CircuitBreakerOperator.of(circuitBreaker))
                .test()
                .assertError(IOException.class)
                .assertNotComplete()
                .assertSubscribed();

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(2);
    }
}
