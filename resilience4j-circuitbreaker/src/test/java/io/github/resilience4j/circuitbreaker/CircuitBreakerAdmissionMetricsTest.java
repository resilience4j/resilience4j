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
package io.github.resilience4j.circuitbreaker;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import io.github.resilience4j.circuitbreaker.CircuitBreaker.AdmissionMetrics;
import org.junit.jupiter.api.Test;

import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED;
import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.HALF_OPEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

class CircuitBreakerAdmissionMetricsTest {

    @Test
    void mixedPermissionApisCountEachOfferOnceBeforeAnyCompletion() {
        CircuitBreaker breaker = recovering(new ControlledClock(), 5);
        int admitted = 0;
        for (int i = 0; i < 100; i++) {
            if (permission(breaker, i % 3 != 0)) {
                admitted++;
            }
        }

        assertThat(admitted).isEqualTo(5);
        assertCounts(breaker, 100, 5);
        assertThat(breaker.getMetrics().getNumberOfBufferedCalls()).isZero();
        assertThat(breaker.getHalfOpenPhase().getPermittedTrafficPercentage()).isEqualTo(5);
    }

    @Test
    void pendingCompletionStagesAreAlreadyAdmittedAndTheirOutcomesDoNotChangeCounts() {
        CircuitBreaker breaker = recovering(new ControlledClock(), 5);
        List<CompletableFuture<String>> backendCalls = new ArrayList<>();
        Supplier<CompletionStage<String>> decorated = CircuitBreaker.decorateCompletionStage(breaker, () -> {
            CompletableFuture<String> backend = new CompletableFuture<>();
            backendCalls.add(backend);
            return backend;
        });
        List<CompletionStage<String>> results = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            results.add(decorated.get());
        }

        assertThat(backendCalls).hasSize(5);
        assertThat(backendCalls).allSatisfy(call -> assertThat(call.isDone()).isFalse());
        assertCounts(breaker, 100, 5);
        assertThat(breaker.getMetrics().getNumberOfBufferedCalls()).isZero();
        assertThat(results.stream()
            .filter(result -> result.toCompletableFuture().isCompletedExceptionally())
            .count())
            .isEqualTo(95);

        backendCalls.get(0).complete("ok");
        backendCalls.get(1).completeExceptionally(new IllegalStateException("PSP failure"));
        backendCalls.get(2).completeExceptionally(new IgnoredFailure());
        backendCalls.get(3).cancel(false);
        backendCalls.get(4).complete("ok");

