package io.github.resilience4j.hedge;

import io.github.resilience4j.core.ThreadModeExtension;
import io.github.resilience4j.core.ThreadType;
import io.github.resilience4j.hedge.event.HedgeEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency tests for Hedge pattern with both virtual and platform threads.
 * Tests thread safety, concurrent operations, race conditions, and resource cleanup.
 * Tests run in both platform and virtual thread modes.
 *
 * @author kanghyun.yang
 * @since 3.0.0
 */
@ExtendWith(ThreadModeExtension.class)
class HedgeConcurrencyTest {

    private static final Logger LOG = LoggerFactory.getLogger(HedgeConcurrencyTest.class);

    private static final int NUM_THREADS = 10;

    private ScheduledExecutorService hedgeExecutor;
    private ExecutorService testExecutor;
    private HedgeRegistry hedgeRegistry;
    private final List<Hedge> createdHedges = new ArrayList<>();

    @BeforeEach
    void setUp() {
        hedgeExecutor = Executors.newScheduledThreadPool(NUM_THREADS);
        testExecutor = Executors.newFixedThreadPool(NUM_THREADS);
        hedgeRegistry = HedgeRegistry.builder().build();
        createdHedges.clear();
    }

    private Hedge track(Hedge hedge) {
        createdHedges.add(hedge);
        return hedge;
    }

    @AfterEach
    void tearDown() {
        for (Hedge hedge : createdHedges) {
            hedge.close();
        }
        createdHedges.clear();

        hedgeRegistry.close();
        shutdown(hedgeExecutor);
        shutdown(testExecutor);
    }

