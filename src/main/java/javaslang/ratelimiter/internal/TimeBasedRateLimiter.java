package javaslang.ratelimiter.internal;

import static java.lang.System.nanoTime;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.locks.LockSupport.parkNanos;

import javaslang.ratelimiter.RateLimiter;
import javaslang.ratelimiter.RateLimiterConfig;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * @author bstorozhuk
 */
public class TimeBasedRateLimiter implements RateLimiter {

    private final String name;
    private final RateLimiterConfig rateLimiterConfig;
    private final long cyclePeriodInNanos;
    private final int permissionsPerCycle;
    private final ReentrantLock lock;
    private final AtomicInteger waitingThreads;
    private long activeCycle;
    private volatile int activePermissions;

    public TimeBasedRateLimiter(String name, RateLimiterConfig rateLimiterConfig) {
        this.name = name;
        this.rateLimiterConfig = rateLimiterConfig;

        cyclePeriodInNanos = rateLimiterConfig.getLimitRefreshPeriod().toNanos();
        permissionsPerCycle = rateLimiterConfig.getLimitForPeriod();

        activeCycle = nanoTime() / cyclePeriodInNanos;
        waitingThreads = new AtomicInteger(0);
        lock = new ReentrantLock(false);

        activePermissions = permissionsPerCycle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getPermission(final Duration timeoutDuration) {
        Supplier<Boolean> permissionSupplier = () -> {
            long currentNanos = nanoTime();
            long currentCycle = currentNanos / cyclePeriodInNanos;
//            System.out.println(MessageFormat.format(
//                "Thread {0}: START activeCycle={1}; permissions={2}; currentNanos={3}; currentCycle={4};",
//                currentThread().getId(), activeCycle, activePermissions, currentNanos, currentCycle)
//            );
            if (activeCycle != currentCycle) {
                refreshLimiterState(currentCycle);
            }
            return acquirePermission(currentNanos, timeoutDuration);
        };
        return executeConcurrently(permissionSupplier);
    }

    private void refreshLimiterState(final long currentCycle) {
        assert lock.isHeldByCurrentThread();
        activeCycle = currentCycle;
        activePermissions = Integer.min(activePermissions + permissionsPerCycle, permissionsPerCycle);
//        System.out.println(MessageFormat.format(
//            "Thread {0}: AFTER REFRESH activeCycle={1}; permissions={2}; currentCycle={3};",
//            currentThread().getId(), activeCycle, activePermissions, currentCycle)
//        );
    }

    private boolean acquirePermission(final long currentNanos, final Duration timeoutDuration) {
        assert lock.isHeldByCurrentThread();
        long currentCycle = currentNanos / cyclePeriodInNanos;
        long timeoutInNanos = timeoutDuration.toNanos();
        long nanosToWait = nanosToWaitForPermission(currentNanos, currentCycle);
        if (timeoutInNanos < nanosToWait) {
            waitForPermission(timeoutInNanos);
            return false;
        }
        activePermissions--;
        if (nanosToWait <= 0) {
//            System.out.println(MessageFormat.format(
//                "Thread {0}: ACQUIRE IMMEDIATELY activeCycle={1}; permissions={2}; currentCycle={3}; nanosToWait={4};",
//                currentThread().getId(), activeCycle, activePermissions, currentCycle, nanosToWait)
//            );
            return true;
        }
        lock.unlock();
        return waitForPermission(nanosToWait);
    }

    private long nanosToWaitForPermission(final long currentNanos, final long currentCycle) {
        if (activePermissions > 0) {
            return 0L;
        }
        long nextCycleTimeInNanos = (currentCycle + 1) * cyclePeriodInNanos;
        long nanosToNextCycle = nextCycleTimeInNanos - currentNanos;
        int fullCyclesToWait = (-activePermissions) / permissionsPerCycle;
        return (fullCyclesToWait * cyclePeriodInNanos) + nanosToNextCycle;
    }

    private boolean waitForPermission(final long nanosToWait) {
//        System.out.println(MessageFormat.format(
//            "Thread {0}: WAIT activeCycle={1}; permissions={2}; nanosToWait={3};",
//            currentThread().getId(), activeCycle, activePermissions, nanosToWait)
//        );
        waitingThreads.incrementAndGet();
        long deadline = nanoTime() + nanosToWait;
        while (nanoTime() < deadline || currentThread().isInterrupted()) {
            long sleepBlockDuration = deadline - nanoTime();
            parkNanos(sleepBlockDuration);
        }
        waitingThreads.decrementAndGet();
        return !currentThread().isInterrupted();
    }

    private <T> T executeConcurrently(final Supplier<T> permissionSupplier) {
        lock.lock();
        try {
            return permissionSupplier.get();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RateLimiterConfig getRateLimiterConfig() {
        return rateLimiterConfig;
    }

    @Override
    public Metrics getMetrics() {
        return null;
    }

    /**
     * Enhanced {@link Metrics} with some implementation specific details
     */
    public final class TimeBasedRateLimiterMetrics implements Metrics {
        private TimeBasedRateLimiterMetrics() {
        }

        /**
         * {@inheritDoc}
         *
         * @return
         */
        @Override
        public int getNumberOfWaitingThreads() {
            return waitingThreads.get();
        }

        /**
         * Returns the estimated time in nanos to wait for permission.
         * <p>
         * <p>This method is typically used for debugging and testing purposes.
         *
         * @return the estimated time in nanos to wait for permission.
         */
        public long nanosToWait() {
            long currentNanos = nanoTime();
            long currentCycle = currentNanos / cyclePeriodInNanos;
            if (currentCycle == activeCycle) {
                return 0;
            }
            return nanosToWaitForPermission(currentNanos, currentCycle);
        }
    }
}
