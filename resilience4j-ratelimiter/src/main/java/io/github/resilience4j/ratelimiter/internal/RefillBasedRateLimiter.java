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
import io.github.resilience4j.ratelimiter.event.RateLimiterOnFailureEvent;
import io.github.resilience4j.ratelimiter.event.RateLimiterOnSuccessEvent;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import static java.lang.Long.min;
import static java.lang.System.nanoTime;
import static java.lang.System.setOut;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.locks.LockSupport.parkNanos;

/**
 * {@link RefillBasedRateLimiter} has a max capacity of permits refilled periodically.
 * <p>Each period has duration of {@link RateLimiterConfig#limitRefreshPeriod} in nanoseconds.
 * <p>By contract the initial {@link RefillBasedRateLimiter.State#activePermissions} are set
 * to be the same with{@link RateLimiterConfig#burstLimit}
 * <p>A ratio of permit per nanoseconds is calculated using
 * {@link RateLimiterConfig#limitRefreshPeriod} and {@link RateLimiterConfig#limitForPeriod}.
 * On a permit request the permits that should have been replenished are calculated based on the nanos passed and the ration of nanos per permit.
 * For the {@link RefillBasedRateLimiter} callers it is looks like a token bucket supporting bursts and a gradual refill,
 * under the hood there is some optimisations that will skip this refresh if {@link RefillBasedRateLimiter} is not used actively.
 * <p>All {@link RefillBasedRateLimiter} updates are atomic and state is encapsulated in {@link
 * AtomicReference} to {@link RefillBasedRateLimiter.State}
 */
public class RefillBasedRateLimiter implements RateLimiter {

    private static final long nanoTimeStart = nanoTime();

    private final String name;
    private final AtomicInteger waitingThreads;
    private final AtomicReference<State> state;
    private final Map<String, String> tags;
    private final RateLimiterEventProcessor eventProcessor;

    public RefillBasedRateLimiter(String name, RateLimiterConfig rateLimiterConfig) {
        this(name, rateLimiterConfig, HashMap.empty());
    }

