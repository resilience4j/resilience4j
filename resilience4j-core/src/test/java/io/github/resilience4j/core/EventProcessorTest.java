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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

class EventProcessorTest extends ThreadModeTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(EventProcessorTest.class);

    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = mock(Logger.class);
    }

    @ParameterizedTest(name = "{0} thread mode")
    @EnumSource(ThreadType.class)
    void registerOnEventConsumer(ThreadType threadType) {
        setUpThreadMode(threadType);
        EventProcessor<Number> eventProcessor = new EventProcessor<>();
        EventConsumer<Number> eventConsumer = event -> logger.info(event.toString());

        eventProcessor.onEvent(eventConsumer);
        eventProcessor.onEvent(eventConsumer);

        assertThat(eventProcessor.onEventConsumers).hasSize(1);
        boolean consumed = eventProcessor.processEvent(1);
        then(logger).should(times(1)).info("1");
        assertThat(consumed).isTrue();
    }

    @ParameterizedTest(name = "{0} thread mode")
    @EnumSource(ThreadType.class)
    void registerConsumer(ThreadType threadType) {
        setUpThreadMode(threadType);
        EventProcessor<Number> eventProcessor = new EventProcessor<>();
        EventConsumer<Integer> eventConsumer = event -> logger.info(event.toString());

        eventProcessor.registerConsumer(Integer.class.getName(), eventConsumer);

        assertThat(eventProcessor.eventConsumerMap).hasSize(1);
        assertThat(eventProcessor.eventConsumerMap.get(Integer.class.getName())).hasSize(1);
        boolean consumed = eventProcessor.processEvent(1);
        then(logger).should(times(1)).info("1");
        assertThat(consumed).isTrue();
    }

    @ParameterizedTest(name = "{0} thread mode")
    @EnumSource(ThreadType.class)
    void registerSameConsumerOnlyOnce(ThreadType threadType) {
        setUpThreadMode(threadType);
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

    @ParameterizedTest(name = "{0} thread mode")
    @EnumSource(ThreadType.class)
    void registerTwoDifferentConsumers(ThreadType threadType) {
        setUpThreadMode(threadType);
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

    @ParameterizedTest(name = "{0} thread mode")
    @EnumSource(ThreadType.class)
    void registerDifferentConsumers(ThreadType threadType) {
        setUpThreadMode(threadType);
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

    @ParameterizedTest(name = "{0} thread mode")
    @EnumSource(ThreadType.class)
    void onEventAndRegisterConsumer(ThreadType threadType) {
        setUpThreadMode(threadType);
        EventProcessor<Number> eventProcessor = new EventProcessor<>();
        EventConsumer<Integer> eventConsumer = event -> logger.info(event.toString());

        eventProcessor.registerConsumer(Integer.class.getName(), eventConsumer);
        eventProcessor.onEvent(event -> logger.info(event.toString()));

        boolean consumed = eventProcessor.processEvent(1);
        then(logger).should(times(2)).info("1");
        assertThat(consumed).isTrue();
    }

    @ParameterizedTest(name = "{0} thread mode")
    @EnumSource(ThreadType.class)
    void noConsumers(ThreadType threadType) {
        setUpThreadMode(threadType);
        EventProcessor<Number> eventProcessor = new EventProcessor<>();

        boolean consumed = eventProcessor.processEvent(1);

        assertThat(consumed).isFalse();
    }

    @ParameterizedTest(name = "{0} thread mode")
    @EnumSource(ThreadType.class)
    void onEventParallel(ThreadType threadType) throws Exception {
        setUpThreadMode(threadType);
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

    @ParameterizedTest(name = "{0} thread mode")
    @EnumSource(ThreadType.class)
    void concurrentConsumerRegistrationInBothThreadModes(ThreadType threadType) throws Exception {
        setUpThreadMode(threadType);
        LOG.info("Running concurrentConsumerRegistrationInBothThreadModes in {}", getThreadModeDescription(threadType));

        EventProcessor<Integer> eventProcessor = new EventProcessor<>();
        int concurrentThreads = isVirtualThreadMode(threadType) ? 20 : 5; // More threads in virtual mode
        AtomicInteger totalConsumers = new AtomicInteger(0);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(concurrentThreads);

        // Create appropriate executor based on thread mode
        try (ExecutorService executor = isVirtualThreadMode(threadType) ?
            Executors.newVirtualThreadPerTaskExecutor() :
            Executors.newFixedThreadPool(concurrentThreads)) {

            // Launch concurrent threads to register consumers
            for (int i = 0; i < concurrentThreads; i++) {
                final int threadNum = i;

                executor.submit(() -> {
                    try {
                        // Wait for all threads to be ready
                        startLatch.await();

                        // Register a consumer
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

            // Start all threads
            startLatch.countDown();

            // Wait for all threads to complete registration
            boolean completed = completionLatch.await(5, TimeUnit.SECONDS);
            assertThat(completed)
                .as("All consumer registrations should complete within timeout in " + getThreadModeDescription(threadType))
                .isTrue();
        }

        // Process an event to verify all consumers were registered
        boolean consumed = eventProcessor.processEvent(42);

        // Verify processing results
        assertThat(consumed)
            .as("Event should be consumed in " + getThreadModeDescription(threadType))
            .isTrue();
        assertThat(totalConsumers.get())
            .as("All consumers should process the event in " + getThreadModeDescription(threadType))
            .isEqualTo(concurrentThreads);

        LOG.info("Concurrent consumer registration test passed in {} - Consumers: {}",
            getThreadModeDescription(threadType), concurrentThreads);
    }

    @ParameterizedTest(name = "{0} thread mode")
    @EnumSource(ThreadType.class)
    void concurrentEventProcessingInBothThreadModes(ThreadType threadType) throws Exception {
        setUpThreadMode(threadType);
        LOG.info("Running concurrentEventProcessingInBothThreadModes in {}", getThreadModeDescription(threadType));

        EventProcessor<Integer> eventProcessor = new EventProcessor<>();
        int concurrentThreads = isVirtualThreadMode(threadType) ? 15 : 3; // More threads in virtual mode
        AtomicInteger eventsProcessed = new AtomicInteger(0);
        AtomicInteger virtualThreadCount = new AtomicInteger(0);

        // Register a consumer that tracks thread type and processing
        eventProcessor.registerConsumer(Integer.class.getName(), event -> {
            // Record if running on a virtual thread
            if (Thread.currentThread().isVirtual()) {
                virtualThreadCount.incrementAndGet();
            }

            eventsProcessed.incrementAndGet();
            logger.info("Processed event: " + event);
        });

        CountDownLatch completionLatch = new CountDownLatch(concurrentThreads);

        // Create appropriate executor based on thread mode
        try (ExecutorService executor = isVirtualThreadMode(threadType) ?
            Executors.newVirtualThreadPerTaskExecutor() :
            Executors.newFixedThreadPool(concurrentThreads)) {

            // Launch concurrent threads to process events
            for (int i = 0; i < concurrentThreads; i++) {
                final int eventId = i;

                executor.submit(() -> {
                    try {
                        // Process an event
                        eventProcessor.processEvent(eventId);
                        return null;
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            // Wait for all threads to complete
            boolean completed = completionLatch.await(5, TimeUnit.SECONDS);
            assertThat(completed)
                .as("All event processing should complete within timeout in " + getThreadModeDescription(threadType))
                .isTrue();
        }

        // Verify results
        assertThat(eventsProcessed.get())
            .as("All events should be processed in " + getThreadModeDescription(threadType))
            .isEqualTo(concurrentThreads);

        // Thread type verification
        if (isVirtualThreadMode(threadType)) {
            assertThat(virtualThreadCount.get())
                .as("Events should be processed on virtual threads when configured in " + getThreadModeDescription(threadType))
                .isEqualTo(concurrentThreads);
        } else {
            assertThat(virtualThreadCount.get())
                .as("Events should be processed on platform threads by default in " + getThreadModeDescription(threadType))
                .isZero();
        }

        LOG.info("Concurrent event processing test passed in {} - Events: {}, Virtual threads: {}",
            getThreadModeDescription(threadType), concurrentThreads, virtualThreadCount.get());
    }
}
