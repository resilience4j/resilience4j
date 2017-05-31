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
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.slf4j.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class BulkheadEventConsumerTest {

    private HelloWorldService helloWorldService;
    private BulkheadConfig config;
    private Logger logger;
    private Bulkhead bulkhead;

    @Before
    public void setUp(){
        helloWorldService = Mockito.mock(HelloWorldService.class);
        config = BulkheadConfig.custom()
                   .maxConcurrentCalls(1)
                   .build();

        bulkhead= Bulkhead.of("test", config);

        logger = mock(Logger.class);
    }

    @Test
    public void shouldReturnTheSameConsumer() {
        Bulkhead.EventConsumer eventConsumer = bulkhead.getEventConsumer();
        Bulkhead.EventConsumer eventConsumer2 = bulkhead.getEventConsumer();

        assertThat(eventConsumer).isEqualTo(eventConsumer2);
    }

    @Test
    public void shouldConsumeOnCallPermittedEvent() {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);
        BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        // When
        bulkhead.getEventConsumer()
            .onCallPermitted(event ->
                    logger.info(event.getEventType().toString()));

        String result = bulkhead.executeSupplier(helloWorldService::returnHelloWorld);

        // Then
        assertThat(result).isEqualTo("Hello world");
        then(logger).should(times(1)).info("CALL_PERMITTED");
    }


    @Test
    public void shouldConsumeOnCallRejectedEvent() {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", config);

        // When
        bulkhead.getEventConsumer()
                .onCallRejected(event ->
                        logger.info(event.getEventType().toString()));

        bulkhead.isCallPermitted();

        Try.ofSupplier(Bulkhead.decorateSupplier(bulkhead,helloWorldService::returnHelloWorld));

        // Then
        then(logger).should(times(1)).info("CALL_REJECTED");
    }


}
