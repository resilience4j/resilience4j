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
import io.github.resilience4j.ratelimiter.RefillRateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;

import java.util.concurrent.atomic.AtomicReference;
import static java.lang.System.nanoTime;

/**
 * {@link RefillRateLimiter} has a max capacity of permits refilled periodically.
 * <p>Each period has duration of {@link RefillRateLimiterConfig#limitRefreshPeriod} in nanoseconds.
 * <p>By contract the initial {@link RefillRateLimiter.State#activePermissions} are set
 * to be the same with{@link RefillRateLimiterConfig#permitCapacity}
 * <p>A ratio of permit per nanoseconds is calculated using
 * {@link RefillRateLimiterConfig#limitRefreshPeriod} and {@link RefillRateLimiterConfig#limitForPeriod}.
 * On a permit request the permits that should have been replenished are calculated based on the nanos passed and the ratio of nanos per permit.
 * For the {@link RefillRateLimiter} callers it is looks like a token bucket supporting bursts and a gradual refill,
 * under the hood there is some optimisations that will skip this refresh if {@link RefillRateLimiter} is not used actively.
 * <p>All {@link RefillRateLimiter} updates are atomic and state is encapsulated in {@link
 * AtomicReference} to {@link RefillRateLimiter.State}
 */
public class RefillRateLimiter extends BaseAtomicLimiter<RefillRateLimiterConfig, RefillRateLimiter.State> implements RateLimiter {

    public RefillRateLimiter(String name, RefillRateLimiterConfig rateLimiterConfig) {
        this(name, rateLimiterConfig, HashMap.empty());
    }

    public RefillRateLimiter(String name, RefillRateLimiterConfig rateLimiterConfig,
                             Map<String, String> tags) {
        super(name, new AtomicReference<>(new State(
            rateLimiterConfig, rateLimiterConfig.getInitialPermits(), 0, nanoTime()
        )),tags);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected State calculateNextState(final int permits, final long timeoutInNanos,
                                     final State activeState) {
        int permissionsNeededExtra = permissionsMissing(permits, activeState.activePermissions);

        long currentNanoTime = nanoTime();

        if(permissionsNeededExtra<0) {
            return new State(activeState.getConfig(), -permissionsNeededExtra, 0, activeState.updatedAt);
        } else if(permissionsNeededExtra==0) {
            return new State(activeState.getConfig(), 0, 0, currentNanoTime);
        } else {
            assertLessPermitsThanCapacity(permits, activeState);
            return lazyPermissionCalculation(permits ,permissionsNeededExtra, activeState, currentNanoTime, timeoutInNanos);
        }
    }

    private void assertLessPermitsThanCapacity(int permits, State activeState) {
        if(permits > activeState.getConfig().getPermitCapacity()) {
            throw RequestNotPermitted.createRequestNotPermitted("Permits requested cannot exceed rate limiter capacity", this);
        }
    }

    private State lazyPermissionCalculation(int permits,int permissionsNeededExtra, final State activeState, long currentNanoTime, long timeoutInNanos) {
        RefillRateLimiterConfig config = activeState.getConfig();

        long nanosSinceLastUpdate = currentNanoTime - activeState.updatedAt;

        if(nanosSinceLastUpdate >= config.getNanosPerFullCapacity()) {
            /**
             * We reached our max capacity. We remove the permits from the max capacity
             * Regardless of the permits leased previously our permits could not exceeded the max
             * so we clean up
             */
            int permitsLeft = config.getPermitCapacity() - permits;
            return new State(activeState.getConfig(), permitsLeft, 0, currentNanoTime);
        } else {
            /**
             * We have not reached the max capacity. Thus we need to calculate the extra permissions needed.
             */
            long nanosForPermissions = nanosNeededForExtraPermissions(permissionsNeededExtra, config);
            long nanosToCurrentIndex = activeState.updatedAt + nanosForPermissions;

            long nanosToWait = nanosForPermissions - nanosSinceLastUpdate;

            if(nanosToWait>0) {
                boolean canAcquireInTime = timeoutInNanos >= nanosToWait;

                if(canAcquireInTime) {
                    return new State(config, 0, nanosToWait, nanosToCurrentIndex);
                } else {
                    return new State(activeState.getConfig(), activeState.activePermissions, nanosToWait, activeState.updatedAt);
                }
            } else {
                return new State(config, 0, 0, nanosToCurrentIndex);
            }
        }
    }

    private long nanosNeededForExtraPermissions(int neededPermissions, RefillRateLimiterConfig config) {
        return neededPermissions * config.getNanosPerPermit();
    }

    private int permissionsMissing(int permits, int currentPermits) {
        return permits - currentPermits;
    }

    private long nanosSinceLastUpdate(State activeState) {
        return nanoTime() - activeState.updatedAt;
    }

    @Override
    public String toString() {
        return "RefillRateLimiter{" +
            "name='" + getName()+ '\'' +
            ", rateLimiterConfig=" + state().get().getConfig()+
            '}';
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RefillRateLimiterMetrics getDetailedMetrics() {
        return new RefillRateLimiterMetrics();
    }

    /**
     * <p>{@link RefillRateLimiter.State} represents immutable state of {@link RefillRateLimiter}
     * where:
     * <ul>
     * <li>activePermissions - count of available permissions after
     * the last {@link RefillRateLimiter#acquirePermission()} call.
     * Can be negative if some permissions where reserved.</li>
     * <p>
     * <li>nanosToWait - count of nanoseconds to wait for permission for
     * the last {@link RefillRateLimiter#acquirePermission()} call.</li>
     * <p>
     * <li>updatedAt - the last time the state was updated.</li>
     * </ul>
     */
    static class State extends BaseState<RefillRateLimiterConfig> {

        private final int activePermissions;
        private final long updatedAt;

        public State(RefillRateLimiterConfig config, int activePermissions, long nanosToWait, long updatedAt) {
            super(config, activePermissions, nanosToWait);
            this.activePermissions = activePermissions;
            this.updatedAt = updatedAt;
        }

        @Override
        State withConfig(RefillRateLimiterConfig config) {
            return new State(config, activePermissions, getNanosToWait(), updatedAt);
        }

    }

    /**
     * Enhanced {@link Metrics} with some implementation specific details
     */
    public class RefillRateLimiterMetrics implements Metrics {

        private RefillRateLimiterMetrics() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getNumberOfWaitingThreads() {
            return waitingThreads().get();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getAvailablePermissions() {
            State currentState = state().get();
            State estimatedState = calculateNextState(1, -1, currentState);
            return estimatedState.activePermissions;
        }

        /**
         * @return estimated time duration in nanos to wait for the next permission
         */
        public long getNanosToWait() {
            State currentState = state().get();
            State estimatedState = calculateNextState(1, -1, currentState);
            return estimatedState.getNanosToWait();
        }

    }
}
