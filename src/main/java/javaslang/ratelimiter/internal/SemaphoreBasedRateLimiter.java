package javaslang.ratelimiter.internal;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

import javaslang.control.Option;
import javaslang.ratelimiter.RateLimiter;
import javaslang.ratelimiter.RateLimiterConfig;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * A RateLimiter implementation that consists of {@link Semaphore}
 * and scheduler that will refresh permissions
 * after each {@link RateLimiterConfig#limitRefreshPeriod}.
 */
public class SemaphoreBasedRateLimiter implements RateLimiter {

    private static final String NAME_MUST_NOT_BE_NULL = "Name must not be null";
    private static final String CONFIG_MUST_NOT_BE_NULL = "RateLimiterConfig must not be null";

    private final String name;
    private final RateLimiterConfig rateLimiterConfig;
    private final ScheduledExecutorService scheduler;
    private final Semaphore semaphore;
    private final SemaphoreBasedRateLimiterMetrics metrics;

    /**
     * Creates a RateLimiter.
     *
     * @param name              the name of the RateLimiter
     * @param rateLimiterConfig The RateLimiter configuration.
     */
    public SemaphoreBasedRateLimiter(final String name, final RateLimiterConfig rateLimiterConfig) {
        this(name, rateLimiterConfig, null);
    }

    /**
     * Creates a RateLimiter.
     *
     * @param name              the name of the RateLimiter
     * @param rateLimiterConfig The RateLimiter configuration.
     * @param scheduler         executor that will refresh permissions
     */
    public SemaphoreBasedRateLimiter(String name, RateLimiterConfig rateLimiterConfig,
                                     ScheduledExecutorService scheduler) {
        this.name = requireNonNull(name, NAME_MUST_NOT_BE_NULL);
        this.rateLimiterConfig = requireNonNull(rateLimiterConfig, CONFIG_MUST_NOT_BE_NULL);

        this.scheduler = Option.of(scheduler).getOrElse(this::configureScheduler);
        this.semaphore = new Semaphore(this.rateLimiterConfig.getLimitForPeriod(), true);
        this.metrics = this.new SemaphoreBasedRateLimiterMetrics();

        scheduleLimitRefresh();
    }

    private ScheduledExecutorService configureScheduler() {
        ThreadFactory threadFactory = target -> {
            Thread thread = new Thread(target, "SchedulerForSemaphoreBasedRateLimiterImpl-" + name);
            thread.setDaemon(true);
            return thread;
        };
        return newSingleThreadScheduledExecutor(threadFactory);
    }

    private void scheduleLimitRefresh() {
        scheduler.scheduleAtFixedRate(
            this::refreshLimit,
            this.rateLimiterConfig.getLimitRefreshPeriod().toNanos(),
            this.rateLimiterConfig.getLimitRefreshPeriod().toNanos(),
            TimeUnit.NANOSECONDS
        );
    }

    void refreshLimit() {
        semaphore.release(this.rateLimiterConfig.getLimitForPeriod());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getPermission(final Duration timeoutDuration) {
        try {
            boolean success = semaphore.tryAcquire(timeoutDuration.toNanos(), TimeUnit.NANOSECONDS);
            return success;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Metrics getMetrics() {
        return this.metrics;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimiterConfig getRateLimiterConfig() {
        return this.rateLimiterConfig;
    }

    /**
     * Get the enhanced Metrics with some implementation specific details.
     *
     * @return the detailed metrics
     */
    public SemaphoreBasedRateLimiterMetrics getDetailedMetrics() {
        return this.metrics;
    }

    /**
     * Enhanced {@link Metrics} with some implementation specific details
     */
    public final class SemaphoreBasedRateLimiterMetrics implements Metrics {
        private SemaphoreBasedRateLimiterMetrics() {
        }

        /**
         * Returns the current number of permits available in this request limit
         * until the next refresh.
         * <p>
         * <p>This method is typically used for debugging and testing purposes.
         *
         * @return the number of permits available in this rate limiter until the next refresh.
         */
        public int getAvailablePermits() {
            return semaphore.availablePermits();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getNumberOfWaitingThreads() {
            return semaphore.getQueueLength();
        }
    }
}
