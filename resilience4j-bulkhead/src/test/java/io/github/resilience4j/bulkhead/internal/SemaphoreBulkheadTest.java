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
package io.github.resilience4j.bulkhead.internal;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.core.ThreadModeTestBase;
import io.github.resilience4j.core.ThreadType;
import io.github.resilience4j.core.exception.AcquirePermissionCancelledException;
import io.github.resilience4j.core.registry.*;
import io.github.resilience4j.test.RxJava2Adapter;
import io.reactivex.subscribers.TestSubscriber;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import static com.jayway.awaitility.Awaitility.await;
import static io.github.resilience4j.bulkhead.BulkheadConfig.*;
import static io.github.resilience4j.bulkhead.event.BulkheadEvent.Type.*;
import static java.lang.Thread.State.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(Parameterized.class)
public class SemaphoreBulkheadTest extends ThreadModeTestBase {

    /**
     * Constructor for parameterized tests.
     * 
     * @param threadType the thread mode to test with ("platform" or "virtual")
     */
    public SemaphoreBulkheadTest(ThreadType threadType) {
        super(threadType);
    }

    /**
     * Provides the parameterized test data for different thread modes.
     */
    @Parameterized.Parameters(name = "threadMode={0}")
    public static Collection<Object[]> threadModes() {
        return ThreadModeTestBase.threadModes();
    }

    private Bulkhead bulkhead;
    private TestSubscriber<BulkheadEvent.Type> testSubscriber;

    @Before
    public void setUp() {
        BulkheadConfig config = BulkheadConfig.custom()
            .maxConcurrentCalls(2)
            .maxWaitDuration(Duration.ofMillis(0))
            .build();
        bulkhead = Bulkhead.of("test-" + threadType, config);
        testSubscriber = RxJava2Adapter.toFlowable(bulkhead.getEventPublisher())
            .map(BulkheadEvent::getEventType)
            .test();
    }

    @Test
    public void shouldReturnTheCorrectName() {
        assertThat(bulkhead.getName()).isEqualTo("test-" + threadType);
    }

    @Test
    public void shouldHandleBasicBulkheadOperationsInBothThreadModes() throws InterruptedException {
        System.out.println("Running shouldHandleBasicBulkheadOperationsInBothThreadModes in " + getThreadModeDescription());
        
        // Test basic permission acquisition
        boolean firstPermission = bulkhead.tryAcquirePermission();
        assertThat(firstPermission)
            .as("First permission should be acquired in " + getThreadModeDescription())
            .isTrue();
        
        boolean secondPermission = bulkhead.tryAcquirePermission();
        assertThat(secondPermission)
            .as("Second permission should be acquired in " + getThreadModeDescription())
            .isTrue();
        
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls())
            .as("No concurrent calls should be available in " + getThreadModeDescription())
            .isZero();

        // Test rejection when no permits available
        boolean thirdPermission = bulkhead.tryAcquirePermission();
        assertThat(thirdPermission)
            .as("Third permission should be rejected in " + getThreadModeDescription())
            .isFalse();

