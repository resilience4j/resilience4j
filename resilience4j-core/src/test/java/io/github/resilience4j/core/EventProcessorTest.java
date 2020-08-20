/*
 *
 *  Copyright 2017: Robert Winkler
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
package io.github.resilience4j.core;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class EventProcessorTest {

    private Logger logger;

    @Before
    public void setUp() {
        logger = mock(Logger.class);
    }

    @Test
    public void testRegisterOnEventConsumer() {
        EventProcessor<Number> eventProcessor = new EventProcessor<>();
        EventConsumer<Number> eventConsumer = event -> logger.info(event.toString());

        eventProcessor.onEvent(eventConsumer);
        eventProcessor.onEvent(eventConsumer);

        assertThat(eventProcessor.onEventConsumers).hasSize(2);
        boolean consumed = eventProcessor.processEvent(1);
        then(logger).should(times(2)).info("1");
        assertThat(consumed).isEqualTo(true);
    }

    @Test
    public void testRegisterConsumer() {
        EventProcessor<Number> eventProcessor = new EventProcessor<>();
        EventConsumer<Integer> eventConsumer = event -> logger.info(event.toString());

        eventProcessor.registerConsumer(Integer.class.getSimpleName(), eventConsumer);
        eventProcessor.registerConsumer(Integer.class.getSimpleName(), eventConsumer);

        assertThat(eventProcessor.eventConsumerMap).hasSize(1);
        assertThat(eventProcessor.eventConsumerMap.get(Integer.class.getSimpleName())).hasSize(2);
        boolean consumed = eventProcessor.processEvent(1);
        then(logger).should(times(2)).info("1");
        assertThat(consumed).isEqualTo(true);
    }

    @Test
    public void testRegisterDifferentConsumers() {
        EventProcessor<Number> eventProcessor = new EventProcessor<>();
        EventConsumer<Integer> integerConsumer = event -> logger.info(event.toString());
        EventConsumer<Float> floatConsumer = event -> logger.info(event.toString());

        eventProcessor.registerConsumer(Integer.class.getSimpleName(), integerConsumer);
        eventProcessor.registerConsumer(Float.class.getSimpleName(), floatConsumer);

        assertThat(eventProcessor.eventConsumerMap).hasSize(2);
        assertThat(eventProcessor.eventConsumerMap.get(Integer.class.getSimpleName())).hasSize(1);
        assertThat(eventProcessor.eventConsumerMap.get(Float.class.getSimpleName())).hasSize(1);
        boolean consumed = eventProcessor.processEvent(1);
        assertThat(consumed).isEqualTo(true);
        consumed = eventProcessor.processEvent(1.0f);
        assertThat(consumed).isEqualTo(true);
        then(logger).should(times(1)).info("1");
        then(logger).should(times(1)).info("1.0");
    }

    @Test
    public void testOnEventAndRegisterConsumer() {
        EventProcessor<Number> eventProcessor = new EventProcessor<>();
        EventConsumer<Integer> eventConsumer = event -> logger.info(event.toString());

        eventProcessor.registerConsumer(Integer.class.getSimpleName(), eventConsumer);
        eventProcessor.onEvent(event -> logger.info(event.toString()));

        boolean consumed = eventProcessor.processEvent(1);
        then(logger).should(times(2)).info("1");
        assertThat(consumed).isEqualTo(true);
    }

    @Test
    public void testNoConsumers() {
        EventProcessor<Number> eventProcessor = new EventProcessor<>();

        boolean consumed = eventProcessor.processEvent(1);

        assertThat(consumed).isEqualTo(false);
    }


    @Test
    public void testOnEventParallel() throws ExecutionException, InterruptedException {
        CountDownLatch eventConsumed = new CountDownLatch(1);
        CountDownLatch waitForConsumerRegistration = new CountDownLatch(1);

        EventProcessor<Number> eventProcessor = new EventProcessor<>();
        EventConsumer<Integer> eventConsumer1 = event -> {
            try {
                eventConsumed.countDown();
                waitForConsumerRegistration.await(5, TimeUnit.SECONDS);
                logger.info(event.toString());
            } catch (InterruptedException e) {
                fail("Must not happen");
            }
        };

        EventConsumer<Integer> eventConsumer2 = event -> logger.info(event.toString());

        // 1st consumer is added
        eventProcessor.registerConsumer(Integer.class.getSimpleName(), eventConsumer1);

        // process first event in a separate thread to create a race condition
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            eventProcessor.processEvent(1); // blocks because of the count down latch
        });

        eventConsumed.await(1, TimeUnit.SECONDS);

        // 2nd consumer is added
        eventProcessor.registerConsumer(Integer.class.getSimpleName(), eventConsumer2);

        future.get();

        waitForConsumerRegistration.countDown();

        then(logger).should(times(1)).info("1");
    }

}