    public RefillBasedRateLimiter(String name, RateLimiterConfig rateLimiterConfig,
                             Map<String, String> tags) {
        this.name = name;
        this.tags = tags;

        waitingThreads = new AtomicInteger(0);
        state = new AtomicReference<>(new State(
            rateLimiterConfig,  rateLimiterConfig.getInitialPermits(), 0, currentNanoTime()
        ));
        eventProcessor = new RateLimiterEventProcessor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changeTimeoutDuration(final Duration timeoutDuration) {
        RateLimiterConfig newConfig = RateLimiterConfig.from(state.get().config)
            .timeoutDuration(timeoutDuration)
            .build();
        state.updateAndGet(currentState -> new State(
            newConfig, currentState.activePermissions, currentState.nanosToWait,
            currentNanoTime()
        ));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changeLimitForPeriod(final int limitForPeriod) {
        RateLimiterConfig newConfig = RateLimiterConfig.from(state.get().config)
            .limitForPeriod(limitForPeriod)
            .build();
        state.updateAndGet(currentState -> new State(
            newConfig, currentState.activePermissions,
            currentState.nanosToWait, currentNanoTime()
        ));
    }

    /**
     * Calculates time elapsed from the class loading.
     */
    private long currentNanoTime() {
        return nanoTime() - nanoTimeStart;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean acquirePermission(int permits) {
        long timeoutInNanos = state.get().config.getTimeoutDuration().toNanos();
        State modifiedState = updateStateWithBackOff(permits, timeoutInNanos);
        boolean result = waitForPermissionIfNecessary(timeoutInNanos, modifiedState.nanosToWait);
        publishRateLimiterEvent(result, permits);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long reservePermission(final int permits) {
        long timeoutInNanos = state.get().config.getTimeoutDuration().toNanos();
        State modifiedState = updateStateWithBackOff(permits, timeoutInNanos);

        boolean canAcquireImmediately = modifiedState.nanosToWait <= 0;
        if (canAcquireImmediately) {
            publishRateLimiterEvent(true, permits);
            return 0;
        }

        boolean canAcquireInTime = timeoutInNanos >= modifiedState.nanosToWait;
        if (canAcquireInTime) {
            publishRateLimiterEvent(true, permits);
            return modifiedState.nanosToWait;
        }

        publishRateLimiterEvent(false, permits);
        return -1;
    }

    /**
     * Atomically updates the current {@link RefillBasedRateLimiter.State} with the results of applying the {@link
     * RefillBasedRateLimiter#calculateNextState}, returning the updated {@link RefillBasedRateLimiter.State}. It differs from
     * {@link AtomicReference#updateAndGet(UnaryOperator)} by constant back off. It means that after
     * one try to {@link AtomicReference#compareAndSet(Object, Object)} this method will wait for a
     * while before try one more time. This technique was originally described in this
     * <a href="https://arxiv.org/abs/1305.5800"> paper</a>
     * and showed great results with {@link RefillBasedRateLimiter} in benchmark tests.
     *
     * @param timeoutInNanos a side-effect-free function
     * @return the updated value
     */
    private State updateStateWithBackOff(final int permits, final long timeoutInNanos) {
        State prev;
        State next;
        do {
            prev = state.get();
            next = calculateNextState(permits, timeoutInNanos, prev);
        } while (!compareAndSet(prev, next));
        return next;
    }

    /**
     * Atomically sets the value to the given updated value if the current value {@code ==} the
     * expected value. It differs from {@link AtomicReference#updateAndGet(UnaryOperator)} by
     * constant back off. It means that after one try to {@link AtomicReference#compareAndSet(Object,
     * Object)} this method will wait for a while before try one more time. This technique was
     * originally described in this
     * <a href="https://arxiv.org/abs/1305.5800"> paper</a>
     * and showed great results with {@link AtomicRateLimiter} in benchmark tests.
     *
     * @param current the expected value
     * @param next    the new value
     * @return {@code true} if successful. False return indicates that the actual value was not
     * equal to the expected value.
     */
    private boolean compareAndSet(final State current, final State next) {
        if (state.compareAndSet(current, next)) {
            return true;
        }
        parkNanos(1); // back-off
        return false;
    }


    /**
     * A side-effect-free function that can calculate next {@link State} from current. It determines
     * time duration that you should wait for the given number of permits and reserves it for you,
     * if you'll be able to wait long enough.
     *
     * @param permits        number of permits
     * @param timeoutInNanos max time that caller can wait for permission in nanoseconds
     * @param activeState    current state of {@link RefillBasedRateLimiter}
     * @return next {@link State}
     */
    private State calculateNextState(final int permits, final long timeoutInNanos,
                                     final State activeState) {
        /**
         * If you ask more than the burst get an exception
         */
        long permissionsPeriodInNanos = activeState.config.getLimitRefreshPeriod().toNanos();
        int permissionsInPeriod = activeState.config.getLimitForPeriod();

        /**
         * Calculate this one before hand
         */
        long nanosPerOnePermission = permissionsPeriodInNanos/permissionsInPeriod;

        long currentNanos = currentNanoTime();
        long nanosSinceLastUpdate = currentNanos - activeState.updatedAt;

        int currentPermissions = activeState.activePermissions;

        long permissionsBatches = (permissionsInPeriod * nanosSinceLastUpdate / permissionsPeriodInNanos);

        long accumulatedPermissions = permissionsBatches + currentPermissions;

        int nexPermissions = (int) min(activeState.config.getBurstLimit(), accumulatedPermissions);

        long nextNanosToWait = nanosToWaitForPermission(
            permits, nanosPerOnePermission, nexPermissions);

        State nextState = reservePermissions(activeState.config, permits, timeoutInNanos,
            nexPermissions, nextNanosToWait);
        return nextState;
    }

    /**
     * Calculates time to wait for the required permits of permissions to get accumulated
     *
     * @param permits              permits of required permissions
     * @param nanosPerPermission   nanos needed for one permission to be released
     * @param availablePermissions currently available permissions, can be negative if some
     *                             permissions have been reserved
     */
    private long nanosToWaitForPermission(final int permits, final long nanosPerPermission,
                                          final long availablePermissions) {
        if (availablePermissions >= permits) {
            return 0L;
        }

        long permissionsWanted = permits - availablePermissions;
        long nanosToWait = permissionsWanted*nanosPerPermission;
        return nanosToWait;
    }

    /**
     * Determines whether caller can acquire permission before timeout or not and then creates
     * corresponding {@link State}. Reserves permissions only if caller can successfully wait for
     * permission.
     *
     * @param config
     * @param permits        permits of permissions
     * @param timeoutInNanos max time that caller can wait for permission in nanoseconds
     * @param permissions    permissions for new {@link State}
     * @param nanosToWait    nanoseconds to wait for the next permission
     * @return new {@link State} with possibly reserved permissions and time to wait
     */
    private State reservePermissions(final RateLimiterConfig config, final int permits,
                                     final long timeoutInNanos,
                                     final int permissions, final long nanosToWait) {
        boolean canAcquireInTime = timeoutInNanos >= nanosToWait;
        int permissionsWithReservation = permissions;
        if (canAcquireInTime) {
            permissionsWithReservation -= permits;
        }

        return new State(config, permissionsWithReservation, nanosToWait, currentNanoTime());
    }

    /**
     * If nanosToWait is bigger than 0 it tries to park {@link Thread} for nanosToWait but not
     * longer then timeoutInNanos.
     *
     * @param timeoutInNanos max time that caller can wait
     * @param nanosToWait    nanoseconds caller need to wait
     * @return true if caller was able to wait for nanosToWait without {@link Thread#interrupt} and
     * not exceed timeout
     */
    private boolean waitForPermissionIfNecessary(final long timeoutInNanos,
                                                 final long nanosToWait) {
        boolean canAcquireImmediately = nanosToWait <= 0;
        boolean canAcquireInTime = timeoutInNanos >= nanosToWait;

        if (canAcquireImmediately) {
            return true;
        }
        if (canAcquireInTime) {
            return waitForPermission(nanosToWait);
        }
        waitForPermission(timeoutInNanos);
        return false;
    }

    /**
     * Parks {@link Thread} for nanosToWait.
     * <p>If the current thread is {@linkplain Thread#interrupted}
     * while waiting for a permit then it won't throw {@linkplain InterruptedException}, but its
     * interrupt status will be set.
     *
     * @param nanosToWait nanoseconds caller need to wait
     * @return true if caller was not {@link Thread#interrupted} while waiting
     */
    private boolean waitForPermission(final long nanosToWait) {
        waitingThreads.incrementAndGet();
        long deadline = currentNanoTime() + nanosToWait;
        boolean wasInterrupted = false;
        while (currentNanoTime() < deadline && !wasInterrupted) {
            long sleepBlockDuration = deadline - currentNanoTime();
            parkNanos(sleepBlockDuration);
            wasInterrupted = Thread.interrupted();
        }
        waitingThreads.decrementAndGet();
        if (wasInterrupted) {
            currentThread().interrupt();
        }
        return !wasInterrupted;
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
    public RateLimiterConfig getRateLimiterConfig() {
        return state.get().config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getTags() {
        return tags;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Metrics getMetrics() {
        return new RefillBasedRateLimiterMetrics();
    }

    @Override
    public EventPublisher getEventPublisher() {
        return eventProcessor;
    }

    @Override
    public String toString() {
        return "AtomicRateLimiter{" +
            "name='" + name + '\'' +
            ", rateLimiterConfig=" + state.get().config +
            '}';
    }

    /**
     * Get the enhanced Metrics with some implementation specific details.
     *
     * @return the detailed metrics
     */
    public RefillBasedRateLimiterMetrics getDetailedMetrics() {
        return new RefillBasedRateLimiterMetrics();
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
     * <p>{@link RefillBasedRateLimiter.State} represents immutable state of {@link RefillBasedRateLimiter}
     * where:
     * <ul>
     * <li>activePermissions - count of available permissions after
     * the last {@link RefillBasedRateLimiter#acquirePermission()} call.
     * Can be negative if some permissions where reserved.</li>
     * <p>
     * <li>nanosToWait - count of nanoseconds to wait for permission for
     * the last {@link RefillBasedRateLimiter#acquirePermission()} call.</li>
     * <p>
     * <li>updatedAt - the last time the state was updated.</li>
     * </ul>
     */
    private static class State {

        private final RateLimiterConfig config;

        private final int activePermissions;
        private final long nanosToWait;
        private final long updatedAt;

        public State(RateLimiterConfig config, int activePermissions, long nanosToWait, long updatedAt) {
            this.config = config;
            this.activePermissions = activePermissions;
            this.nanosToWait = nanosToWait;
            this.updatedAt = updatedAt;
        }
    }


    /**
     * Enhanced {@link Metrics} with some implementation specific details
     */
    public class RefillBasedRateLimiterMetrics implements Metrics {

        private RefillBasedRateLimiterMetrics() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getNumberOfWaitingThreads() {
            return waitingThreads.get();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getAvailablePermissions() {
            State currentState = state.get();
            State estimatedState = calculateNextState(1, -1, currentState);
            return estimatedState.activePermissions;
        }

        /**
         * @return estimated time duration in nanos to wait for the next permission
         */
        public long getNanosToWait() {
            State currentState = state.get();
            State estimatedState = calculateNextState(1, -1, currentState);
            return estimatedState.nanosToWait;
        }

    }
}