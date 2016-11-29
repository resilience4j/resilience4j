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
package io.github.robwin.circuitbreaker.consumer;

import io.github.robwin.circuitbreaker.CircuitBreaker;
import io.github.robwin.circuitbreaker.consumer.CircuitBreakerEventConsumer;
import javaslang.control.Try;
import io.github.robwin.retry.Retry;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import io.github.robwin.circuitbreaker.event.CircuitBreakerEvent;

import static org.assertj.core.api.Assertions.assertThat;

public class CircuitBreakerEventConsumerTest {

    @Test
    public void shouldBufferEvents() {
        // Given

        // tag::shouldBufferEvents[]
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        CircuitBreakerEventConsumer ringBuffer = new CircuitBreakerEventConsumer(2);
        circuitBreaker.getEventStream()
                .filter(event -> event.getEventType() == CircuitBreakerEvent.Type.ERROR)
                .subscribe(ringBuffer);
        // end::shouldBufferEvents[]

        Assertions.assertThat(ringBuffer.getBufferedCircuitBreakerEvents()).isEmpty();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        Try.CheckedRunnable runnable = () -> { throw new RuntimeException("BAM!");};

        // Create a Retry with 3 retries
        Retry retryContext = Retry.ofDefaults();

        //When
        runnable = CircuitBreaker.decorateCheckedRunnable(circuitBreaker, runnable);
        runnable = Retry.decorateCheckedRunnable(retryContext, runnable);
        Try<Void> result = Try.run(runnable);

        //Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);

        //Should only store 2 events, because capacity is 2
        Assertions.assertThat(ringBuffer.getBufferedCircuitBreakerEvents()).hasSize(2);
    }

    @Test
    public void shouldNotBufferEvents() {
        // Given
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        CircuitBreakerEventConsumer ringBuffer = new CircuitBreakerEventConsumer(2);
        Assertions.assertThat(ringBuffer.getBufferedCircuitBreakerEvents()).isEmpty();

        Try.CheckedRunnable runnable = () -> { throw new RuntimeException("BAM!");};

        // Create a Retry with 3 retries
        Retry retryContext = Retry.ofDefaults();

        //When
        runnable = CircuitBreaker.decorateCheckedRunnable(circuitBreaker, runnable);
        runnable = Retry.decorateCheckedRunnable(retryContext, runnable);
        Try<Void> result = Try.run(runnable);

        //Subscription is too late
        circuitBreaker.getEventStream()
                .filter(event -> event.getEventType() == CircuitBreakerEvent.Type.ERROR)
                .subscribe(ringBuffer);

        //Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);

        //Should store 0 events, because Subscription was too late
        Assertions.assertThat(ringBuffer.getBufferedCircuitBreakerEvents()).hasSize(0);
    }
}
