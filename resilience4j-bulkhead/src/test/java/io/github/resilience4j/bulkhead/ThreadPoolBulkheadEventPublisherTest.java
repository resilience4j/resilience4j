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

import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.slf4j.Logger;

import java.util.concurrent.ExecutionException;

import io.github.resilience4j.test.HelloWorldService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class ThreadPoolBulkheadEventPublisherTest {

    private HelloWorldService helloWorldService;
    private ThreadPoolBulkheadConfig config;
    private Logger logger;
    private ThreadPoolBulkhead bulkhead;

    @Before
    public void setUp(){
        helloWorldService = mock(HelloWorldService.class);
        config = ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(1)
            .coreThreadPoolSize(1)
            .build();

        bulkhead = ThreadPoolBulkhead.of("test", config);

        logger = mock(Logger.class);
    }

    @Test
    public void shouldReturnTheSameConsumer() {
        ThreadPoolBulkhead.EventPublisher eventPublisher = bulkhead.getEventPublisher();
        ThreadPoolBulkhead.EventPublisher eventPublisher2 = bulkhead.getEventPublisher();

        assertThat(eventPublisher).isEqualTo(eventPublisher2);
    }

    @Test
    public void shouldConsumeOnCallPermittedEvent() throws ExecutionException, InterruptedException {
        // Given
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("test", config);
        BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        // When
        bulkhead.getEventPublisher()
            .onCallPermitted(event ->
                    logger.info(event.getEventType().toString()));

        String result = bulkhead.executeSupplier(helloWorldService::returnHelloWorld).get();

        // Then
        assertThat(result).isEqualTo("Hello world");
        then(logger).should(times(1)).info("CALL_PERMITTED");
    }

    @Test
    public void shouldConsumeOnCallFinishedEventWhenExecutionIsFinished() throws Exception {
        // Given
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("test", config);

        // When
        bulkhead.getEventPublisher()
                .onCallFinished(event ->
                        logger.info(event.getEventType().toString()));

        bulkhead.onComplete();

        // Then
        then(logger).should(times(1)).info("CALL_FINISHED");
    }
}