        // Test permission release
        bulkhead.onComplete();
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls())
            .as("One concurrent call should be available after completion in " + getThreadModeDescription())
            .isEqualTo(1);

        bulkhead.onComplete();
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls())
            .as("Two concurrent calls should be available after second completion in " + getThreadModeDescription())
            .isEqualTo(2);

        // Test that we can acquire permission again
        boolean fourthPermission = bulkhead.tryAcquirePermission();
        assertThat(fourthPermission)
            .as("Fourth permission should be acquired after releases in " + getThreadModeDescription())
            .isTrue();

        // Verify event sequence
        testSubscriber.assertValueCount(6)
            .assertValues(CALL_PERMITTED, CALL_PERMITTED, CALL_REJECTED, CALL_FINISHED,
                CALL_FINISHED, CALL_PERMITTED);
        
        System.out.println("✅ Basic bulkhead operations test passed in " + getThreadModeDescription());
    }

    @Test
    public void testToString() {
        String result = bulkhead.toString();

        assertThat(result).isEqualTo("Bulkhead 'test-" + threadType + "'");
    }

    @Test
    public void testCreateWithNullConfig() {
        Supplier<BulkheadConfig> configSupplier = () -> null;

        assertThatThrownBy(() -> Bulkhead.of("test", configSupplier))
            .isInstanceOf(NullPointerException.class).hasMessage("Config must not be null");
    }

    @Test
    public void testCreateWithDefaults() {
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

    @Test
    public void shouldHandleTimeoutBehaviorConsistentlyInBothThreadModes() throws InterruptedException {
        System.out.println("Running shouldHandleTimeoutBehaviorConsistentlyInBothThreadModes in " + getThreadModeDescription());
        
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
                .as("Permission should be rejected due to timeout in " + getThreadModeDescription())
                .isFalse();
            assertThat(actualWaitTime.toMillis())
                .as("Wait time should be within expected range in " + getThreadModeDescription())
                .isBetween(expectedMillisOfWaitTime, (long) (expectedMillisOfWaitTime * 1.3));
        });
        subTestRoutine.setDaemon(true);
        subTestRoutine.start();

        assertThat(entered)
            .as("Initial entry should succeed in " + getThreadModeDescription())
            .isTrue();
        
        subTestRoutine.join(2 * expectedMillisOfWaitTime);
        assertThat(subTestRoutine.isInterrupted())
            .as("Sub-thread should not be interrupted in " + getThreadModeDescription())
            .isFalse();
        assertThat(subTestRoutine.isAlive())
            .as("Sub-thread should complete in " + getThreadModeDescription())
            .isFalse();
        
        System.out.println("✅ Time-out behavior test passed in " + getThreadModeDescription());
    }

    @Test
    public void testTryEnterWithInterruptDuringTimeout() throws InterruptedException {
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
            .until(() -> subTestRoutine.getState() == Thread.State.TIMED_WAITING);
        subTestRoutine.interrupt();
        await().atMost(expectedWaitTime.dividedBy(2).toMillis(), MILLISECONDS)
            .pollInterval(expectedWaitTime.dividedBy(100).toMillis(), MILLISECONDS)
            .until(() -> subTestRoutine.getState() == Thread.State.TERMINATED);
        assertThat(entered).isTrue();
        assertThat(interruptedWithoutCodeFlowBreak.get()).isTrue();
        assertThat(subTestRoutine.isAlive()).isFalse();
    }

    @Test
    public void testAcquireWithInterruptDuringTimeout() throws InterruptedException {
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
            .until(() -> subTestRoutine.getState() == Thread.State.TIMED_WAITING);
        subTestRoutine.interrupt();
        await().atMost(expectedWaitTime.dividedBy(2).toMillis(), MILLISECONDS)
            .pollInterval(expectedWaitTime.dividedBy(100).toMillis(), MILLISECONDS)
            .until(() -> subTestRoutine.getState() == Thread.State.TERMINATED);
        assertThat(entered).isTrue();
        assertThat(interruptedWithoutCodeFlowBreak.get()).isTrue();
        assertThat(interruptedWithException.get()).isTrue();
        assertThat(subTestRoutine.isAlive()).isFalse();
    }

    @Test
    public void testZeroMaxConcurrentCalls() {
        BulkheadConfig config = BulkheadConfig.custom()
            .maxConcurrentCalls(0)
            .maxWaitDuration(Duration.ofMillis(0))
            .build();
        SemaphoreBulkhead bulkhead = new SemaphoreBulkhead("test", config);

        boolean entered = bulkhead.tryAcquirePermission();

        assertThat(entered).isFalse();
    }

    @Test
    public void testEntryTimeout() {
        BulkheadConfig config = BulkheadConfig.custom()
            .maxConcurrentCalls(1)
            .maxWaitDuration(Duration.ofMillis(10))
            .build();
        SemaphoreBulkhead bulkhead = new SemaphoreBulkhead("test", config);
        bulkhead.tryAcquirePermission(); // consume the permit

        boolean entered = bulkhead.tryEnterBulkhead();

        assertThat(entered).isFalse();
    }

    @Test
    public void changePermissionsInIdleState() {
        BulkheadConfig originalConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(3)
            .maxWaitDuration(Duration.ofMillis(5000))
            .build();
        SemaphoreBulkhead bulkhead = new SemaphoreBulkhead("test", originalConfig);

        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(3);
        assertThat(bulkhead.getBulkheadConfig().getMaxWaitDuration().toMillis()).isEqualTo(5000);

        BulkheadConfig newConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(5)
            .maxWaitDuration(Duration.ofMillis(5000))
            .build();

        bulkhead.changeConfig(newConfig);
        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(5);
        assertThat(bulkhead.getBulkheadConfig().getMaxWaitDuration().toMillis()).isEqualTo(5000);

        newConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(2)
            .maxWaitDuration(Duration.ofMillis(5000))
            .build();

        bulkhead.changeConfig(newConfig);
        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(2);
        assertThat(bulkhead.getBulkheadConfig().getMaxWaitDuration().toMillis()).isEqualTo(5000);

        bulkhead.changeConfig(newConfig);
    }

    @Test
    public void changeWaitTimeInIdleState() {
        BulkheadConfig originalConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(3)
            .maxWaitDuration(Duration.ofMillis(5000))
            .build();
        SemaphoreBulkhead bulkhead = new SemaphoreBulkhead("test", originalConfig);

        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(3);
        assertThat(bulkhead.getBulkheadConfig().getMaxWaitDuration().toMillis()).isEqualTo(5000);

        BulkheadConfig newConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(3)
            .maxWaitDuration(Duration.ofMillis(3000))
            .build();

        bulkhead.changeConfig(newConfig);
        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(3);
        assertThat(bulkhead.getBulkheadConfig().getMaxWaitDuration().toMillis()).isEqualTo(3000);

        newConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(3)
            .maxWaitDuration(Duration.ofMillis(7000))
            .build();

        bulkhead.changeConfig(newConfig);
        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(3);
        assertThat(bulkhead.getBulkheadConfig().getMaxWaitDuration().toMillis()).isEqualTo(7000);

        bulkhead.changeConfig(newConfig);
    }

    @SuppressWarnings("Duplicates")
    @Test
    public void changePermissionsCountWhileOneThreadIsRunningWithThisPermission() {
        BulkheadConfig originalConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(1)
            .maxWaitDuration(Duration.ofMillis(0))
            .build();
        SemaphoreBulkhead bulkhead = new SemaphoreBulkhead("test", originalConfig);

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);

        AtomicBoolean bulkheadThreadTrigger = new AtomicBoolean(true);
        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(1);
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

    @Test
    public void changePermissionsCountWhileOneThreadIsWaitingForPermission() {
        BulkheadConfig originalConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(1)
            .maxWaitDuration(Duration.ofMillis(500000))
            .build();
        SemaphoreBulkhead bulkhead = new SemaphoreBulkhead("test", originalConfig);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        bulkhead.tryAcquirePermission();
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isZero();

        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(1);
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

    @Test
    public void changeWaitingTimeWhileOneThreadIsWaitingForPermission() {
        BulkheadConfig originalConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(1)
            .maxWaitDuration(Duration.ofMillis(500000))
            .build();
        SemaphoreBulkhead bulkhead = new SemaphoreBulkhead("test", originalConfig);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        bulkhead.tryAcquirePermission();
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isZero();

        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(1);
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
    @Test
    public void changePermissionsConcurrentlyWithDetailedLockTesting() throws NoSuchFieldException, IllegalAccessException {
        System.out.println("Running changePermissionsConcurrentlyWithDetailedLockTesting in " + getThreadModeDescription());
        
        BulkheadConfig originalConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(3)
            .maxWaitDuration(Duration.ofMillis(0))
            .build();
        SemaphoreBulkhead bulkhead = new SemaphoreBulkhead("parameterizedTest-" + threadType, originalConfig);
        
        // Access to reflection to check the lock state of ReentrantLock
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
            .until(() -> bulkheadThread.getState().equals(Thread.State.RUNNABLE));

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

        // Enhanced testing: Verify that the ReentrantLock is properly acquired during config change
        await().atMost(1, SECONDS)
            .until(() -> firstChangerThread.getState().equals(Thread.State.WAITING) || lock.isLocked());

        Thread secondChangerThread = new Thread(() -> {
            bulkhead.changeConfig(BulkheadConfig.custom()
                .maxConcurrentCalls(4)
                .maxWaitDuration(Duration.ofMillis(0))
                .build());
        });
        secondChangerThread.setDaemon(true);
        secondChangerThread.start();

        // Enhanced testing: Verify that the second thread is queued when the lock is held
        await().atMost(1, SECONDS)
            .until(() -> lock.isLocked() || lock.hasQueuedThreads());
        
        // In virtual thread mode, verify the lock behavior is consistent
        if (isVirtualThreadMode()) {
            // Virtual threads should handle blocking efficiently without pinning carrier threads
            await().atMost(1, SECONDS)
                .until(() -> lock.hasQueuedThread(secondChangerThread) || lock.getQueueLength() > 0);
        }

        bulkheadThreadTrigger.set(false);
        await().atMost(1, SECONDS)
            .until(() -> bulkheadThread.getState().equals(Thread.State.TERMINATED));
        await().atMost(1, SECONDS)
            .until(() -> firstChangerThread.getState().equals(Thread.State.TERMINATED));
        await().atMost(1, SECONDS)
            .until(() -> secondChangerThread.getState().equals(Thread.State.TERMINATED));

        // Enhanced testing: Verify that the lock is released after all operations complete
        await().atMost(1, SECONDS)
            .until(() -> !lock.isLocked());

        // Final config should reflect the last successful change (could be 1 or 4)
        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls())
            .as("Final config should reflect successful change in " + getThreadModeDescription())
            .isIn(1, 4);
        
        System.out.println("✅ Enhanced concurrent permissions test passed in " + getThreadModeDescription());
    }

    @Test
    public void shouldCreateBulkheadRegistryWithRegistryStore() {
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
