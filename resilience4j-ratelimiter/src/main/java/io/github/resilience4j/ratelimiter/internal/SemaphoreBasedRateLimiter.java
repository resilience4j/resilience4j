/*
 *
 *  Copyright 2016 Robert Winkler and Bohdan Storozhuk
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
package io.github.resilience4j.ratelimiter.internal;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import io.github.resilience4j.ratelimiter.event.RateLimiterOnFailureEvent;
import io.github.resilience4j.ratelimiter.event.RateLimiterOnSuccessEvent;
import io.reactivex.Flowable;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import io.vavr.Lazy;
import io.vavr.control.Option;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

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
    private final FlowableProcessor<RateLimiterEvent> eventPublisher;
    private final Lazy<EventConsumer> lazyEventConsumer;

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

        PublishProcessor<RateLimiterEvent> publisher = PublishProcessor.create();
        this.eventPublisher = publisher.toSerialized();

        this.lazyEventConsumer = Lazy.of(() -> new EventDispatcher(getEventStream()));

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
        int permissionsToRelease = this.rateLimiterConfig.getLimitForPeriod() - semaphore.availablePermits();
        semaphore.release(permissionsToRelease);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getPermission(final Duration timeoutDuration) {
        try {
            boolean success = semaphore.tryAcquire(timeoutDuration.toNanos(), TimeUnit.NANOSECONDS);
            publishRateLimiterEvent(success);
            return success;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            publishRateLimiterEvent(false);
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
    public Flowable<RateLimiterEvent> getEventStream() {
        return eventPublisher;
    }

    @Override
    public EventConsumer getEventConsumer() {
        return lazyEventConsumer.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimiterConfig getRateLimiterConfig() {
        return this.rateLimiterConfig;
    }

    @Override public String toString() {
        return "SemaphoreBasedRateLimiter{" +
            "name='" + name + '\'' +
            ", rateLimiterConfig=" + rateLimiterConfig +
            '}';
    }

    /**
     * {@inheritDoc}
     */
    private final class SemaphoreBasedRateLimiterMetrics implements Metrics {
        private SemaphoreBasedRateLimiterMetrics() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getAvailablePermissions() {
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

    private void publishRateLimiterEvent(boolean permissionAcquired) {
        if (!eventPublisher.hasSubscribers()) {
            return;
        }
        if (permissionAcquired) {
            eventPublisher.onNext(new RateLimiterOnSuccessEvent(name));
            return;
        }
        eventPublisher.onNext(new RateLimiterOnFailureEvent(name));
    }
}
