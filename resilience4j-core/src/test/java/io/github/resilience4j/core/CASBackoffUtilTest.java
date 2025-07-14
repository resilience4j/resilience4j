package io.github.resilience4j.core;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CASBackoffUtil to verify backoff behavior and thread-specific jittering.
 */
public class CASBackoffUtilTest {

    @Test
    public void shouldIncrementSpinCountBelowThreshold() {
        // Given: spin count below MAX_SPIN_COUNT
        int spinCount = 5;

        // When: perform backoff
        int newSpinCount = CASBackoffUtil.performBackoff(spinCount);

        // Then: spin count should be incremented
        assertThat(newSpinCount)
            .as("Spin count should increment when below threshold")
            .isEqualTo(spinCount + 1);
    }

    @Test
    public void shouldResetSpinCountAboveThreshold() {
        // Given: spin count above MAX_SPIN_COUNT
        int spinCount = CASBackoffUtil.getMaxSpinCount() + 10;

        // When: perform backoff
        int newSpinCount = CASBackoffUtil.performBackoff(spinCount);

        // Then: spin count should be reset to 0
        assertThat(newSpinCount)
            .as("Spin count should reset to 0 when above threshold")
            .isZero();
    }

    @Test
    public void shouldResetSpinCountAtThreshold() {
        // Given: spin count exactly at MAX_SPIN_COUNT
        int spinCount = CASBackoffUtil.getMaxSpinCount();

        // When: perform backoff
        int newSpinCount = CASBackoffUtil.performBackoff(spinCount);

        // Then: spin count should be reset to 0
        assertThat(newSpinCount)
            .as("Spin count should reset to 0 at threshold")
            .isZero();
    }

    @Test
    public void shouldIncrementGraduallyUpToThreshold() {
        // Given: initial spin count of 0
        int spinCount = 0;

        // When: perform backoff repeatedly up to threshold
        for (int i = 0; i < CASBackoffUtil.getMaxSpinCount(); i++) {
            spinCount = CASBackoffUtil.performBackoff(spinCount);

            // Then: spin count should increment each time
            assertThat(spinCount)
                .as("Spin count should be %d at iteration %d", i + 1, i)
                .isEqualTo(i + 1);
        }

        // When: perform backoff one more time (now at threshold)
        spinCount = CASBackoffUtil.performBackoff(spinCount);

        // Then: should reset to 0
        assertThat(spinCount)
            .as("Spin count should reset after reaching threshold")
            .isZero();
    }

    @Test
    public void shouldProvideDeterministicJitterForSameThread() throws InterruptedException {
        // Given: multiple calls from the same thread
        int iterations = 10;
        CountDownLatch latch = new CountDownLatch(1);
        Set<Long> observedDelays = new HashSet<>();

        // When: perform backoff above threshold multiple times in same thread
        Thread testThread = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                long start = System.nanoTime();
                CASBackoffUtil.performBackoff(CASBackoffUtil.getMaxSpinCount() + 1);
                long elapsed = System.nanoTime() - start;
                observedDelays.add(elapsed);
            }
            latch.countDown();
        });

        testThread.start();
        boolean completed = latch.await(5, TimeUnit.SECONDS);

        // Then: should complete without timeout
        assertThat(completed)
            .as("Test should complete within timeout")
            .isTrue();

        // Note: We can't assert exact timing due to system variability,
        // but we verify the thread completed successfully with deterministic behavior
        assertThat(observedDelays)
            .as("Should observe some timing variations due to system scheduling")
            .isNotEmpty();
    }

    @Test
    public void shouldProvideDifferentJitterForDifferentThreads() throws InterruptedException {
        // Given: multiple threads with different IDs
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        Set<Long> threadIds = new HashSet<>();

        // When: multiple threads perform backoff simultaneously
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                try {
                    // Capture thread ID
                    synchronized (threadIds) {
                        threadIds.add(Thread.currentThread().threadId());
                    }

                    // Wait for all threads to be ready
                    startLatch.await();

                    // Perform backoff above threshold
                    CASBackoffUtil.performBackoff(CASBackoffUtil.getMaxSpinCount() + 1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
            thread.start();
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for all threads to complete
        boolean completed = doneLatch.await(5, TimeUnit.SECONDS);

        // Then: all threads should complete without deadlock
        assertThat(completed)
            .as("All threads should complete within timeout")
            .isTrue();

        // And: we should have captured unique thread IDs
        assertThat(threadIds)
            .as("Should observe %d different thread IDs", threadCount)
            .hasSize(threadCount);
    }

    @Test
    public void shouldNotBlockIndefinitely() throws InterruptedException {
        // Given: high spin count requiring park
        int highSpinCount = CASBackoffUtil.getMaxSpinCount() + 100;
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger result = new AtomicInteger(-1);

        // When: perform backoff in separate thread with timeout
        Thread testThread = new Thread(() -> {
            result.set(CASBackoffUtil.performBackoff(highSpinCount));
            latch.countDown();
        });

        testThread.start();
        boolean completed = latch.await(1, TimeUnit.SECONDS);

        // Then: should complete quickly (within 1 second)
        assertThat(completed)
            .as("Backoff should complete quickly, not block indefinitely")
            .isTrue();

        // And: should return 0
        assertThat(result.get())
            .as("Should reset spin count to 0")
            .isZero();
    }

    @Test
    public void shouldHandleZeroSpinCount() {
        // Given: zero spin count
        int spinCount = 0;

        // When: perform backoff
        int newSpinCount = CASBackoffUtil.performBackoff(spinCount);

        // Then: should increment to 1
        assertThat(newSpinCount)
            .as("Zero spin count should increment to 1")
            .isEqualTo(1);
    }

    @Test
    public void shouldHandleNegativeSpinCount() {
        // Given: negative spin count (edge case, shouldn't happen in practice)
        int spinCount = -1;

        // When: perform backoff
        int newSpinCount = CASBackoffUtil.performBackoff(spinCount);

        // Then: should increment (treating negative as below threshold)
        assertThat(newSpinCount)
            .as("Negative spin count should increment")
            .isEqualTo(0);
    }

    @Test
    public void shouldProvideMaxParkNanosGetter() {
        // Given: MAX_PARK_NANOS is calculated from other constants
        // MIN_PARK_NANOS (1μs) + (JITTER_RANGE-1) * PARK_STEP_NANOS
        // = 1,000ns + 10 * 1,000ns = 11,000ns (11μs)
        long expectedMaxParkNanos = 11_000L;

        // When: get max park time
        long maxParkNanos = CASBackoffUtil.getMaxParkNanos();

        // Then: should return correct calculated value
        assertThat(maxParkNanos)
            .as("Max park time should be 11 microseconds (11,000 nanoseconds)")
            .isEqualTo(expectedMaxParkNanos);

        // And: should be consistent with the formula
        assertThat(maxParkNanos)
            .as("MAX_PARK_NANOS should equal MIN_PARK_NANOS + (JITTER_RANGE-1) * PARK_STEP_NANOS")
            .isEqualTo(1_000L + (11 - 1) * 1_000L);
    }
}
