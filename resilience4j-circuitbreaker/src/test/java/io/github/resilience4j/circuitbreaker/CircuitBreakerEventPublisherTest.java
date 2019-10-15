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

import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class CircuitBreakerEventPublisherTest {

    private Logger logger;
    private CircuitBreaker circuitBreaker;

    @Before
    public void setUp(){
        logger = mock(Logger.class);
        circuitBreaker = CircuitBreaker.ofDefaults("testName");
    }

    @Test
    public void shouldReturnTheSameConsumer() {
        CircuitBreaker.EventPublisher eventPublisher = circuitBreaker.getEventPublisher();
        CircuitBreaker.EventPublisher eventPublisher2 = circuitBreaker.getEventPublisher();

        assertThat(eventPublisher).isEqualTo(eventPublisher2);
    }

    @Test
    public void shouldConsumeOnEvent() {
        circuitBreaker.getEventPublisher()
                .onEvent(this::logEventType);

        circuitBreaker.onSuccess(1000, TimeUnit.NANOSECONDS);

        then(logger).should(times(1)).info("SUCCESS");
    }

    @Test
    public void shouldConsumeOnSuccessEvent() {
        circuitBreaker.getEventPublisher()
                .onSuccess(this::logEventType);

        circuitBreaker.onSuccess(1000, TimeUnit.NANOSECONDS);

        then(logger).should(times(1)).info("SUCCESS");
    }

    @Test
    public void shouldConsumeOnErrorEvent() {
        circuitBreaker.getEventPublisher()
                .onError(this::logEventType);

        circuitBreaker.onError(1000, TimeUnit.NANOSECONDS, new IOException("BAM!"));

        then(logger).should(times(1)).info("ERROR");
    }

    @Test
    public void shouldConsumeOnResetEvent() {
        circuitBreaker.getEventPublisher()
                .onReset(this::logEventType);

        circuitBreaker.reset();

        then(logger).should(times(1)).info("RESET");
    }

    @Test
    public void shouldConsumeOnStateTransitionEvent() {
        circuitBreaker = CircuitBreaker.of("test", CircuitBreakerConfig.custom()
                .slidingWindowSize(1).build());
        circuitBreaker.getEventPublisher()
                .onStateTransition(this::logEventType);

        circuitBreaker.onError(1000, TimeUnit.NANOSECONDS, new IOException("BAM!"));
        circuitBreaker.onError(1000, TimeUnit.NANOSECONDS, new IOException("BAM!"));

        then(logger).should(times(1)).info("STATE_TRANSITION");
    }

    @Test
    public void shouldConsumeCallNotPermittedEvent() {
        circuitBreaker = CircuitBreaker.of("test", CircuitBreakerConfig.custom()
                .slidingWindowSize(1).build());
        circuitBreaker.getEventPublisher()
                .onCallNotPermitted(this::logEventType);

        circuitBreaker.onError(1000, TimeUnit.NANOSECONDS, new IOException("BAM!"));
        circuitBreaker.onError(1000, TimeUnit.NANOSECONDS, new IOException("BAM!"));
        circuitBreaker.tryAcquirePermission();

        then(logger).should(times(1)).info("NOT_PERMITTED");
    }

    @Test
    public void shouldNotProduceEventsInDisabledState() {
        circuitBreaker = CircuitBreaker.of("test", CircuitBreakerConfig.custom()
                .slidingWindowSize(1).build());
        circuitBreaker.getEventPublisher()
                .onEvent(this::logEventType);

        //When we transition to disabled
        circuitBreaker.transitionToDisabledState();
        //And we execute other calls that should generate events
        circuitBreaker.onError(1000, TimeUnit.NANOSECONDS, new IOException("BAM!"));
        circuitBreaker.onError(1000, TimeUnit.NANOSECONDS, new IOException("BAM!"));
        circuitBreaker.tryAcquirePermission();
        circuitBreaker.onSuccess(0, TimeUnit.NANOSECONDS);
        circuitBreaker.onError(1000, TimeUnit.NANOSECONDS, new IOException("BAM!"));

        //Then we do not produce events
        then(logger).should(times(1)).info("STATE_TRANSITION");
        then(logger).should(times(0)).info("NOT_PERMITTED");
        then(logger).should(times(0)).info("SUCCESS");
        then(logger).should(times(0)).info("ERROR");
        then(logger).should(times(0)).info("IGNORED_ERROR");
    }

    private void logEventType(CircuitBreakerEvent event) {
        logger.info(event.getEventType().toString());
    }

    @Test
    public void shouldConsumeIgnoredErrorEvent() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .ignoreExceptions(IOException.class)
                .build();
        circuitBreaker = CircuitBreaker.of("test", circuitBreakerConfig);
        circuitBreaker.getEventPublisher()
                .onIgnoredError(this::logEventType);

        circuitBreaker.onError(10000, TimeUnit.NANOSECONDS, new IOException("BAM!"));

        then(logger).should(times(1)).info("IGNORED_ERROR");
    }

}
