/*
 *
 *  Copyright 2026 authors
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
package io.github.resilience4j.circuitbreaker.internal;

import io.github.resilience4j.circuitbreaker.CircuitBreaker.HalfOpenPhase;
import io.github.resilience4j.circuitbreaker.HalfOpenRecoveryConfig;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.resilience4j.circuitbreaker.internal.CircuitBreakerMetrics.Result;
import static io.github.resilience4j.circuitbreaker.internal.CircuitBreakerMetrics.Result.BELOW_THRESHOLDS;

/**
 * The traffic and evaluation policy for one gradual HALF_OPEN recovery cycle.
 * State ownership, timers, metrics recording and event publication belong to the
 * state machine. This policy only decides admission and the next recovery action.
 */
final class HalfOpenRecoveryPolicy {

    enum Decision {
        STAY,
        ADVANCE,
        STEP_BACK,
        OPEN,
        CLOSE
    }

    private final HalfOpenRecoveryConfig config;
    private final int numberOfPhases;
    private final AtomicInteger admissionRemainder;

    HalfOpenRecoveryPolicy(HalfOpenRecoveryConfig config) {
        this.config = Objects.requireNonNull(config);
        // Prime the first probe once per recovery cycle. Phase changes carry the remaining
        // fractional admission budget instead of granting another initial probe.
        admissionRemainder = new AtomicInteger(100 - config.getInitialTrafficPercentage());
        int remaining = 100 - config.getInitialTrafficPercentage();
        numberOfPhases = 1 + (remaining + config.getTrafficPercentageStep() - 1)
            / config.getTrafficPercentageStep();
    }

    int getMinimumNumberOfCalls() {
        return config.getMinimumNumberOfCalls();
    }

    Phase newPhase(int index, Instant enteredAt) {
        if (index < 0 || index >= numberOfPhases) {
            throw new IllegalArgumentException("Recovery phase index is out of range");
        }
        int percentage = Math.min(100, config.getInitialTrafficPercentage()
            + index * config.getTrafficPercentageStep());
        return new Phase(config, index, new HalfOpenPhase(index + 1, numberOfPhases, percentage),
            enteredAt, admissionRemainder);
    }

    /** Immutable phase identity and timing sharing the recovery cycle's admission budget. */
    static final class Phase {

        private final HalfOpenRecoveryConfig config;
        private final int index;
        private final HalfOpenPhase snapshot;
        private final Instant enteredAt;
        private final AtomicInteger admissionRemainder;

        private Phase(HalfOpenRecoveryConfig config, int index, HalfOpenPhase snapshot,
            Instant enteredAt, AtomicInteger admissionRemainder) {
            this.config = config;
            this.index = index;
            this.snapshot = snapshot;
            this.enteredAt = Objects.requireNonNull(enteredAt);
            this.admissionRemainder = admissionRemainder;
        }

        int getIndex() {
            return index;
        }

        HalfOpenPhase getSnapshot() {
            return snapshot;
        }

        boolean tryAcquirePermission() {
            int percentage = snapshot.getPermittedTrafficPercentage();
            int remainder = admissionRemainder.getAndUpdate(value -> (value + percentage) % 100);
            return remainder + percentage >= 100;
        }

        boolean minimumDurationElapsed(Clock clock) {
            return Duration.between(enteredAt, clock.instant())
                .compareTo(config.getMinimumPhaseDuration()) >= 0;
        }

        Decision evaluate(Result result, Clock clock) {
            if (Result.hasExceededThresholds(result)) {
                return rejectPhase();
            }
            if (result == Result.RECOVERY_HOLD) {
                return snapshot.getPermittedTrafficPercentage() == 100 && minimumDurationElapsed(clock)
                    ? rejectPhase() : Decision.STAY;
            }
            if (result != BELOW_THRESHOLDS || !minimumDurationElapsed(clock)) {
                return Decision.STAY;
            }
            return snapshot.getPermittedTrafficPercentage() == 100
                ? Decision.CLOSE : Decision.ADVANCE;
        }

        private Decision rejectPhase() {
            return config.getFailureStrategy() == HalfOpenRecoveryConfig.FailureStrategy.STEP_BACK
                && index > 0 ? Decision.STEP_BACK : Decision.OPEN;
        }
    }
}