        assertCounts(breaker, 100, 5);
    }

    @Test
    void releasingUnusedPermissionAndIgnoringAnOutcomeDoNotUndoAdmissions() {
        CircuitBreaker breaker = CircuitBreaker.of("released", CircuitBreakerConfig.custom()
            .ignoreExceptions(IgnoredFailure.class)
            .halfOpenRecoveryConfig(HalfOpenRecoveryConfig.custom().build()).build());
        breaker.acquirePermission();
        breaker.releasePermission();

        assertCounts(breaker, 1, 1);
        assertThat(breaker.tryAcquirePermission()).isTrue();
        breaker.onError(0, TimeUnit.NANOSECONDS, new IgnoredFailure());

        assertCounts(breaker, 2, 2);
        assertThat(breaker.getMetrics().getNumberOfBufferedCalls()).isZero();
    }

    @Test
    void throwingOutcomePredicateStillDoesNotUndoAnAdmission() {
        IllegalStateException predicateFailure = new IllegalStateException("broken predicate");
        CircuitBreaker breaker = CircuitBreaker.of("predicate", CircuitBreakerConfig.custom()
            .recordException(error -> {
                throw predicateFailure;
            })
            .halfOpenRecoveryConfig(HalfOpenRecoveryConfig.custom().build()).build());
        breaker.acquirePermission();

        assertThatThrownBy(() -> breaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException("backend")))
            .isSameAs(predicateFailure);
        assertCounts(breaker, 1, 1);
    }

    @Test
    void lazyOpenToHalfOpenDelegationCountsOnePublicAttempt() {
        for (boolean throwingApi : new boolean[]{false, true}) {
            ControlledClock clock = new ControlledClock();
            CircuitBreaker breaker = breaker(clock, 5);
            breaker.transitionToOpenState();
            clock.advance(Duration.ofSeconds(2));

            assertThat(permission(breaker, throwingApi)).isTrue();
            assertThat(breaker.getState()).isEqualTo(HALF_OPEN);
            assertCounts(breaker, 1, 1);
        }
    }

    @Test
    void deniedListenerSeesTheOfferBeforeMakingAReentrantPermissionRequest() {
        CircuitBreaker breaker = breaker(new ControlledClock(), 5);
        AtomicReference<AdmissionMetrics> observed = new AtomicReference<>();
        breaker.transitionToOpenState();
        breaker.getEventPublisher().onCallNotPermitted(event -> {
            observed.set(breaker.getAdmissionMetrics());
            breaker.reset();
            breaker.acquirePermission();
        });

        assertThat(breaker.tryAcquirePermission()).isFalse();
        assertThat(observed.get()).isNotNull();
        assertThat(observed.get().getNumberOfAttempts()).isEqualTo(1);
        assertThat(observed.get().getNumberOfPermittedAttempts()).isZero();
        assertCounts(breaker, 2, 1);
        assertThat(breaker.getState()).isEqualTo(CLOSED);
    }

    @Test
    void ordinaryBreakersKeepFiniteHalfOpenPermissionsWithoutOptionalAdmissionCollection() {
        CircuitBreaker breaker = CircuitBreaker.of("ordinary", CircuitBreakerConfig.custom()
            .clock(new ControlledClock()).permittedNumberOfCallsInHalfOpenState(2).build());
        breaker.acquirePermission();

        assertThat(breaker.tryAcquirePermission()).isTrue();
        assertCounts(breaker, 0, 0);
        breaker.transitionToOpenState();

        assertThat(breaker.tryAcquirePermission()).isFalse();
        assertThatThrownBy(breaker::acquirePermission).isInstanceOf(CallNotPermittedException.class);
        breaker.transitionToHalfOpenState();

        assertThat(permission(breaker, true)).isTrue();
        assertThat(permission(breaker, false)).isTrue();
        assertThat(permission(breaker, true)).isFalse();
        assertThat(permission(breaker, false)).isFalse();
        assertCounts(breaker, 0, 0);
        breaker.releasePermission();

        assertThat(breaker.tryAcquirePermission()).isTrue();
        assertThat(breaker.tryAcquirePermission()).isFalse();
        assertCounts(breaker, 0, 0);

        breaker.transitionToDisabledState();

        assertThat(breaker.tryAcquirePermission()).isTrue();
        breaker.acquirePermission();

        assertCounts(breaker, 0, 0);
        breaker.transitionToMetricsOnlyState();
        breaker.acquirePermission();

        assertThat(breaker.tryAcquirePermission()).isTrue();
        assertCounts(breaker, 0, 0);
        breaker.transitionToForcedOpenState();

        assertThat(breaker.tryAcquirePermission()).isFalse();
        assertThatThrownBy(breaker::acquirePermission).isInstanceOf(CallNotPermittedException.class);
        assertCounts(breaker, 0, 0);
        breaker.reset();
        breaker.acquirePermission();

        assertThat(breaker.tryAcquirePermission()).isTrue();
        assertCounts(breaker, 0, 0);
        assertThat(breaker.getHalfOpenPhase().isActive()).isFalse();
    }

    @Test
    void enabledCountersSpanAdministrativeStatesWithoutRelyingOnEventsAndSurviveReset() {
        CircuitBreaker breaker = breaker(new ControlledClock(), 5);
        breaker.acquirePermission();

        assertThat(breaker.tryAcquirePermission()).isTrue();
        assertCounts(breaker, 2, 2);
        breaker.transitionToDisabledState();

        assertThat(breaker.tryAcquirePermission()).isTrue();
        breaker.acquirePermission();

        assertCounts(breaker, 4, 4);
        breaker.transitionToMetricsOnlyState();
        breaker.acquirePermission();

        assertThat(breaker.tryAcquirePermission()).isTrue();
        assertCounts(breaker, 6, 6);
        breaker.transitionToForcedOpenState();

        assertThat(breaker.tryAcquirePermission()).isFalse();
        assertThatThrownBy(breaker::acquirePermission).isInstanceOf(CallNotPermittedException.class);
        assertCounts(breaker, 8, 6);
        breaker.reset();
        breaker.acquirePermission();

        assertThat(breaker.tryAcquirePermission()).isTrue();
        assertCounts(breaker, 10, 8);
    }

    @Test
    void countersSurvivePhaseAdvanceStepBackAndResetAndOldSnapshotsStayImmutable() {
        ControlledClock clock = new ControlledClock();
        CircuitBreaker breaker = CircuitBreaker.of("changing", CircuitBreakerConfig.custom().clock(clock)
            .halfOpenRecoveryConfig(HalfOpenRecoveryConfig.custom().initialTrafficPercentage(25)
                .trafficPercentageStep(25)
                .minimumNumberOfCalls(2)
                .minimumPhaseDuration(Duration.ofSeconds(10))
                .failureStrategy(HalfOpenRecoveryConfig.FailureStrategy.STEP_BACK).build()).build());
        enterRecovery(breaker);
        acquireAdmitted(breaker);
        breaker.onSuccess(0, TimeUnit.NANOSECONDS);
        acquireAdmitted(breaker);
        breaker.onSuccess(0, TimeUnit.NANOSECONDS);

        assertCounts(breaker, 5, 2);
        clock.advance(Duration.ofSeconds(10));

        acquireAdmitted(breaker);
        assertThat(breaker.getHalfOpenPhase().getPermittedTrafficPercentage()).isEqualTo(25);
        assertCounts(breaker, 9, 3);
        breaker.onSuccess(0, TimeUnit.NANOSECONDS);
        assertThat(breaker.getHalfOpenPhase().getPermittedTrafficPercentage()).isEqualTo(50);
        assertCounts(breaker, 9, 3);
        acquireAdmitted(breaker);
        breaker.onError(0, TimeUnit.NANOSECONDS, new IllegalStateException("unhealthy"));
        acquireAdmitted(breaker);
        breaker.onError(0, TimeUnit.NANOSECONDS, new IllegalStateException("unhealthy"));

        assertThat(breaker.getHalfOpenPhase().getPermittedTrafficPercentage()).isEqualTo(25);
        assertCounts(breaker, 13, 5);
        AdmissionMetrics previous = breaker.getAdmissionMetrics();
        breaker.reset();

        assertCounts(breaker, 13, 5);
        breaker.acquirePermission();

        assertCounts(breaker, 14, 6);
        assertThat(previous.getNumberOfAttempts()).isEqualTo(13);
        assertThat(previous.getNumberOfPermittedAttempts()).isEqualTo(5);
    }

    @Test
    void failedPermissionCheckIsCountedWithoutChangingItsException() {
        for (boolean throwingApi : new boolean[]{false, true}) {
            ControlledClock clock = new ControlledClock();
            CircuitBreaker breaker = breaker(clock, 5);
            breaker.transitionToOpenState();
            IllegalStateException failure = new IllegalStateException("clock unavailable");
            clock.nextFailure.set(failure);

            assertThatThrownBy(() -> permission(breaker, throwingApi)).isSameAs(failure);
            assertCounts(breaker, 1, 0);
        }
    }

    @Test
    void attemptIsVisibleWhilePermissionEvaluationIsStillPending() throws Exception {
        ControlledClock clock = new ControlledClock();
        CircuitBreaker breaker = breaker(clock, 5);
        breaker.transitionToOpenState();
        clock.advance(Duration.ofSeconds(2));
        PendingGate pending = new PendingGate();
        clock.nextBlock.set(pending);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Boolean> permission = executor.submit(() -> {
                breaker.acquirePermission();
                return true;
            });

            assertThat(pending.entered.await(10, TimeUnit.SECONDS)).isTrue();
            assertCounts(breaker, 1, 0);
            pending.release.countDown();

            assertThat(permission.get(10, TimeUnit.SECONDS)).isTrue();
            assertCounts(breaker, 1, 1);
        } finally {
            pending.release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentAttemptsHaveExactTotalsAndSnapshotsKeepTheirBounds() throws Exception {
        CircuitBreaker breaker = recovering(new ControlledClock(), 37);
        ExecutorService executor = Executors.newFixedThreadPool(9);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch firstRead = new CountDownLatch(1);
        AtomicBoolean reading = new AtomicBoolean(true);
        List<Future<Integer>> workers = new ArrayList<>();
        Future<Integer> reader = executor.submit(() -> {
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("Start was not released");
            }
            int snapshots = 0;
            try {
                while (reading.get()) {
                    AdmissionMetrics snapshot = breaker.getAdmissionMetrics();

                    assertThat(snapshot.getNumberOfPermittedAttempts())
                        .isBetween(0L, snapshot.getNumberOfAttempts());
                    snapshots++;
                    firstRead.countDown();
                }
                return snapshots;
            } finally {
                firstRead.countDown();
            }
        });
        try {
            for (int thread = 0; thread < 8; thread++) {
                workers.add(executor.submit(() -> {
                    if (!start.await(10, TimeUnit.SECONDS) || !firstRead.await(10, TimeUnit.SECONDS)) {
                        throw new AssertionError("Workers were not released");
                    }
                    int permitted = 0;
                    for (int i = 0; i < 1000; i++) {
                        if (permission(breaker, i % 2 == 0)) {
                            permitted++;
                        }
                    }
                    return permitted;
                }));
            }
            start.countDown();
            int permitted = 0;
            for (Future<Integer> worker : workers) {
                permitted += worker.get(10, TimeUnit.SECONDS);
            }
            reading.set(false);

            assertThat(reader.get(10, TimeUnit.SECONDS)).isPositive();
            assertThat(permitted).isEqualTo(2960);
            assertCounts(breaker, 8000, 2960);
        } finally {
            reading.set(false);
            start.countDown();
            firstRead.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void externalImplementationsHaveAnEmptyCompatibleDefaultAndSnapshotsRejectInvalidCounts() {
        CircuitBreaker external = mock(CircuitBreaker.class, CALLS_REAL_METHODS);

        assertCounts(external, 0, 0);
        long[][] invalid = {{-1, 0}, {1, -1}, {1, 2}};
        for (long[] counts : invalid) {
            assertThatThrownBy(() -> new AdmissionMetrics(counts[0], counts[1]))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    private static CircuitBreaker recovering(ControlledClock clock, int percentage) {
        CircuitBreaker breaker = breaker(clock, percentage);
        enterRecovery(breaker);
        return breaker;
    }

    private static CircuitBreaker breaker(ControlledClock clock, int percentage) {
        return CircuitBreaker.of("admission", CircuitBreakerConfig.custom().clock(clock)
            .waitDurationInOpenState(Duration.ofSeconds(1)).ignoreExceptions(IgnoredFailure.class)
            .halfOpenRecoveryConfig(HalfOpenRecoveryConfig.custom().initialTrafficPercentage(percentage)
                .minimumNumberOfCalls(10).minimumPhaseDuration(Duration.ofSeconds(10)).build()).build());
    }

    private static void enterRecovery(CircuitBreaker breaker) {
        breaker.transitionToOpenState();
        breaker.transitionToHalfOpenState();
    }

    private static boolean permission(CircuitBreaker breaker, boolean throwing) {
        if (!throwing) {
            return breaker.tryAcquirePermission();
        }
        try {
            breaker.acquirePermission();
            return true;
        } catch (CallNotPermittedException expected) {
            return false;
        }
    }

    private static void acquireAdmitted(CircuitBreaker breaker) {
        for (int attempt = 0; attempt < 100; attempt++) {
            if (breaker.tryAcquirePermission()) {
                return;
            }
        }
        throw new AssertionError("Positive recovery percentage must admit within 100 offers");
    }

    private static void assertCounts(CircuitBreaker breaker, long attempts, long permitted) {
        AdmissionMetrics snapshot = breaker.getAdmissionMetrics();

        assertThat(snapshot.getNumberOfAttempts()).isEqualTo(attempts);
        assertThat(snapshot.getNumberOfPermittedAttempts()).isEqualTo(permitted);
    }

    private static final class IgnoredFailure extends RuntimeException {
    }

    private static final class PendingGate {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
    }

    private static final class ControlledClock extends Clock {
        private final AtomicReference<Instant> now;
        private final ZoneId zone;
        private final AtomicReference<RuntimeException> nextFailure;
        private final AtomicReference<PendingGate> nextBlock;

        ControlledClock() {
            this(new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z")), ZoneOffset.UTC,
                new AtomicReference<>(), new AtomicReference<>());
        }

        private ControlledClock(AtomicReference<Instant> now, ZoneId zone,
            AtomicReference<RuntimeException> nextFailure, AtomicReference<PendingGate> nextBlock) {
            this.now = now;
            this.zone = zone;
            this.nextFailure = nextFailure;
            this.nextBlock = nextBlock;
        }

        void advance(Duration duration) {
            now.updateAndGet(instant -> instant.plus(duration));
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new ControlledClock(now, zone, nextFailure, nextBlock);
        }

        @Override
        public Instant instant() {
            RuntimeException failure = nextFailure.getAndSet(null);
            if (failure != null) {
                throw failure;
            }
            PendingGate pending = nextBlock.getAndSet(null);
            if (pending != null) {
                pending.entered.countDown();
                try {
                    if (!pending.release.await(10, TimeUnit.SECONDS)) {
                        throw new AssertionError("Gate was not released");
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(interrupted);
                }
            }
            return now.get();
        }
    }
}