    @TestTemplate
    void shouldHandleConcurrentHedgeOperations(ThreadType threadType) throws Exception {
        LOG.info("Testing concurrent hedge operations with {}", threadType);

        Hedge hedge = track(Hedge.of(Duration.ofMillis(50)));
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(NUM_THREADS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicReference<Exception> exception = new AtomicReference<>();

        for (int i = 0; i < NUM_THREADS; i++) {
            final int threadId = i;
            testExecutor.submit(() -> {
                try {
                    startLatch.await();

                    Callable<String> task = () -> {
                        Thread.sleep(10);
                        return "result-" + threadId;
                    };

                    String result = hedge.submit(task, hedgeExecutor).get(5, TimeUnit.SECONDS);
                    if (result != null && result.equals("result-" + threadId)) {
                        successCount.incrementAndGet();
                    }

                } catch (Exception e) {
                    exception.set(e);
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertThat(completionLatch.await(30, TimeUnit.SECONDS)).isTrue();

        if (exception.get() != null) {
            throw new AssertionError("Concurrent execution failed", exception.get());
        }

        assertThat(successCount.get()).isEqualTo(NUM_THREADS);

        LOG.info("Concurrent hedge operations test passed with {}", threadType);
    }

    @TestTemplate
    void shouldHandleConcurrentHedgeRegistryOperations(ThreadType threadType) throws Exception {
        LOG.info("Testing concurrent hedge registry operations with {}", threadType);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(NUM_THREADS);
        AtomicInteger registryOperationCount = new AtomicInteger(0);
        AtomicReference<Exception> exception = new AtomicReference<>();

        for (int i = 0; i < NUM_THREADS; i++) {
            final int threadId = i;
            testExecutor.submit(() -> {
                try {
                    startLatch.await();

                    String hedgeName = "hedge-" + threadId;
                    HedgeConfig config = HedgeConfig.custom()
                        .preconfiguredDuration(Duration.ofMillis(30))
                        .build();

                    Hedge hedge = hedgeRegistry.hedge(hedgeName, config);
                    assertThat(hedge).isNotNull();

                    Callable<String> task = () -> "result-" + threadId;
                    String result = hedge.submit(task, hedgeExecutor).get(5, TimeUnit.SECONDS);
                    assertThat(result).isEqualTo("result-" + threadId);

                    assertThat(hedgeRegistry.getAllHedges()
                        .anyMatch(h -> h.getName().equals(hedgeName))).isTrue();

                    registryOperationCount.incrementAndGet();

                } catch (Exception e) {
                    exception.set(e);
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertThat(completionLatch.await(30, TimeUnit.SECONDS)).isTrue();

        if (exception.get() != null) {
            throw new AssertionError("Concurrent registry operations failed", exception.get());
        }

        assertThat(registryOperationCount.get()).isEqualTo(NUM_THREADS);
        assertThat(hedgeRegistry.getAllHedges().count()).isEqualTo(NUM_THREADS);

        LOG.info("Concurrent hedge registry operations test passed with {}", threadType);
    }

    @TestTemplate
    void shouldHandleConcurrentEventPublishing(ThreadType threadType) throws Exception {
        LOG.info("Testing concurrent event publishing with {}", threadType);

        HedgeConfig config = HedgeConfig.custom()
            .preconfiguredDuration(Duration.ofMillis(50))
            .build();

        Hedge hedge = track(Hedge.of(config));
        List<HedgeEvent> events = Collections.synchronizedList(new ArrayList<>());
        hedge.getEventPublisher().onEvent(events::add);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(NUM_THREADS);
        AtomicReference<Exception> exception = new AtomicReference<>();

        for (int i = 0; i < NUM_THREADS; i++) {
            final int threadId = i;
            testExecutor.submit(() -> {
                try {
                    startLatch.await();

                    Callable<String> task = () -> {
                        Thread.sleep(10);
                        return "event-test-" + threadId;
                    };
                    hedge.submit(task, hedgeExecutor).get(5, TimeUnit.SECONDS);

                } catch (Exception e) {
                    exception.set(e);
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertThat(completionLatch.await(30, TimeUnit.SECONDS)).isTrue();

        if (exception.get() != null) {
            throw new AssertionError("Concurrent event publishing failed", exception.get());
        }

        Thread.sleep(200);
        assertThat(events).isNotEmpty();

        LOG.info("Concurrent event publishing test passed with {}", threadType);
    }

    @TestTemplate
    void shouldHandleRaceConditionsBetweenOperations(ThreadType threadType) throws Exception {
        LOG.info("Testing race conditions with {}", threadType);

        HedgeConfig config = HedgeConfig.custom()
            .preconfiguredDuration(Duration.ofMillis(30))
            .build();

        Hedge hedge = track(Hedge.of(config));
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(NUM_THREADS);
        AtomicInteger responseCount = new AtomicInteger(0);
        AtomicReference<Exception> exception = new AtomicReference<>();

        for (int i = 0; i < NUM_THREADS; i++) {
            final int threadId = i;
            testExecutor.submit(() -> {
                try {
                    startLatch.await();

                    Callable<String> task = () -> {
                        Thread.sleep(5L + (threadId % 5));
                        return "result-" + threadId;
                    };
                    String result = hedge.submit(task, hedgeExecutor).get(5, TimeUnit.SECONDS);

                    if (result != null) {
                        responseCount.incrementAndGet();
                    }

                } catch (Exception e) {
                    exception.set(e);
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertThat(completionLatch.await(30, TimeUnit.SECONDS)).isTrue();

        if (exception.get() != null) {
            throw new AssertionError("Race condition test failed", exception.get());
        }

        assertThat(responseCount.get()).isEqualTo(NUM_THREADS);

        LOG.info("Race conditions test passed with {}", threadType);
    }

    @TestTemplate
    void shouldHandleConcurrentHedgeTriggering(ThreadType threadType) throws Exception {
        LOG.info("Testing concurrent hedge triggering with {}", threadType);

        HedgeConfig config = HedgeConfig.custom()
            .preconfiguredDuration(Duration.ofMillis(30))
            .build();

        Hedge hedge = track(Hedge.of(config));
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(5);
        AtomicInteger responseCount = new AtomicInteger(0);
        AtomicReference<Exception> exception = new AtomicReference<>();

        for (int i = 0; i < 5; i++) {
            final int threadId = i;
            testExecutor.submit(() -> {
                try {
                    startLatch.await();

                    Callable<String> task = () -> {
                        Thread.sleep(50L + threadId * 10L);
                        return "hedge-result-" + threadId;
                    };
                    String result = hedge.submit(task, hedgeExecutor).get(10, TimeUnit.SECONDS);

                    if (result != null) {
                        responseCount.incrementAndGet();
                    }

                } catch (Exception e) {
                    exception.set(e);
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertThat(completionLatch.await(30, TimeUnit.SECONDS)).isTrue();

        if (exception.get() != null) {
            throw new AssertionError("Hedge triggering test failed", exception.get());
        }

        assertThat(responseCount.get()).isEqualTo(5);

        LOG.info("Concurrent hedge triggering test passed with {}", threadType);
    }

    private static void shutdown(ExecutorService executor) {
        if (executor == null || executor.isShutdown()) {
            return;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
