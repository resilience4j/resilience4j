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
import org.slf4j.Logger;

import javax.xml.ws.WebServiceException;
import java.io.IOException;

import static io.vavr.API.*;
import static io.vavr.Predicates.instanceOf;
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
                .onEvent(event ->
                        logger.info(event.getEventType().toString()));


        circuitBreaker.onSuccess(1000);


        then(logger).should(times(1)).info("SUCCESS");
    }

    @Test
    public void shouldConsumeOnSuccessEvent() {
        circuitBreaker.getEventPublisher()
                .onSuccess(event ->
                        logger.info(event.getEventType().toString()));


        circuitBreaker.onSuccess(1000);


        then(logger).should(times(1)).info("SUCCESS");
    }

    @Test
    public void shouldConsumeOnErrorEvent() {
        circuitBreaker.getEventPublisher()
                .onError(event ->
                        logger.info(event.getEventType().toString()));


        circuitBreaker.onError(1000, new IOException("BAM!"));

        then(logger).should(times(1)).info("ERROR");
    }

    @Test
    public void shouldConsumeOnStateTransitionEvent() {
        circuitBreaker = CircuitBreaker.of("test", CircuitBreakerConfig.custom()
                .ringBufferSizeInClosedState(1).build());

        circuitBreaker.getEventPublisher()
                .onStateTransition(event ->
                        logger.info(event.getEventType().toString()));


        circuitBreaker.onError(1000, new IOException("BAM!"));
        circuitBreaker.onError(1000, new IOException("BAM!"));


        then(logger).should(times(1)).info("STATE_TRANSITION");
    }

    @Test
    public void shouldConsumeCallNotPermittedEvent() {
        circuitBreaker = CircuitBreaker.of("test", CircuitBreakerConfig.custom()
                .ringBufferSizeInClosedState(1).build());

        circuitBreaker.getEventPublisher()
                .onCallNotPermitted(event ->
                        logger.info(event.getEventType().toString()));


        circuitBreaker.onError(1000, new IOException("BAM!"));
        circuitBreaker.onError(1000, new IOException("BAM!"));
        circuitBreaker.isCallPermitted();


        then(logger).should(times(1)).info("NOT_PERMITTED");
    }

    @Test
    public void shouldConsumeIgnoredErrorEvent() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .recordFailure(throwable -> Match(throwable).of(
                        Case($(instanceOf(WebServiceException.class)), true),
                        Case($(), false)))
                .build();

        circuitBreaker = CircuitBreaker.of("test", circuitBreakerConfig);

        circuitBreaker.getEventPublisher()
                .onIgnoredError(event ->
                        logger.info(event.getEventType().toString()));


        circuitBreaker.onError(1000, new IOException("BAM!"));


        then(logger).should(times(1)).info("IGNORED_ERROR");
    }

}
