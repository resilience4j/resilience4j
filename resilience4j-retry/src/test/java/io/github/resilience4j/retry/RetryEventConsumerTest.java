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
package io.github.resilience4j.retry;

import io.github.resilience4j.test.HelloWorldService;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import javax.xml.ws.WebServiceException;

import static io.vavr.API.*;
import static io.vavr.Predicates.instanceOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class RetryEventConsumerTest {

    private HelloWorldService helloWorldService;
    private Logger logger;
    private Retry retry;

    @Before
    public void setUp(){
        helloWorldService = mock(HelloWorldService.class);
        logger = mock(Logger.class);
        retry = Retry.ofDefaults("testName");
    }

    @Test
    public void shouldReturnTheSameConsumer() {
        Retry.EventConsumer eventConsumer = retry.getEventConsumer();
        Retry.EventConsumer eventConsumer2 = retry.getEventConsumer();

        assertThat(eventConsumer).isEqualTo(eventConsumer2);
    }

    @Test
    public void shouldConsumeOnSuccessEvent() {
        // Given the HelloWorldService returns Hello world
        given(helloWorldService.returnHelloWorld())
                .willThrow(new WebServiceException("BAM!"))
                .willReturn("Hello world");

        retry.getEventConsumer()
            .onSuccess(event ->
                    logger.info(event.getEventType().toString()));

        retry.executeSupplier(helloWorldService::returnHelloWorld);

        then(helloWorldService).should(times(2)).returnHelloWorld();
        then(logger).should(times(1)).info("SUCCESS");
    }

    @Test
    public void shouldConsumeOnErrorEvent() {
        given(helloWorldService.returnHelloWorld())
                .willThrow(new WebServiceException("BAM!"));

        retry.getEventConsumer()
            .onError(event ->
                    logger.info(event.getEventType().toString()));


        Try.ofSupplier(Retry.decorateSupplier(retry, helloWorldService::returnHelloWorld));

        then(logger).should(times(1)).info("ERROR");
        then(helloWorldService).should(times(3)).returnHelloWorld();
    }

    @Test
    public void shouldConsumeIgnoredErrorEvent() {
        given(helloWorldService.returnHelloWorld())
                .willThrow(new WebServiceException("BAM!"));

        RetryConfig retryConfig = RetryConfig.custom()
                .retryOnException(throwable -> Match(throwable).of(
                        Case($(instanceOf(WebServiceException.class)), false),
                        Case($(), true)))
                .build();
        retry = Retry.of("testName", retryConfig);

        retry.getEventConsumer()
            .onIgnoredError(event ->
                    logger.info(event.getEventType().toString()));

        Try.ofSupplier(Retry.decorateSupplier(retry, helloWorldService::returnHelloWorld));

        then(logger).should(times(1)).info("IGNORED_ERROR");
        then(helloWorldService).should(times(1)).returnHelloWorld();
    }

}
