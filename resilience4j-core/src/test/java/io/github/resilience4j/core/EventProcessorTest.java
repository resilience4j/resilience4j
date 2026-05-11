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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

@ExtendWith(ThreadModeExtension.class)
class EventProcessorTest {

    private static final Logger LOG = LoggerFactory.getLogger(EventProcessorTest.class);

    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = mock(Logger.class);
    }

    @TestTemplate
    void registerOnEventConsumer(ThreadType threadType) {
        EventProcessor<Number> eventProcessor = new EventProcessor<>();
        EventConsumer<Number> eventConsumer = event -> logger.info(event.toString());

        eventProcessor.onEvent(eventConsumer);
        eventProcessor.onEvent(eventConsumer);

        assertThat(eventProcessor.onEventConsumers).hasSize(1);
        boolean consumed = eventProcessor.processEvent(1);
        then(logger).should(times(1)).info("1");
        assertThat(consumed).isTrue();
    }

    @TestTemplate
    void registerConsumer(ThreadType threadType) {
        EventProcessor<Number> eventProcessor = new EventProcessor<>();
        EventConsumer<Integer> eventConsumer = event -> logger.info(event.toString());

        eventProcessor.registerConsumer(Integer.class.getName(), eventConsumer);

        assertThat(eventProcessor.eventConsumerMap).hasSize(1);
        assertThat(eventProcessor.eventConsumerMap.get(Integer.class.getName())).hasSize(1);
        boolean consumed = eventProcessor.processEvent(1);
        then(logger).should(times(1)).info("1");
        assertThat(consumed).isTrue();
    }

    @TestTemplate
    void registerSameConsumerOnlyOnce(ThreadType threadType) {
        EventProcessor<Number> eventProcessor = new EventProcessor<>();
        EventConsumer<Integer> eventConsumer = event -> logger.info(event.toString());

        eventProcessor.registerConsumer(Integer.class.getName(), eventConsumer);
        eventProcessor.registerConsumer(Integer.class.getName(), eventConsumer);

        assertThat(eventProcessor.eventConsumerMap).hasSize(1);
        assertThat(eventProcessor.eventConsumerMap.get(Integer.class.getName())).hasSize(1);
        boolean consumed = eventProcessor.processEvent(1);
        then(logger).should(times(1)).info("1");
        assertThat(consumed).isTrue();
    }

    @TestTemplate
    void registerTwoDifferentConsumers(ThreadType threadType) {
        EventProcessor<Number> eventProcessor = new EventProcessor<>();

        EventConsumer<Integer> eventConsumer1 = event -> logger.info(event.toString());
        EventConsumer<Integer> eventConsumer2 = event -> logger.info(event.toString());

        eventProcessor.registerConsumer(Integer.class.getName(), eventConsumer1);
        eventProcessor.registerConsumer(Integer.class.getName(), eventConsumer2);

        assertThat(eventProcessor.eventConsumerMap).hasSize(1);
        assertThat(eventProcessor.eventConsumerMap.get(Integer.class.getName())).hasSize(2);
        boolean consumed = eventProcessor.processEvent(1);
        assertThat(consumed).isTrue();
        then(logger).should(times(2)).info("1");
    }

    @TestTemplate
    void registerDifferentConsumers(ThreadType threadType) {
        EventProcessor<Number> eventProcessor = new EventProcessor<>();
        EventConsumer<Integer> integerConsumer = event -> logger.info(event.toString());
        EventConsumer<Float> floatConsumer = event -> logger.info(event.toString());

        eventProcessor.registerConsumer(Integer.class.getName(), integerConsumer);
        eventProcessor.registerConsumer(Float.class.getName(), floatConsumer);

        assertThat(eventProcessor.eventConsumerMap).hasSize(2);
        assertThat(eventProcessor.eventConsumerMap.get(Integer.class.getName())).hasSize(1);
        assertThat(eventProcessor.eventConsumerMap.get(Float.class.getName())).hasSize(1);
        boolean consumed = eventProcessor.processEvent(1);
        assertThat(consumed).isTrue();
        consumed = eventProcessor.processEvent(1.0f);
        assertThat(consumed).isTrue();
        then(logger).should(times(1)).info("1");
        then(logger).should(times(1)).info("1.0");
    }

    @TestTemplate
    void onEventAndRegisterConsumer(ThreadType threadType) {
        EventProcessor<Number> eventProcessor = new EventProcessor<>();
        EventConsumer<Integer> eventConsumer = event -> logger.info(event.toString());

        eventProcessor.registerConsumer(Integer.class.getName(), eventConsumer);
        eventProcessor.onEvent(event -> logger.info(event.toString()));

        boolean consumed = eventProcessor.processEvent(1);
        then(logger).should(times(2)).info("1");
        assertThat(consumed).isTrue();
    }

    @TestTemplate
    void noConsumers(ThreadType threadType) {
        EventProcessor<Number> eventProcessor = new EventProcessor<>();

        boolean consumed = eventProcessor.processEvent(1);

        assertThat(consumed).isFalse();
    }

    @TestTemplate
    void onEventParallel(ThreadType threadType) throws Exception {
        CountDownLatch eventConsumed = new CountDownLatch(1);
        CountDownLatch waitForConsumerRegistration = new CountDownLatch(1);

        EventProcessor<Number> eventProcessor = new EventProcessor<>();
        EventConsumer<Integer> eventConsumer1 = event -> {
            try {
                eventConsumed.countDown();
                waitForConsumerRegistration.await(5, TimeUnit.SECONDS);
                logger.info(event.toString());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Must not happen", e);
            }
        };

        EventConsumer<Integer> eventConsumer2 = event -> logger.info(event.toString());

        // 1st consumer is added
        eventProcessor.registerConsumer(Integer.class.getName(), eventConsumer1);

        // process first event in a separate thread to create a race condition
        CompletableFuture<Void> future = CompletableFuture.runAsync(() ->
            eventProcessor.processEvent(1));

        eventConsumed.await(1, TimeUnit.SECONDS);

        // 2nd consumer is added
        eventProcessor.registerConsumer(Integer.class.getName(), eventConsumer2);

        future.get();

        waitForConsumerRegistration.countDown();

        then(logger).should(times(1)).info("1");
    }

    @Test
    void onEventRejectsNullConsumer() {
        EventProcessor<Number> eventProcessor = new EventProcessor<>();
        assertThatThrownBy(() -> eventProcessor.onEvent(null))
            .isInstanceOf(NullPointerException.class);
    }

    @TestTemplate
    void concurrentConsumerRegistrationInBothThreadModes(ThreadType threadType) throws Exception {
        LOG.info("Running concurrentConsumerRegistrationInBothThreadModes in {}", threadType);

        EventProcessor<Integer> eventProcessor = new EventProcessor<>();
        int concurrentThreads = threadType == ThreadType.VIRTUAL ? 20 : 5;
        AtomicInteger totalConsumers = new AtomicInteger(0);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(concurrentThreads);

        try (ExecutorService executor = threadType == ThreadType.VIRTUAL ?
            Executors.newVirtualThreadPerTaskExecutor() :
            Executors.newFixedThreadPool(concurrentThreads)) {

            for (int i = 0; i < concurrentThreads; i++) {
                final int threadNum = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        eventProcessor.registerConsumer(Integer.class.getName(),
                            event -> {
                                totalConsumers.incrementAndGet();
                                logger.info("Consumer " + threadNum + " processed: " + event);
                            });
                        return null;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            startLatch.countDown();

            assertThat(completionLatch.await(5, TimeUnit.SECONDS))
                .as("All consumer registrations should complete within timeout in %s", threadType)
                .isTrue();
        }

        boolean consumed = eventProcessor.processEvent(42);

        assertThat(consumed).as("Event should be consumed in %s", threadType).isTrue();
        assertThat(totalConsumers.get())
            .as("All consumers should process the event in %s", threadType)
            .isEqualTo(concurrentThreads);

        LOG.info("Concurrent consumer registration test passed in {} - Consumers: {}", threadType, concurrentThreads);
    }

    @TestTemplate
    void concurrentEventProcessingInBothThreadModes(ThreadType threadType) throws Exception {
        LOG.info("Running concurrentEventProcessingInBothThreadModes in {}", threadType);

        EventProcessor<Integer> eventProcessor = new EventProcessor<>();
        int concurrentThreads = threadType == ThreadType.VIRTUAL ? 15 : 3;
        AtomicInteger eventsProcessed = new AtomicInteger(0);
        AtomicInteger virtualThreadCount = new AtomicInteger(0);

        eventProcessor.registerConsumer(Integer.class.getName(), event -> {
            if (Thread.currentThread().isVirtual()) {
                virtualThreadCount.incrementAndGet();
            }
            eventsProcessed.incrementAndGet();
            logger.info("Processed event: " + event);
        });

        CountDownLatch completionLatch = new CountDownLatch(concurrentThreads);

        try (ExecutorService executor = threadType == ThreadType.VIRTUAL ?
            Executors.newVirtualThreadPerTaskExecutor() :
            Executors.newFixedThreadPool(concurrentThreads)) {

            for (int i = 0; i < concurrentThreads; i++) {
                final int eventId = i;
                executor.submit(() -> {
                    try {
                        eventProcessor.processEvent(eventId);
                        return null;
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            assertThat(completionLatch.await(5, TimeUnit.SECONDS))
                .as("All event processing should complete within timeout in %s", threadType)
                .isTrue();
        }

        assertThat(eventsProcessed.get())
            .as("All events should be processed in %s", threadType)
            .isEqualTo(concurrentThreads);

        if (threadType == ThreadType.VIRTUAL) {
            assertThat(virtualThreadCount.get())
                .as("Events should be processed on virtual threads in %s", threadType)
                .isEqualTo(concurrentThreads);
        } else {
            assertThat(virtualThreadCount.get())
                .as("Events should be processed on platform threads in %s", threadType)
                .isZero();
        }

        LOG.info("Concurrent event processing test passed in {} - Events: {}, Virtual threads: {}",
            threadType, concurrentThreads, virtualThreadCount.get());
    }
}
