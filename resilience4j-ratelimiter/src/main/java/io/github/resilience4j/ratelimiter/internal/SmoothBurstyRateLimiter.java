/*
 *
 *  Copyright 2020 Emmanouil Gkatziouras
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

import com.google.common.util.concurrent.SmoothBurstyDecorator;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.event.RateLimiterOnFailureEvent;
import io.github.resilience4j.ratelimiter.event.RateLimiterOnSuccessEvent;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

/**
 * The SmoothBursty Rate Limiter is an implementation utilizing Guava's {@link com.google.common.util.concurrent.SmoothRateLimiter.SmoothBursty}
 */
public class SmoothBurstyRateLimiter implements RateLimiter {

    private static final String NAME_MUST_NOT_BE_NULL = "Name must not be null";
    private static final String CONFIG_MUST_NOT_BE_NULL = "Config must not be null";

    private final String name;
    private final AtomicReference<RateLimiterConfig> rateLimiterConfig;
    private final SmoothBurstyDecorator rateLimiter;
    private final SmoothBurstyRateLimiterMetrics metrics;
    private final Map<String, String> tags;
    private final RateLimiterEventProcessor eventProcessor;

    /**
     * Creates a RateLimiter.
     *
     * @param name              the name of the RateLimiter
     * @param rateLimiterConfig The RateLimiter configuration.
     */
    public SmoothBurstyRateLimiter(String name, RateLimiterConfig rateLimiterConfig) {
        this(name, rateLimiterConfig, HashMap.empty());
    }

    /**
     * Creates a RateLimiter.
     *
     * @param name              the name of the RateLimiter
     * @param rateLimiterConfig The RateLimiter configuration.
     * @param tags              tags to assign to the RateLimiter
     */
    public SmoothBurstyRateLimiter(String name, RateLimiterConfig rateLimiterConfig, Map<String, String> tags) {
        this.name = requireNonNull(name, NAME_MUST_NOT_BE_NULL);
        this.rateLimiterConfig = new AtomicReference<>(
            requireNonNull(rateLimiterConfig, CONFIG_MUST_NOT_BE_NULL));
        this.tags = tags;

        /**
         * Actually this is the calculation of the requests per second
         */
        this.rateLimiter = SmoothBurstyDecorator.create(rateLimiterConfig);
        this.metrics = this.new SmoothBurstyRateLimiterMetrics();

        this.eventProcessor = new RateLimiterEventProcessor();
    }

    @Override
    public void changeTimeoutDuration(Duration timeoutDuration) {
        RateLimiterConfig newConfig = RateLimiterConfig.from(rateLimiterConfig.get())
            .timeoutDuration(timeoutDuration)
            .build();
        rateLimiterConfig.set(newConfig);
    }

    @Override
    public void changeLimitForPeriod(int limitForPeriod) {
        RateLimiterConfig newConfig = RateLimiterConfig.from(rateLimiterConfig.get())
            .limitForPeriod(limitForPeriod)
            .build();
        rateLimiter.updateRate(newConfig);
        rateLimiterConfig.set(newConfig);
    }

    /**
     * Due to the indirect usage of {@link com.google.common.util.concurrent.RateLimiter.SleepingStopwatch.class#sleepMicrosUninterruptibly}
     * there is not going to be an interrupt
     *
     * @param permits number of permits - use for systems where 1 call != 1 permit
     * @return
     */
    @Override
    public boolean acquirePermission(int permits) {
        Duration timeoutDuration = rateLimiterConfig.get().getTimeoutDuration();
        boolean success = rateLimiter.tryAcquire(permits, timeoutDuration);
        publishRateLimiterEvent(success, permits);
        return success;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long reservePermission(int permits) {
        long timeoutInNanos = rateLimiterConfig.get().getTimeoutDuration().toNanos();
        long nanosToWait = rateLimiter.reservePermission(permits);

        boolean canAcquireImmediately = nanosToWait <= 0;

        if (canAcquireImmediately) {
            publishRateLimiterEvent(true, permits);
            return 0;
        }

        boolean canAcquireInTime = timeoutInNanos >= nanosToWait;

        if (canAcquireInTime) {
            publishRateLimiterEvent(true, permits);
            return nanosToWait;
        }

        publishRateLimiterEvent(false, permits);
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Metrics getMetrics() {
        return new SmoothBurstyRateLimiterMetrics();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventPublisher getEventPublisher() {
        return eventProcessor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimiterConfig getRateLimiterConfig() {
        return this.rateLimiterConfig.get();
    }

    @Override
    public String toString() {
        return "SmoothBurstyRateLimiter{"
            + "name='" + name + '\''
            + ", rateLimiterConfig=" + rateLimiterConfig
            + '}';
    }

    @Override
    public Map<String, String> getTags() {
        return tags;
    }

    private void publishRateLimiterEvent(boolean permissionAcquired, int permits) {
        if (!eventProcessor.hasConsumers()) {
            return;
        }
        if (permissionAcquired) {
            eventProcessor.consumeEvent(new RateLimiterOnSuccessEvent(name, permits));
            return;
        }
        eventProcessor.consumeEvent(new RateLimiterOnFailureEvent(name, permits));
    }

    /**
     * {@inheritDoc}
     */
    private final class SmoothBurstyRateLimiterMetrics implements Metrics {

        private SmoothBurstyRateLimiterMetrics() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getNumberOfWaitingThreads() {
            return -1;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getAvailablePermissions() {
            return rateLimiter.getAvailablePermissions();
        }
    }

}
