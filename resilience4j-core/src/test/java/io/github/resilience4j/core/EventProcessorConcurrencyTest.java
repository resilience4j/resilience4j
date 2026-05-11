package io.github.resilience4j.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency tests for EventProcessor to verify correct behavior under concurrent
 * consumer registration and event processing.
 */
class EventProcessorConcurrencyTest {

    private static final Logger LOG = LoggerFactory.getLogger(EventProcessorConcurrencyTest.class);

    private static class TestEvent {
        private final String data;

        TestEvent(String data) {
            this.data = data;
        }

        String getData() {
            return data;
        }
    }

    private EventProcessor<TestEvent> eventProcessor;

    @BeforeEach
    void setUp() {
        eventProcessor = new EventProcessor<>();
    }

    @Test
    void shouldHandleConcurrentConsumerRegistration() throws Exception {
        int threadCount = 50;
        int consumersPerThread = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger totalRegistrations = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                final int threadIndex = i;
                Future<?> future = executor.submit(() -> {
                    try {
                        startLatch.await();

                        for (int j = 0; j < consumersPerThread; j++) {
                            String className = "TestEvent" + threadIndex + "_" + j;
                            EventConsumer<TestEvent> consumer = event -> {};
                            eventProcessor.registerConsumer(className, consumer);
                            totalRegistrations.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
                futures.add(future);
            }

            startLatch.countDown();
            assertThat(doneLatch.await(10, TimeUnit.SECONDS))
                .as("All registration threads should complete")
                .isTrue();

            for (Future<?> future : futures) {
                future.get(1, TimeUnit.SECONDS);
            }

            assertThat(totalRegistrations.get())
                .as("All consumers should be registered")
                .isEqualTo(threadCount * consumersPerThread);

            assertThat(eventProcessor.hasConsumers())
                .as("EventProcessor should have consumers")
                .isTrue();

            LOG.info("Concurrent consumer registration test passed - {} consumers registered",
                totalRegistrations.get());
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void shouldHandleConcurrentEventProcessing() throws Exception {
        int consumerCount = 20;
        AtomicInteger[] consumptionCounts = new AtomicInteger[consumerCount];

        for (int i = 0; i < consumerCount; i++) {
            final int consumerIndex = i;
            consumptionCounts[i] = new AtomicInteger(0);
            eventProcessor.onEvent(event -> consumptionCounts[consumerIndex].incrementAndGet());
        }

        int threadCount = 30;
        int eventsPerThread = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            for (int i = 0; i < threadCount; i++) {
                final int threadIndex = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < eventsPerThread; j++) {
                            eventProcessor.processEvent(new TestEvent("Event-" + threadIndex + "-" + j));
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertThat(doneLatch.await(15, TimeUnit.SECONDS))
                .as("All event processing threads should complete")
                .isTrue();

            int expectedEventsPerConsumer = threadCount * eventsPerThread;
            for (int i = 0; i < consumerCount; i++) {
                assertThat(consumptionCounts[i].get())
                    .as("Consumer %d should have received all events", i)
                    .isEqualTo(expectedEventsPerConsumer);
            }

            LOG.info("Concurrent event processing test passed - {} events processed by {} consumers",
                threadCount * eventsPerThread, consumerCount);
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void shouldHandleConcurrentRegistrationAndEventProcessing() throws Exception {
        int registrationThreads = 20;
        int eventThreads = 20;
        int operationsPerThread = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(registrationThreads + eventThreads);
        AtomicInteger totalEventsProcessed = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(registrationThreads + eventThreads);
        try {
            for (int i = 0; i < registrationThreads; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < operationsPerThread; j++) {
                            eventProcessor.onEvent(event -> totalEventsProcessed.incrementAndGet());
                            Thread.sleep(1);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            for (int i = 0; i < eventThreads; i++) {
                final int threadIndex = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < operationsPerThread; j++) {
                            eventProcessor.processEvent(new TestEvent("Event-" + threadIndex + "-" + j));
                            Thread.sleep(1);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertThat(doneLatch.await(30, TimeUnit.SECONDS))
                .as("All threads should complete without deadlock")
                .isTrue();

            assertThat(totalEventsProcessed.get())
                .as("Events should have been processed")
                .isGreaterThan(0);

            assertThat(eventProcessor.hasConsumers())
                .as("Should have consumers registered")
                .isTrue();

            LOG.info("Concurrent registration and processing test passed - {} event consumptions",
                totalEventsProcessed.get());
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void shouldHandleTypeSpecificConsumerRegistration() throws Exception {
        int threadCount = 30;
        int operationsPerThread = 50;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger typeACount = new AtomicInteger(0);
        AtomicInteger typeBCount = new AtomicInteger(0);

        eventProcessor.registerConsumer("TypeA", event -> typeACount.incrementAndGet());
        eventProcessor.registerConsumer("TypeB", event -> typeBCount.incrementAndGet());

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            for (int i = 0; i < threadCount; i++) {
                final int threadIndex = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < operationsPerThread; j++) {
                            eventProcessor.processEvent(new TestEvent("Data-" + threadIndex + "-" + j));
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertThat(doneLatch.await(15, TimeUnit.SECONDS))
                .as("Type-specific event processing should complete")
                .isTrue();

            LOG.info("Type-specific consumer test passed - TypeA: {}, TypeB: {}",
                typeACount.get(), typeBCount.get());
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void shouldCorrectlyReportHasConsumersUnderConcurrency() throws Exception {
        assertThat(eventProcessor.hasConsumers())
            .as("Initially should have no consumers")
            .isFalse();

        int threadCount = 20;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            for (int i = 0; i < threadCount; i++) {
                final int threadIndex = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        EventConsumer<TestEvent> consumer = event -> {};
                        if (threadIndex % 2 == 0) {
                            eventProcessor.onEvent(consumer);
                        } else {
                            eventProcessor.registerConsumer("Type" + threadIndex, consumer);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertThat(doneLatch.await(10, TimeUnit.SECONDS))
                .as("Registration should complete")
                .isTrue();

            assertThat(eventProcessor.hasConsumers())
                .as("Should accurately report having consumers after concurrent registration")
                .isTrue();

            LOG.info("hasConsumers() correctness test passed under concurrency");
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
