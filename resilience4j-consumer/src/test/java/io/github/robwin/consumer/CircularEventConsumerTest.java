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
package io.github.robwin.consumer;

import org.junit.Test;

import java.io.IOException;
import java.time.Duration;

import javax.xml.ws.WebServiceException;

import io.github.robwin.circuitbreaker.CircuitBreaker;
import io.github.robwin.circuitbreaker.CircuitBreakerConfig;
import io.github.robwin.circuitbreaker.event.CircuitBreakerEvent;
import io.github.robwin.circuitbreaker.event.CircuitBreakerOnErrorEvent;
import javaslang.API;

import static io.github.robwin.circuitbreaker.event.CircuitBreakerEvent.Type;
import static javaslang.API.$;
import static javaslang.API.Case;
import static javaslang.Predicates.instanceOf;
import static org.assertj.core.api.Assertions.assertThat;

public class CircularEventConsumerTest {

    @Test
    public void shouldBufferErrorEvents() {
        // Given

        // tag::shouldBufferEvents[]
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircularEventConsumer<CircuitBreakerOnErrorEvent> ringBuffer = new CircularEventConsumer<>(2);
        circuitBreaker.getEventStream()
                .filter(event -> event.getEventType() == Type.ERROR)
                .cast(CircuitBreakerOnErrorEvent.class)
                .subscribe(ringBuffer);
        // end::shouldBufferEvents[]

        assertThat(ringBuffer.getBufferedEvents()).isEmpty();

        //When
        circuitBreaker.onError(Duration.ZERO, new RuntimeException("Bla"));
        circuitBreaker.onError(Duration.ZERO, new RuntimeException("Bla"));
        circuitBreaker.onError(Duration.ZERO, new RuntimeException("Bla"));

        //Then
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(3);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(3);

        //Should only store 2 events, because capacity is 2
        assertThat(ringBuffer.getBufferedEvents()).hasSize(2);
        //ringBuffer.getBufferedEvents().forEach(event -> LOG.info(event.toString()));
    }

    @Test
    public void shouldBufferAllEvents() {
        // Given
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .ringBufferSizeInClosedState(3)
                .recordFailure(throwable -> API.Match(throwable).of(
                        Case(instanceOf(WebServiceException.class), true),
                        Case($(), false)))
                .build();
        CircuitBreaker circuitBreaker = CircuitBreaker.of("testName", circuitBreakerConfig);
        CircularEventConsumer<CircuitBreakerEvent> ringBuffer = new CircularEventConsumer<>(10);
        circuitBreaker.getEventStream()
                .subscribe(ringBuffer);

        assertThat(ringBuffer.getBufferedEvents()).isEmpty();

        //When
        circuitBreaker.onSuccess(Duration.ZERO);
        circuitBreaker.onError(Duration.ZERO, new WebServiceException("Bla"));
        circuitBreaker.onError(Duration.ZERO, new IOException("Bla"));
        circuitBreaker.onError(Duration.ZERO, new WebServiceException("Bla"));


        //Then
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(3);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(2);

        //Should store 3 events, because circuit emits 2 error events and one state transition event
        assertThat(ringBuffer.getBufferedEvents()).hasSize(5);
        assertThat(ringBuffer.getBufferedEvents()).extracting("eventType")
                .containsExactly(Type.SUCCESS, Type.ERROR, Type.IGNORED_ERROR, Type.ERROR, Type.STATE_TRANSITION);
        //ringBuffer.getBufferedEvents().forEach(event -> LOG.info(event.toString()));
    }

    @Test
    public void shouldNotBufferEvents() {
        // Given
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");

        CircularEventConsumer<CircuitBreakerOnErrorEvent> ringBuffer = new CircularEventConsumer<>(2);
        assertThat(ringBuffer.getBufferedEvents()).isEmpty();

        circuitBreaker.onError(Duration.ZERO, new RuntimeException("Bla"));
        circuitBreaker.onError(Duration.ZERO, new RuntimeException("Bla"));
        circuitBreaker.onError(Duration.ZERO, new RuntimeException("Bla"));

        //Subscription is too late
        circuitBreaker.getEventStream()
                .filter(event -> event.getEventType() == Type.ERROR)
                .cast(CircuitBreakerOnErrorEvent.class)
                .subscribe(ringBuffer);

        //Then
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(3);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(3);

        //Should store 0 events, because Subscription was too late
        assertThat(ringBuffer.getBufferedEvents()).hasSize(0);
    }
}
