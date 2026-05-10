/*
 *
 *  Copyright 2026 Robert Winkler, Lucas Lech
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
package io.github.resilience4j.bulkhead.internal;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.core.ThreadModeExtension;
import io.github.resilience4j.core.ThreadType;
import io.github.resilience4j.core.exception.AcquirePermissionCancelledException;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.InMemoryRegistryStore;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.test.RxJava2Adapter;
import io.reactivex.subscribers.TestSubscriber;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import static io.github.resilience4j.bulkhead.BulkheadConfig.DEFAULT_FAIR_CALL_HANDLING_STRATEGY_ENABLED;
import static io.github.resilience4j.bulkhead.BulkheadConfig.DEFAULT_MAX_CONCURRENT_CALLS;
import static io.github.resilience4j.bulkhead.BulkheadConfig.DEFAULT_WRITABLE_STACK_TRACE_ENABLED;
import static io.github.resilience4j.bulkhead.event.BulkheadEvent.Type.CALL_FINISHED;
import static io.github.resilience4j.bulkhead.event.BulkheadEvent.Type.CALL_PERMITTED;
import static io.github.resilience4j.bulkhead.event.BulkheadEvent.Type.CALL_REJECTED;
import static java.lang.Thread.State.RUNNABLE;
import static java.lang.Thread.State.TERMINATED;
import static java.lang.Thread.State.TIMED_WAITING;
import static java.lang.Thread.State.WAITING;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@ExtendWith(ThreadModeExtension.class)
class SemaphoreBulkheadTest {

    private static final Logger LOG = LoggerFactory.getLogger(SemaphoreBulkheadTest.class);

    // Each @TestTemplate invocation creates a fresh instance, so these fields are per-invocation.
    // setUp() is called via @TestTemplate's BeforeEachCallback from ThreadModeExtension,
    // but since there's no @BeforeEach here the fields are initialised inside each test method.
    // We use a helper to build a standard bulkhead per test.
    private Bulkhead createDefaultBulkhead(ThreadType threadType) {
        BulkheadConfig config = BulkheadConfig.custom()
            .maxConcurrentCalls(2)
            .maxWaitDuration(Duration.ofMillis(0))
            .build();
        return Bulkhead.of("test-" + threadType, config);
    }

    private TestSubscriber<BulkheadEvent.Type> subscribe(Bulkhead bulkhead) {
        return RxJava2Adapter.toFlowable(bulkhead.getEventPublisher())
            .map(BulkheadEvent::getEventType)
            .test();
    }

    @TestTemplate
    void shouldReturnTheCorrectName(ThreadType threadType) {
        Bulkhead bulkhead = createDefaultBulkhead(threadType);
        assertThat(bulkhead.getName()).isEqualTo("test-" + threadType);
    }

    @TestTemplate
    void shouldHandleBasicBulkheadOperationsInBothThreadModes(ThreadType threadType) {
        LOG.info("Running shouldHandleBasicBulkheadOperationsInBothThreadModes in {}", threadType);

        Bulkhead bulkhead = createDefaultBulkhead(threadType);
        TestSubscriber<BulkheadEvent.Type> testSubscriber = subscribe(bulkhead);

        boolean firstPermission = bulkhead.tryAcquirePermission();
        assertThat(firstPermission)
            .as("First permission should be acquired in %s", threadType)
            .isTrue();

        boolean secondPermission = bulkhead.tryAcquirePermission();
        assertThat(secondPermission)
            .as("Second permission should be acquired in %s", threadType)
            .isTrue();

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls())
            .as("No concurrent calls should be available in %s", threadType)
            .isZero();

        boolean thirdPermission = bulkhead.tryAcquirePermission();
        assertThat(thirdPermission)
            .as("Third permission should be rejected in %s", threadType)
            .isFalse();

        bulkhead.onComplete();
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls())
            .as("One concurrent call should be available after completion in %s", threadType)
            .isEqualTo(1);

        bulkhead.onComplete();
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls())
            .as("Two concurrent calls should be available after second completion in %s", threadType)
            .isEqualTo(2);

        boolean fourthPermission = bulkhead.tryAcquirePermission();
        assertThat(fourthPermission)
            .as("Fourth permission should be acquired after releases in %s", threadType)
            .isTrue();

        testSubscriber.assertValueCount(6)
            .assertValues(CALL_PERMITTED, CALL_PERMITTED, CALL_REJECTED, CALL_FINISHED,
                CALL_FINISHED, CALL_PERMITTED);

        LOG.info("Basic bulkhead operations test passed in {}", threadType);
    }

    @TestTemplate
    void testToString(ThreadType threadType) {
        Bulkhead bulkhead = createDefaultBulkhead(threadType);
        assertThat(bulkhead.toString()).isEqualTo("Bulkhead 'test-" + threadType + "'");
    }

    @TestTemplate
    void createWithNullConfig(ThreadType threadType) {
        Supplier<BulkheadConfig> configSupplier = () -> null;
        assertThatThrownBy(() -> Bulkhead.of("test", configSupplier))
            .isInstanceOf(NullPointerException.class).hasMessage("Config must not be null");
    }

    @TestTemplate
    void createWithDefaults(ThreadType threadType) {
        Bulkhead bulkhead = Bulkhead.ofDefaults("test");

        assertThat(bulkhead).isNotNull();
        assertThat(bulkhead.getBulkheadConfig()).isNotNull();
        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls())
            .isEqualTo(DEFAULT_MAX_CONCURRENT_CALLS);
        assertThat(bulkhead.getBulkheadConfig().isWritableStackTraceEnabled())
            .isEqualTo(DEFAULT_WRITABLE_STACK_TRACE_ENABLED);
        assertThat(bulkhead.getBulkheadConfig().isFairCallHandlingEnabled())
            .isEqualTo(DEFAULT_FAIR_CALL_HANDLING_STRATEGY_ENABLED);
    }

    @TestTemplate
    void shouldHandleTimeoutBehaviorConsistentlyInBothThreadModes(ThreadType threadType) throws InterruptedException {
        LOG.info("Running shouldHandleTimeoutBehaviorConsistentlyInBothThreadModes in {}", threadType);

        long expectedMillisOfWaitTime = 50;
        BulkheadConfig config = BulkheadConfig.custom()
            .maxConcurrentCalls(1)
            .maxWaitDuration(Duration.ofMillis(expectedMillisOfWaitTime))
            .build();
        SemaphoreBulkhead bulkhead = new SemaphoreBulkhead("timeoutTest-" + threadType, config);

        boolean entered = bulkhead.tryEnterBulkhead();

        Thread subTestRoutine = new Thread(() -> {
            long start = System.nanoTime();
            boolean acquired = bulkhead.tryAcquirePermission();
            Duration actualWaitTime = Duration.ofNanos(System.nanoTime() - start);

            assertThat(acquired)
                .as("Permission should be rejected due to timeout in %s", threadType)
                .isFalse();
            assertThat(actualWaitTime.toMillis())
                .as("Wait time should be within expected range in %s", threadType)
                .isBetween(expectedMillisOfWaitTime, (long) (expectedMillisOfWaitTime * 1.3));
        });
        subTestRoutine.setDaemon(true);
        subTestRoutine.start();

        assertThat(entered)
            .as("Initial entry should succeed in %s", threadType)
            .isTrue();

        subTestRoutine.join(2 * expectedMillisOfWaitTime);
        assertThat(subTestRoutine.isInterrupted())
            .as("Sub-thread should not be interrupted in %s", threadType)
            .isFalse();
        assertThat(subTestRoutine.isAlive())
            .as("Sub-thread should complete in %s", threadType)
            .isFalse();

        LOG.info("Timeout behavior test passed in {}", threadType);
    }

    @TestTemplate
    void tryEnterWithInterruptDuringTimeout(ThreadType threadType) throws InterruptedException {
        Duration expectedWaitTime = Duration.ofMillis(2000);
        BulkheadConfig config = BulkheadConfig.custom()
            .maxConcurrentCalls(1)
            .maxWaitDuration(expectedWaitTime)
            .build();
        SemaphoreBulkhead bulkhead = new SemaphoreBulkhead("test", config);

        AtomicBoolean interruptedWithoutCodeFlowBreak = new AtomicBoolean(false);
        boolean entered = bulkhead.tryEnterBulkhead();
        Thread subTestRoutine = new Thread(() -> {
            long start = System.nanoTime();
            boolean acquired = bulkhead.tryAcquirePermission();
            Duration actualWaitTime = Duration.ofNanos(System.nanoTime() - start);
            assertThat(acquired).isFalse();
            assertThat(actualWaitTime).isLessThan(expectedWaitTime);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            interruptedWithoutCodeFlowBreak.set(true);
        });
        subTestRoutine.setDaemon(true);
        subTestRoutine.start();

        await().atMost(expectedWaitTime.dividedBy(2).toMillis(), MILLISECONDS)
            .pollInterval(expectedWaitTime.dividedBy(100).toMillis(), MILLISECONDS)
            .until(() -> subTestRoutine.getState() == TIMED_WAITING);
        subTestRoutine.interrupt();
        await().atMost(expectedWaitTime.dividedBy(2).toMillis(), MILLISECONDS)
            .pollInterval(expectedWaitTime.dividedBy(100).toMillis(), MILLISECONDS)
            .until(() -> subTestRoutine.getState() == TERMINATED);
        assertThat(entered).isTrue();
        assertThat(interruptedWithoutCodeFlowBreak.get()).isTrue();
        assertThat(subTestRoutine.isAlive()).isFalse();
    }

    @TestTemplate
    void acquireWithInterruptDuringTimeout(ThreadType threadType) throws InterruptedException {
        Duration expectedWaitTime = Duration.ofMillis(2000);
        BulkheadConfig configTemplate = BulkheadConfig.custom()
            .maxConcurrentCalls(1)
            .maxWaitDuration(expectedWaitTime)
            .build();
        BulkheadConfig config = BulkheadConfig.from(configTemplate).build();
        SemaphoreBulkhead bulkhead = new SemaphoreBulkhead("test", config);

        AtomicBoolean interruptedWithoutCodeFlowBreak = new AtomicBoolean(false);
        AtomicBoolean interruptedWithException = new AtomicBoolean(false);
        boolean entered = bulkhead.tryEnterBulkhead();
        Thread subTestRoutine = new Thread(() -> {
            long start = System.nanoTime();
            try {
                bulkhead.acquirePermission();
            } catch (AcquirePermissionCancelledException bulkheadException) {
                assertThat(bulkheadException.getMessage())
                    .contains("interrupted while waiting for a permission");
                interruptedWithException.set(true);
            } finally {
                Duration actualWaitTime = Duration.ofNanos(System.nanoTime() - start);
                assertThat(actualWaitTime).isLessThan(expectedWaitTime);
                assertThat(Thread.currentThread().isInterrupted()).isTrue();
                interruptedWithoutCodeFlowBreak.set(true);
            }
        });
        subTestRoutine.setDaemon(true);
        subTestRoutine.start();

        await().atMost(expectedWaitTime.dividedBy(2).toMillis(), MILLISECONDS)
            .pollInterval(expectedWaitTime.dividedBy(100).toMillis(), MILLISECONDS)
            .until(() -> subTestRoutine.getState() == TIMED_WAITING);
        subTestRoutine.interrupt();
        await().atMost(expectedWaitTime.dividedBy(2).toMillis(), MILLISECONDS)
            .pollInterval(expectedWaitTime.dividedBy(100).toMillis(), MILLISECONDS)
            .until(() -> subTestRoutine.getState() == TERMINATED);
        assertThat(entered).isTrue();
        assertThat(interruptedWithoutCodeFlowBreak.get()).isTrue();
        assertThat(interruptedWithException.get()).isTrue();
        assertThat(subTestRoutine.isAlive()).isFalse();
    }

    @TestTemplate
    void zeroMaxConcurrentCalls(ThreadType threadType) {
        BulkheadConfig config = BulkheadConfig.custom()
            .maxConcurrentCalls(0)
            .maxWaitDuration(Duration.ofMillis(0))
            .build();
        SemaphoreBulkhead bulkhead = new SemaphoreBulkhead("test", config);

        assertThat(bulkhead.tryAcquirePermission()).isFalse();
    }

    @TestTemplate
    void entryTimeout(ThreadType threadType) {
        BulkheadConfig config = BulkheadConfig.custom()
            .maxConcurrentCalls(1)
            .maxWaitDuration(Duration.ofMillis(10))
            .build();
        SemaphoreBulkhead bulkhead = new SemaphoreBulkhead("test", config);
        bulkhead.tryAcquirePermission(); // consume the permit

        assertThat(bulkhead.tryEnterBulkhead()).isFalse();
    }

    @TestTemplate
    void changePermissionsInIdleState(ThreadType threadType) {
        BulkheadConfig originalConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(3)
            .maxWaitDuration(Duration.ofMillis(5000))
            .build();
        SemaphoreBulkhead bulkhead = new SemaphoreBulkhead("test", originalConfig);

        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(3);
        Assertions.assertThat(bulkhead.getBulkheadConfig().getMaxWaitDuration()).hasMillis(5000);

        BulkheadConfig newConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(5)
            .maxWaitDuration(Duration.ofMillis(5000))
            .build();

        bulkhead.changeConfig(newConfig);
        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(5);
        Assertions.assertThat(bulkhead.getBulkheadConfig().getMaxWaitDuration()).hasMillis(5000);

        newConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(2)
            .maxWaitDuration(Duration.ofMillis(5000))
            .build();

        bulkhead.changeConfig(newConfig);
        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(2);
        Assertions.assertThat(bulkhead.getBulkheadConfig().getMaxWaitDuration()).hasMillis(5000);

        bulkhead.changeConfig(newConfig);
    }

    @TestTemplate
    void changeWaitTimeInIdleState(ThreadType threadType) {
        BulkheadConfig originalConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(3)
            .maxWaitDuration(Duration.ofMillis(5000))
            .build();
        SemaphoreBulkhead bulkhead = new SemaphoreBulkhead("test", originalConfig);

        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(3);
        Assertions.assertThat(bulkhead.getBulkheadConfig().getMaxWaitDuration()).hasMillis(5000);

        BulkheadConfig newConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(3)
            .maxWaitDuration(Duration.ofMillis(3000))
            .build();

        bulkhead.changeConfig(newConfig);
        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(3);
        Assertions.assertThat(bulkhead.getBulkheadConfig().getMaxWaitDuration()).hasMillis(3000);

        newConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(3)
            .maxWaitDuration(Duration.ofMillis(7000))
            .build();

        bulkhead.changeConfig(newConfig);
        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(3);
        Assertions.assertThat(bulkhead.getBulkheadConfig().getMaxWaitDuration()).hasMillis(7000);

        bulkhead.changeConfig(newConfig);
    }

    @SuppressWarnings("Duplicates")
    @TestTemplate
    void changePermissionsCountWhileOneThreadIsRunningWithThisPermission(ThreadType threadType) {
        BulkheadConfig originalConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(1)
            .maxWaitDuration(Duration.ofMillis(0))
            .build();
        SemaphoreBulkhead bulkhead = new SemaphoreBulkhead("test", originalConfig);

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isOne();

        AtomicBoolean bulkheadThreadTrigger = new AtomicBoolean(true);
        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isOne();
        Thread bulkheadThread = new Thread(() -> {
            bulkhead.tryAcquirePermission();
            while (bulkheadThreadTrigger.get()) {
                Thread.yield();
            }
            bulkhead.onComplete();
        });
        bulkheadThread.setDaemon(true);
        bulkheadThread.start();

        await().atMost(1, SECONDS)
            .until(() -> bulkheadThread.getState().equals(RUNNABLE));

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isZero();
        assertThat(bulkhead.tryEnterBulkhead()).isFalse();

        BulkheadConfig newConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(2)
            .maxWaitDuration(Duration.ofMillis(0))
            .build();

        bulkhead.changeConfig(newConfig);
        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(2);
        assertThat(bulkhead.getBulkheadConfig().getMaxWaitDuration().toMillis()).isZero();
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        assertThat(bulkhead.tryEnterBulkhead()).isTrue();

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isZero();
        assertThat(bulkhead.tryEnterBulkhead()).isFalse();

        Thread changerThread = new Thread(() -> {
            bulkhead.changeConfig(BulkheadConfig.custom()
                .maxConcurrentCalls(1)
                .maxWaitDuration(Duration.ofMillis(0))
                .build());
        });
        changerThread.setDaemon(true);
        changerThread.start();

        await().atMost(1, SECONDS)
            .until(() -> changerThread.getState().equals(WAITING));

        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(2);

        bulkheadThreadTrigger.set(false);
        await().atMost(1, SECONDS)
            .until(() -> bulkheadThread.getState().equals(TERMINATED));
        await().atMost(1, SECONDS)
            .until(() -> changerThread.getState().equals(TERMINATED));

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isZero();
        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(1);

        bulkhead.onComplete();
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(1);
    }

    @TestTemplate
    void changePermissionsCountWhileOneThreadIsWaitingForPermission(ThreadType threadType) {
        BulkheadConfig originalConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(1)
            .maxWaitDuration(Duration.ofMillis(500000))
            .build();
        SemaphoreBulkhead bulkhead = new SemaphoreBulkhead("test", originalConfig);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isOne();
        bulkhead.tryAcquirePermission();
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isZero();

        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isOne();
        Thread bulkheadThread = new Thread(() -> {
            bulkhead.tryAcquirePermission();
            bulkhead.onComplete();
        });
        bulkheadThread.setDaemon(true);
        bulkheadThread.start();

        await().atMost(1, SECONDS)
            .until(() -> bulkheadThread.getState().equals(TIMED_WAITING));

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isZero();

        BulkheadConfig newConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(2)
            .maxWaitDuration(Duration.ofMillis(500000))
            .build();

        bulkhead.changeConfig(newConfig);
        await().atMost(1, SECONDS)
            .until(() -> bulkheadThread.getState().equals(TERMINATED));
        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(2);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @TestTemplate
    void changeWaitingTimeWhileOneThreadIsWaitingForPermission(ThreadType threadType) {
        BulkheadConfig originalConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(1)
            .maxWaitDuration(Duration.ofMillis(500000))
            .build();
        SemaphoreBulkhead bulkhead = new SemaphoreBulkhead("test", originalConfig);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isOne();
        bulkhead.tryAcquirePermission();
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isZero();

        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isOne();
        Thread bulkheadThread = new Thread(() -> {
            bulkhead.tryAcquirePermission();
            bulkhead.onComplete();
        });
        bulkheadThread.setDaemon(true);
        bulkheadThread.start();

        await().atMost(1, SECONDS)
            .until(() -> bulkheadThread.getState().equals(TIMED_WAITING));
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isZero();

        BulkheadConfig newConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(1)
            .maxWaitDuration(Duration.ofMillis(0))
            .build();

        bulkhead.changeConfig(newConfig);
        assertThat(bulkhead.tryEnterBulkhead()).isFalse(); // main thread is not blocked

        // previously blocked thread is still waiting
        await().atMost(1, SECONDS)
            .until(() -> bulkheadThread.getState().equals(TIMED_WAITING));
    }

    @SuppressWarnings("Duplicates")
    @TestTemplate
    void changePermissionsConcurrentlyWithDetailedLockTesting(ThreadType threadType)
            throws NoSuchFieldException, IllegalAccessException {
        LOG.info("Running changePermissionsConcurrentlyWithDetailedLockTesting in {}", threadType);

        BulkheadConfig originalConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(3)
            .maxWaitDuration(Duration.ofMillis(0))
            .build();
        SemaphoreBulkhead bulkhead = new SemaphoreBulkhead("parameterizedTest-" + threadType, originalConfig);

        Field field = SemaphoreBulkhead.class.getDeclaredField("lock");
        field.setAccessible(true);
        ReentrantLock lock = (ReentrantLock) field.get(bulkhead);

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(3);

        AtomicBoolean bulkheadThreadTrigger = new AtomicBoolean(true);
        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(3);

        Thread bulkheadThread = new Thread(() -> {
            bulkhead.tryAcquirePermission();
            while (bulkheadThreadTrigger.get()) {
                Thread.yield();
            }
            bulkhead.onComplete();
        });
        bulkheadThread.setDaemon(true);
        bulkheadThread.start();

        await().atMost(1, SECONDS)
            .until(() -> bulkheadThread.getState().equals(RUNNABLE));

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(2);
        assertThat(bulkhead.tryEnterBulkhead()).isTrue();
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);

        Thread firstChangerThread = new Thread(() -> {
            bulkhead.changeConfig(BulkheadConfig.custom()
                .maxConcurrentCalls(1)
                .maxWaitDuration(Duration.ofMillis(0))
                .build());
        });
        firstChangerThread.setDaemon(true);
        firstChangerThread.start();

        await().atMost(1, SECONDS)
            .until(() -> firstChangerThread.getState().equals(WAITING) || lock.isLocked());

        Thread secondChangerThread = new Thread(() -> {
            bulkhead.changeConfig(BulkheadConfig.custom()
                .maxConcurrentCalls(4)
                .maxWaitDuration(Duration.ofMillis(0))
                .build());
        });
        secondChangerThread.setDaemon(true);
        secondChangerThread.start();

        await().atMost(1, SECONDS)
            .until(() -> lock.isLocked() || lock.hasQueuedThreads());

        if (threadType == ThreadType.VIRTUAL) {
            // Virtual threads should handle blocking efficiently without pinning carrier threads
            await().atMost(1, SECONDS)
                .until(() -> lock.hasQueuedThread(secondChangerThread) || lock.getQueueLength() > 0);
        }

        bulkheadThreadTrigger.set(false);
        await().atMost(1, SECONDS)
            .until(() -> bulkheadThread.getState().equals(TERMINATED));
        await().atMost(1, SECONDS)
            .until(() -> firstChangerThread.getState().equals(TERMINATED));
        await().atMost(1, SECONDS)
            .until(() -> secondChangerThread.getState().equals(TERMINATED));

        await().atMost(1, SECONDS)
            .until(() -> !lock.isLocked());

        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls())
            .as("Final config should reflect successful change in %s", threadType)
            .isIn(1, 4);

        LOG.info("Enhanced concurrent permissions test passed in {}", threadType);
    }

    @TestTemplate
    void shouldCreateBulkheadRegistryWithRegistryStore(ThreadType threadType) {
        RegistryEventConsumer<Bulkhead> registryEventConsumer = getNoOpsRegistryEventConsumer();
        List<RegistryEventConsumer<Bulkhead>> registryEventConsumers = new ArrayList<>();
        registryEventConsumers.add(registryEventConsumer);
        Map<String, BulkheadConfig> configs = new HashMap<>();
        final BulkheadConfig defaultConfig = BulkheadConfig.ofDefaults();
        configs.put("default", defaultConfig);
        final InMemoryBulkheadRegistry inMemoryBulkheadRegistry =
            new InMemoryBulkheadRegistry(configs, registryEventConsumers,
                Map.of("Tag1", "Tag1Value"), new InMemoryRegistryStore<>());

        AssertionsForClassTypes.assertThat(inMemoryBulkheadRegistry).isNotNull();
        AssertionsForClassTypes.assertThat(inMemoryBulkheadRegistry.getDefaultConfig()).isEqualTo(defaultConfig);
        AssertionsForClassTypes.assertThat(inMemoryBulkheadRegistry.getConfiguration("testNotFound")).isEmpty();
        inMemoryBulkheadRegistry.addConfiguration("testConfig", defaultConfig);
        AssertionsForClassTypes.assertThat(inMemoryBulkheadRegistry.getConfiguration("testConfig")).isNotNull();
    }

    private RegistryEventConsumer<Bulkhead> getNoOpsRegistryEventConsumer() {
        return new RegistryEventConsumer<Bulkhead>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<Bulkhead> entryAddedEvent) {
            }
            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<Bulkhead> entryRemoveEvent) {
            }
            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<Bulkhead> entryReplacedEvent) {
            }
        };
    }
}
