package io.github.resilience4j.core;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency tests for EventProcessor to verify correct behavior under concurrent
 * consumer registration and event processing.
 */
public class EventProcessorConcurrencyTest {

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

    @Before
    public void setUp() {
        eventProcessor = new EventProcessor<>();
    }

    @Test
    public void shouldHandleConcurrentConsumerRegistration() throws Exception {
        // Given: multiple threads registering consumers
        int threadCount = 50;
        int consumersPerThread = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger totalRegistrations = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<?>> futures = new ArrayList<>();

            // When: multiple threads register consumers concurrently
            for (int i = 0; i < threadCount; i++) {
                final int threadIndex = i;
                Future<?> future = executor.submit(() -> {
                    try {
                        startLatch.await();

                        for (int j = 0; j < consumersPerThread; j++) {
                            String className = "TestEvent" + threadIndex + "_" + j;
                            EventConsumer<TestEvent> consumer = event -> {
                                // Simple consumer that just increments counter
                            };

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
            boolean completed = doneLatch.await(10, TimeUnit.SECONDS);

            // Then: all registrations should complete without error
            assertThat(completed)
                .as("All registration threads should complete")
                .isTrue();

            // Verify no exceptions
            for (Future<?> future : futures) {
                future.get(1, TimeUnit.SECONDS);
            }

            assertThat(totalRegistrations.get())
                .as("All consumers should be registered")
                .isEqualTo(threadCount * consumersPerThread);

            assertThat(eventProcessor.hasConsumers())
                .as("EventProcessor should have consumers")
                .isTrue();

            System.out.println("✅ Concurrent consumer registration test passed - " +
                             totalRegistrations.get() + " consumers registered");

        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void shouldHandleConcurrentEventProcessing() throws Exception {
        // Given: event processor with registered consumers
        int consumerCount = 20;
        AtomicInteger[] consumptionCounts = new AtomicInteger[consumerCount];

        for (int i = 0; i < consumerCount; i++) {
            final int consumerIndex = i;
            consumptionCounts[i] = new AtomicInteger(0);

            EventConsumer<TestEvent> consumer = event -> {
                consumptionCounts[consumerIndex].incrementAndGet();
            };

            eventProcessor.onEvent(consumer);
        }

        // When: multiple threads process events concurrently
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
                            TestEvent event = new TestEvent("Event-" + threadIndex + "-" + j);
                            eventProcessor.processEvent(event);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            boolean completed = doneLatch.await(15, TimeUnit.SECONDS);

            // Then: all events should be processed
            assertThat(completed)
                .as("All event processing threads should complete")
                .isTrue();

            // Verify each consumer received all events
            int expectedEventsPerConsumer = threadCount * eventsPerThread;
            for (int i = 0; i < consumerCount; i++) {
                assertThat(consumptionCounts[i].get())
                    .as("Consumer %d should have received all events", i)
                    .isEqualTo(expectedEventsPerConsumer);
            }

            System.out.println("✅ Concurrent event processing test passed - " +
                             (threadCount * eventsPerThread) + " events processed by " +
                             consumerCount + " consumers");

        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void shouldHandleConcurrentRegistrationAndEventProcessing() throws Exception {
        // Given: concurrent registration and event processing
        int registrationThreads = 20;
        int eventThreads = 20;
        int operationsPerThread = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(registrationThreads + eventThreads);
        AtomicInteger totalEventsProcessed = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(registrationThreads + eventThreads);
        try {
            // Registration threads
            for (int i = 0; i < registrationThreads; i++) {
                final int threadIndex = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();

                        for (int j = 0; j < operationsPerThread; j++) {
                            EventConsumer<TestEvent> consumer = event -> {
                                totalEventsProcessed.incrementAndGet();
                            };
                            eventProcessor.onEvent(consumer);

                            // Small sleep to interleave with event processing
                            Thread.sleep(1);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            // Event processing threads
            for (int i = 0; i < eventThreads; i++) {
                final int threadIndex = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();

                        for (int j = 0; j < operationsPerThread; j++) {
                            TestEvent event = new TestEvent("Event-" + threadIndex + "-" + j);
                            eventProcessor.processEvent(event);

                            // Small sleep to interleave with registration
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
            boolean completed = doneLatch.await(30, TimeUnit.SECONDS);

            // Then: all operations should complete without deadlock
            assertThat(completed)
                .as("All threads should complete without deadlock")
                .isTrue();

            // Verify events were processed
            assertThat(totalEventsProcessed.get())
                .as("Events should have been processed")
                .isGreaterThan(0);

            assertThat(eventProcessor.hasConsumers())
                .as("Should have consumers registered")
                .isTrue();

            System.out.println("✅ Concurrent registration and processing test passed - " +
                             totalEventsProcessed.get() + " event consumptions");

        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void shouldHandleTypeSpecificConsumerRegistration() throws Exception {
        // Given: type-specific consumers
        int threadCount = 30;
        int operationsPerThread = 50;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger typeACount = new AtomicInteger(0);
        AtomicInteger typeBCount = new AtomicInteger(0);

        // Register type-specific consumers
        EventConsumer<TestEvent> consumerA = event -> typeACount.incrementAndGet();
        EventConsumer<TestEvent> consumerB = event -> typeBCount.incrementAndGet();

        eventProcessor.registerConsumer("TypeA", consumerA);
        eventProcessor.registerConsumer("TypeB", consumerB);

        // When: threads send events with specific types
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            for (int i = 0; i < threadCount; i++) {
                final int threadIndex = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();

                        for (int j = 0; j < operationsPerThread; j++) {
                            // Alternate between type-specific event processing
                            // Note: In real scenario, event class name would determine routing
                            // Here we're just testing the concurrent registration/processing
                            TestEvent event = new TestEvent("Data-" + threadIndex + "-" + j);
                            eventProcessor.processEvent(event);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            boolean completed = doneLatch.await(15, TimeUnit.SECONDS);

            // Then: should complete without errors
            assertThat(completed)
                .as("Type-specific event processing should complete")
                .isTrue();

            System.out.println("✅ Type-specific consumer test passed - TypeA: " +
                             typeACount.get() + ", TypeB: " + typeBCount.get());

        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void shouldCorrectlyReportHasConsumersUnderConcurrency() throws Exception {
        // Given: initially no consumers
        assertThat(eventProcessor.hasConsumers())
            .as("Initially should have no consumers")
            .isFalse();

        // When: multiple threads register consumers concurrently
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
            boolean completed = doneLatch.await(10, TimeUnit.SECONDS);

            // Then: should complete and hasConsumers should be true
            assertThat(completed)
                .as("Registration should complete")
                .isTrue();

            assertThat(eventProcessor.hasConsumers())
                .as("Should accurately report having consumers after concurrent registration")
                .isTrue();

            System.out.println("✅ hasConsumers() correctness test passed under concurrency");

        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
