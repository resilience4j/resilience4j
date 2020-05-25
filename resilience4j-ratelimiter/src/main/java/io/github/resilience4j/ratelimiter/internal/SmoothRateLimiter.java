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

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.System.nanoTime;

/**
 * {@link SmoothRateLimiter} splits all nanoseconds from the start of epoch into cycles.
 * <p>Each cycle has duration of {@link RateLimiterConfig#limitRefreshPeriod} in nanoseconds.
 * <p>By contract on start of each cycle {@link SmoothRateLimiter} should
 * set {@link SmoothRateLimiter.State#activePermissions} to {@link RateLimiterConfig#limitForPeriod}. For the {@link
 * SmoothRateLimiter} callers it is really looks so, but under the hood there is some optimisations
 * that will skip this refresh if {@link SmoothRateLimiter} is not used actively.
 * <p>All {@link SmoothRateLimiter} updates are atomic and state is encapsulated in {@link
 * AtomicReference} to {@link SmoothRateLimiter.State}
 */
public class SmoothRateLimiter implements RateLimiter {

    private static final long nanoTimeStart = nanoTime();

    private final String name;
    private final AtomicInteger waitingThreads;
    private final AtomicReference<State> state;
    private final Map<String, String> tags;
    private final RateLimiterEventProcessor eventProcessor;

    public SmoothRateLimiter(String name, RateLimiterConfig rateLimiterConfig) {
        this(name, rateLimiterConfig, HashMap.empty());
    }

    public SmoothRateLimiter(String name, RateLimiterConfig rateLimiterConfig,
                             Map<String,String> tags) {
        this.name = name;
        this.tags = tags;

        waitingThreads = new AtomicInteger(0);
        state = new AtomicReference<>(new State(
            rateLimiterConfig, 0, rateLimiterConfig.getLimitForPeriod(), 0
        ));
        eventProcessor = new RateLimiterEventProcessor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changeTimeoutDuration(Duration timeoutDuration) {
        RateLimiterConfig newConfig = RateLimiterConfig.from(state.get().config)
            .timeoutDuration(timeoutDuration)
            .build();
        state.updateAndGet(currentState -> new State(newConfig, currentState.activeCycle, currentState.activePermissions,
            currentState.nanosToWait));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changeLimitForPeriod(int limitForPeriod) {
        RateLimiterConfig newConfig = RateLimiterConfig.from(state.get().config)
            .limitForPeriod(limitForPeriod)
            .build();
        state.updateAndGet(currentState -> new State(
            newConfig, currentState.activeCycle, currentState.activePermissions,
            currentState.nanosToWait
        ));
    }

    /**
     * Calculates time elapsed from the class loading.
     */
    private long currentNanoTime() {
        return nanoTime() - nanoTimeStart;
    }

    @Override
    public boolean acquirePermission(int permits) {
        return false;
    }

    @Override
    public long reservePermission(int permits) {
        return 0;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RateLimiterConfig getRateLimiterConfig() {
        return null;
    }

    @Override
    public Map<String, String> getTags() {
        return tags;
    }

    @Override
    public Metrics getMetrics() {
        return null;
    }

    @Override
    public EventPublisher getEventPublisher() {
        return eventProcessor;
    }

    /**
     * <p>{@link SmoothRateLimiter.State} represents immutable state of {@link SmoothRateLimiter}
     * where:
     * <ul>
     * <li>activeCycle - {@link SmoothRateLimiter} cycle number that was used
     * by the last {@link SmoothRateLimiter#acquirePermission()} call.</li>
     * <p>
     * <li>activePermissions - count of available permissions after
     * the last {@link SmoothRateLimiter#acquirePermission()} call.
     * Can be negative if some permissions where reserved.</li>
     * <p>
     * <li>nanosToWait - count of nanoseconds to wait for permission for
     * the last {@link SmoothRateLimiter#acquirePermission()} call.</li>
     * </ul>
     */
    private static class State {

        private final RateLimiterConfig config;

        private final long activeCycle;
        private final int activePermissions;
        private final long nanosToWait;

        private State(RateLimiterConfig config,
                      final long activeCycle, final int activePermissions, final long nanosToWait) {
            this.config = config;
            this.activeCycle = activeCycle;
            this.activePermissions = activePermissions;
            this.nanosToWait = nanosToWait;
        }

    }
}
