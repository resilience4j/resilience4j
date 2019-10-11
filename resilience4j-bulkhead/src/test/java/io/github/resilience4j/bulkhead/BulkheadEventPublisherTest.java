/*
 *
 *  Copyright 2017 Robert Winkler, Lucas Lech
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
package io.github.resilience4j.bulkhead;

import io.github.resilience4j.test.HelloWorldService;
import io.vavr.control.Try;

import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class BulkheadEventPublisherTest {

    private HelloWorldService helloWorldService;
    private BulkheadConfig config;
    private Logger logger;
    private Bulkhead bulkhead;

    @Before
    public void setUp(){
        helloWorldService = mock(HelloWorldService.class);
        config = BulkheadConfig.custom()
                   .maxConcurrentCalls(1)
                   .build();

        bulkhead= Bulkhead.of("test", config);

        logger = mock(Logger.class);
    }

    @Test
    public void shouldReturnTheSameConsumer() {
        Bulkhead.EventPublisher eventPublisher = bulkhead.getEventPublisher();
        Bulkhead.EventPublisher eventPublisher2 = bulkhead.getEventPublisher();

        assertThat(eventPublisher).isEqualTo(eventPublisher2);
    }

    @Test
    public void shouldConsumeOnCallPermittedEvent() {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        bulkhead.getEventPublisher().onCallPermitted(
                event -> logger.info(event.getEventType().toString()));

        String result = bulkhead.executeSupplier(helloWorldService::returnHelloWorld);

        assertThat(result).isEqualTo("Hello world");
        then(logger).should(times(1)).info("CALL_PERMITTED");
    }


    @Test
    public void shouldConsumeOnCallRejectedEvent() {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        bulkhead.getEventPublisher().onCallRejected(
                event -> logger.info(event.getEventType().toString()));
        bulkhead.tryAcquirePermission();
        Supplier<String> supplier = Bulkhead
                .decorateSupplier(bulkhead, helloWorldService::returnHelloWorld);
        
        Try.ofSupplier(supplier);

        then(logger).should(times(1)).info("CALL_REJECTED");
    }

    @Test
    public void shouldConsumeOnCallFinishedEventWhenExecutionIsFinished() throws Exception {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        bulkhead.getEventPublisher().onCallFinished(
                event -> logger.info(event.getEventType().toString()));
        Supplier<String> supplier = Bulkhead
                .decorateSupplier(bulkhead, helloWorldService::returnHelloWorld);
        
        Try.ofSupplier(supplier);

        then(logger).should(times(1)).info("CALL_FINISHED");
    }

    @Test
    public void shouldConsumeOnCallFinishedEventOnComplete() throws Exception {
        Bulkhead bulkhead = Bulkhead.of("test", config);
        bulkhead.getEventPublisher().onCallFinished(
                event -> logger.info(event.getEventType().toString()));

        bulkhead.onComplete();

        then(logger).should(times(1)).info("CALL_FINISHED");
    }
}
