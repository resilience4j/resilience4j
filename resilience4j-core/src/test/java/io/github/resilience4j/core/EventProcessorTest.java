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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

@RunWith(Parameterized.class)
public class EventProcessorTest extends ThreadModeTestBase {

    @Parameterized.Parameters(name = "threadMode={0}")
    public static Collection<Object[]> threadModes() {
        return ThreadModeTestBase.threadModes();
    }

    /**
     * Constructor for parameterized tests.
     * 
     * @param threadType the thread mode to test with ("platform" or "virtual")
     */
    public EventProcessorTest(ThreadType threadType) {
        super(threadType);
    }

    private Logger logger;

    @Before
    public void setUp() {
        setUpThreadMode(); // Set up thread mode from ThreadModeTestBase
        logger = mock(Logger.class);
    }

    @After
    public void tearDown() {
        cleanUpThreadMode(); // Clean up thread mode from ThreadModeTestBase
    }

    @Test
    public void testRegisterOnEventConsumer() {
        EventProcessor<Number> eventProcessor = new EventProcessor<>();
        EventConsumer<Number> eventConsumer = event -> logger.info(event.toString());

        eventProcessor.onEvent(eventConsumer);
        eventProcessor.onEvent(eventConsumer);

        assertThat(eventProcessor.onEventConsumers).hasSize(1);
        boolean consumed = eventProcessor.processEvent(1);
        then(logger).should(times(1)).info("1");
        assertThat(consumed).isTrue();
    }

    @Test
    public void testRegisterConsumer() {
        EventProcessor<Number> eventProcessor = new EventProcessor<>();
        EventConsumer<Integer> eventConsumer = event -> logger.info(event.toString());

        eventProcessor.registerConsumer(Integer.class.getName(), eventConsumer);

        assertThat(eventProcessor.eventConsumerMap).hasSize(1);
        assertThat(eventProcessor.eventConsumerMap.get(Integer.class.getName())).hasSize(1);
        boolean consumed = eventProcessor.processEvent(1);
        then(logger).should(times(1)).info("1");
        assertThat(consumed).isTrue();
    }

    @Test
    public void testRegisterSameConsumerOnlyOnce() {
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

    @Test
    public void testRegisterTwoDifferentConsumers() {
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

    @Test
    public void testRegisterDifferentConsumers() {
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

    @Test
    public void testOnEventAndRegisterConsumer() {
        EventProcessor<Number> eventProcessor = new EventProcessor<>();
        EventConsumer<Integer> eventConsumer = event -> logger.info(event.toString());

        eventProcessor.registerConsumer(Integer.class.getName(), eventConsumer);
        eventProcessor.onEvent(event -> logger.info(event.toString()));

        boolean consumed = eventProcessor.processEvent(1);
        then(logger).should(times(2)).info("1");
        assertThat(consumed).isTrue();
    }

    @Test
    public void testNoConsumers() {
        EventProcessor<Number> eventProcessor = new EventProcessor<>();

        boolean consumed = eventProcessor.processEvent(1);

        assertThat(consumed).isFalse();
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
        eventProcessor.registerConsumer(Integer.class.getName(), eventConsumer1);

        // process first event in a separate thread to create a race condition
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            eventProcessor.processEvent(1); // blocks because of the count down latch
        });

        eventConsumed.await(1, TimeUnit.SECONDS);

        // 2nd consumer is added
        eventProcessor.registerConsumer(Integer.class.getName(), eventConsumer2);

        future.get();

        waitForConsumerRegistration.countDown();

        then(logger).should(times(1)).info("1");
    }

    @Test
    public void testConcurrentConsumerRegistrationInBothThreadModes() throws Exception {
        System.out.println("Running testConcurrentConsumerRegistrationInBothThreadModes in " + getThreadModeDescription());
        
        EventProcessor<Integer> eventProcessor = new EventProcessor<>();
        int concurrentThreads = isVirtualThreadMode() ? 20 : 5; // More threads in virtual mode
        AtomicInteger totalConsumers = new AtomicInteger(0);
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(concurrentThreads);
        
        // Create appropriate executor based on thread mode
        try (ExecutorService executor = isVirtualThreadMode() ? 
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
                .as("All consumer registrations should complete within timeout in " + getThreadModeDescription())
                .isTrue();
        }
        
        // Process an event to verify all consumers were registered
        boolean consumed = eventProcessor.processEvent(42);
        
        // Verify processing results
        assertThat(consumed)
            .as("Event should be consumed in " + getThreadModeDescription())
            .isTrue();
        assertThat(totalConsumers.get())
            .as("All consumers should process the event in " + getThreadModeDescription())
            .isEqualTo(concurrentThreads);
        
        System.out.println("✅ Concurrent consumer registration test passed in " + getThreadModeDescription() + 
                         " - Consumers: " + concurrentThreads);
    }

    @Test
    public void testConcurrentEventProcessingInBothThreadModes() throws Exception {
        System.out.println("Running testConcurrentEventProcessingInBothThreadModes in " + getThreadModeDescription());
        
        EventProcessor<Integer> eventProcessor = new EventProcessor<>();
        int concurrentThreads = isVirtualThreadMode() ? 15 : 3; // More threads in virtual mode
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
        try (ExecutorService executor = isVirtualThreadMode() ? 
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
                .as("All event processing should complete within timeout in " + getThreadModeDescription())
                .isTrue();
        }
        
        // Verify results
        assertThat(eventsProcessed.get())
            .as("All events should be processed in " + getThreadModeDescription())
            .isEqualTo(concurrentThreads);
        
        // Thread type verification
        if (isVirtualThreadMode()) {
            assertThat(virtualThreadCount.get())
                .as("Events should be processed on virtual threads when configured in " + getThreadModeDescription())
                .isEqualTo(concurrentThreads);
        } else {
            assertThat(virtualThreadCount.get())
                .as("Events should be processed on platform threads by default in " + getThreadModeDescription())
                .isZero();
        }
        
        System.out.println("✅ Concurrent event processing test passed in " + getThreadModeDescription() + 
                         " - Events: " + concurrentThreads + ", Virtual threads: " + virtualThreadCount.get());
    }

}
