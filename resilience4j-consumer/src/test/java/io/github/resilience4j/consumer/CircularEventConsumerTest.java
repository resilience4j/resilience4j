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
package io.github.resilience4j.consumer;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent.Type;
import static org.assertj.core.api.Assertions.assertThat;

public class CircularEventConsumerTest {

    @Test
    public void shouldBufferErrorEvents() {
        // tag::shouldBufferEvents[]
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircularEventConsumer<CircuitBreakerEvent> ringBuffer = new CircularEventConsumer<>(2);
        circuitBreaker.getEventPublisher().onEvent(ringBuffer);
        // end::shouldBufferEvents[]
        assertThat(ringBuffer.getBufferedEvents()).isEmpty();
        assertThat(ringBuffer.getBufferedEventsStream()).isEmpty();

        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException("Bla"));
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException("Bla"));
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException("Bla"));

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(3);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(3);
        //Because capacity is 2
        assertThat(ringBuffer.getBufferedEvents()).hasSize(2);
        assertThat(ringBuffer.getBufferedEventsStream()).hasSize(2);
    }

    @Test
    public void shouldBufferAllEvents() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .slidingWindowSize(3)
            .ignoreExceptions(IOException.class)
            .build();
        CircuitBreaker circuitBreaker = CircuitBreaker.of("testName", circuitBreakerConfig);
        CircularEventConsumer<CircuitBreakerEvent> ringBuffer = new CircularEventConsumer<>(10);
        circuitBreaker.getEventPublisher().onEvent(ringBuffer);
        assertThat(ringBuffer.getBufferedEvents()).isEmpty();
        assertThat(ringBuffer.getBufferedEventsStream()).isEmpty();

        circuitBreaker.onSuccess(0, TimeUnit.NANOSECONDS);
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException("Bla"));
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new IOException("Bla"));
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException("Bla"));

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(3);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(2);
        circuitBreaker.reset();
        CircuitBreaker.Metrics resetMetrics = circuitBreaker.getMetrics();
        assertThat(resetMetrics.getNumberOfBufferedCalls()).isEqualTo(0);
        assertThat(resetMetrics.getNumberOfFailedCalls()).isEqualTo(0);
        //Because circuit emits 2 error events and one state transition event
        assertThat(ringBuffer.getBufferedEvents()).hasSize(8);
        assertThat(ringBuffer.getBufferedEventsStream()).hasSize(8);
        assertThat(ringBuffer.getBufferedEvents()).extracting("eventType")
            .containsExactly(Type.SUCCESS, Type.ERROR, Type.IGNORED_ERROR, Type.ERROR,
                Type.FAILURE_RATE_EXCEEDED, Type.STATE_TRANSITION, Type.STATE_TRANSITION,
                Type.RESET);
        assertThat(ringBuffer.getBufferedEventsStream()).extracting("eventType")
            .containsExactly(Type.SUCCESS, Type.ERROR, Type.IGNORED_ERROR, Type.ERROR,
                Type.FAILURE_RATE_EXCEEDED, Type.STATE_TRANSITION, Type.STATE_TRANSITION,
                Type.RESET);
    }

    @Test
    public void shouldNotBufferEvents() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircularEventConsumer<CircuitBreakerEvent> ringBuffer = new CircularEventConsumer<>(2);
        assertThat(ringBuffer.getBufferedEvents()).isEmpty();
        assertThat(ringBuffer.getBufferedEventsStream()).isEmpty();

        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException("Bla"));
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException("Bla"));
        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException("Bla"));
        //Subscription is too late
        circuitBreaker.getEventPublisher().onEvent(ringBuffer);

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(3);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(3);
        //Because Subscription was too late
        assertThat(ringBuffer.getBufferedEvents()).hasSize(0);
        assertThat(ringBuffer.getBufferedEventsStream()).hasSize(0);
    }
}
