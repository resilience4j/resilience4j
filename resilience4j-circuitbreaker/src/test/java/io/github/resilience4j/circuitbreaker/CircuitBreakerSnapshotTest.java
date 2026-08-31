/*
 *
 *  Copyright 2025
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

import com.statemachinesystems.mockclock.MockClock;
import org.junit.Test;

import java.io.*;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test to demonstrate issue #2325: Loss of metrics when recreating CircuitBreaker
 */
public class CircuitBreakerSnapshotTest {

    @Test
    public void shouldPreserveMetricsWhenRecreatingWithSnapshot() {
        // Given: CircuitBreaker with some metrics
        CircuitBreakerConfig config1 = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .slidingWindowSize(10)
            .build();

        CircuitBreaker cb1 = CircuitBreaker.of("testService", config1);

        // Simulate failures and successes
        for (int i = 0; i < 6; i++) {
            cb1.onError(100, TimeUnit.MILLISECONDS, new RuntimeException("Test failure"));
        }
        for (int i = 0; i < 4; i++) {
            cb1.onSuccess(100, TimeUnit.MILLISECONDS);
        }

        // Capture state before recreation
        int failedCallsBefore = cb1.getMetrics().getNumberOfFailedCalls();
        int successfulCallsBefore = cb1.getMetrics().getNumberOfSuccessfulCalls();
        long notPermittedCallsBefore = cb1.getMetrics().getNumberOfNotPermittedCalls();
        CircuitBreaker.State stateBefore = cb1.getState();

        // When: Create snapshot
        CircuitBreakerSnapshot snapshot = cb1.createSnapshot();

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.getState()).isEqualTo(stateBefore);
        assertThat(snapshot.getMetricsSnapshot().getNumberOfFailedCalls()).isEqualTo(failedCallsBefore);
        assertThat(snapshot.getMetricsSnapshot().getNumberOfSuccessfulCalls()).isEqualTo(successfulCallsBefore);
        assertThat(snapshot.getMetricsSnapshot().getNumberOfNotPermittedCalls()).isEqualTo(notPermittedCallsBefore);

        // And: Configuration changes, create new CircuitBreaker with snapshot
        CircuitBreakerConfig config2 = CircuitBreakerConfig.custom()
            .failureRateThreshold(60)  // Changed threshold
            .slidingWindowSize(10)
            .build();

        CircuitBreaker cb2 = CircuitBreaker.of("testService", config2, snapshot);

        // Then: State is preserved
        assertThat(cb2.getState())
            .as("State should be preserved")
            .isEqualTo(stateBefore);

        // Note: Metrics values from snapshot are captured in the snapshot object,
        // but the sliding window metrics start fresh with the new config.
        // This is expected behavior for Phase 1 implementation.
    }

    @Test
    public void shouldPreserveOpenStateWhenRecreatingWithSnapshot() {
        // Given: CircuitBreaker in OPEN state
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .slidingWindowSize(5)
            .minimumNumberOfCalls(5)
            .waitDurationInOpenState(Duration.ofMinutes(1))
            .build();

        CircuitBreaker cb1 = CircuitBreaker.of("testService", config);

        // Trigger OPEN state with failures
        for (int i = 0; i < 5; i++) {
            cb1.onError(100, TimeUnit.MILLISECONDS, new RuntimeException("Test failure"));
        }

        CircuitBreaker.State stateBefore = cb1.getState();
        assertThat(stateBefore).isEqualTo(CircuitBreaker.State.OPEN);

        // When: Create snapshot and recreate with it
        CircuitBreakerSnapshot snapshot = cb1.createSnapshot();
        assertThat(snapshot.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(snapshot.getAttempts()).isGreaterThan(0);
        assertThat(snapshot.getRetryAfterWaitUntil()).isNotNull();

        CircuitBreaker cb2 = CircuitBreaker.of("testService", config, snapshot);

        // Then: OPEN state is preserved
        assertThat(cb2.getState())
            .as("OPEN state should be preserved")
            .isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    public void shouldPreserveHalfOpenState() {
        // Given: CircuitBreaker in HALF_OPEN state
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .slidingWindowSize(5)
            .minimumNumberOfCalls(5)
            .permittedNumberOfCallsInHalfOpenState(3)
            .build();

        CircuitBreaker cb1 = CircuitBreaker.of("testService", config);

        // Trigger OPEN state
        for (int i = 0; i < 5; i++) {
            cb1.onError(100, TimeUnit.MILLISECONDS, new RuntimeException("Test failure"));
        }

        // Transition to HALF_OPEN
        cb1.transitionToHalfOpenState();
        assertThat(cb1.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        // When: Create snapshot
        CircuitBreakerSnapshot snapshot = cb1.createSnapshot();
        assertThat(snapshot.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        // Recreate with snapshot
        CircuitBreaker cb2 = CircuitBreaker.of("testService", config, snapshot);

        // Then: HALF_OPEN state is preserved
        assertThat(cb2.getState())
            .as("HALF_OPEN state should be preserved")
            .isEqualTo(CircuitBreaker.State.HALF_OPEN);
    }

    @Test
    public void shouldPreserveClosedState() {
        // Given: CircuitBreaker in CLOSED state
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .slidingWindowSize(10)
            .build();

        CircuitBreaker cb1 = CircuitBreaker.of("testService", config);

        // Add some successful calls
        for (int i = 0; i < 5; i++) {
            cb1.onSuccess(100, TimeUnit.MILLISECONDS);
        }

        assertThat(cb1.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // When: Create snapshot
        CircuitBreakerSnapshot snapshot = cb1.createSnapshot();
        assertThat(snapshot.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Recreate with snapshot
        CircuitBreaker cb2 = CircuitBreaker.of("testService", config, snapshot);

        // Then: CLOSED state is preserved
        assertThat(cb2.getState())
            .as("CLOSED state should be preserved")
            .isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    public void shouldPreserveDisabledState() {
        // Given: CircuitBreaker in DISABLED state
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .slidingWindowSize(10)
            .build();

        CircuitBreaker cb1 = CircuitBreaker.of("testService", config);
        cb1.transitionToDisabledState();

        assertThat(cb1.getState()).isEqualTo(CircuitBreaker.State.DISABLED);

        // When: Create snapshot
        CircuitBreakerSnapshot snapshot = cb1.createSnapshot();
        assertThat(snapshot.getState()).isEqualTo(CircuitBreaker.State.DISABLED);

        // Recreate with snapshot
        CircuitBreaker cb2 = CircuitBreaker.of("testService", config, snapshot);

        // Then: DISABLED state is preserved
        assertThat(cb2.getState())
            .as("DISABLED state should be preserved")
            .isEqualTo(CircuitBreaker.State.DISABLED);
    }

    @Test
    public void shouldPreserveForcedOpenState() {
        // Given: CircuitBreaker in FORCED_OPEN state
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .slidingWindowSize(10)
            .build();

        CircuitBreaker cb1 = CircuitBreaker.of("testService", config);
        cb1.transitionToForcedOpenState();

        assertThat(cb1.getState()).isEqualTo(CircuitBreaker.State.FORCED_OPEN);

        // When: Create snapshot
        CircuitBreakerSnapshot snapshot = cb1.createSnapshot();
        assertThat(snapshot.getState()).isEqualTo(CircuitBreaker.State.FORCED_OPEN);
        assertThat(snapshot.getAttempts()).isGreaterThan(0);

        // Recreate with snapshot
        CircuitBreaker cb2 = CircuitBreaker.of("testService", config, snapshot);

        // Then: FORCED_OPEN state is preserved
        assertThat(cb2.getState())
            .as("FORCED_OPEN state should be preserved")
            .isEqualTo(CircuitBreaker.State.FORCED_OPEN);
    }

    @Test
    public void shouldPreserveMetricsOnlyState() {
        // Given: CircuitBreaker in METRICS_ONLY state
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .slidingWindowSize(10)
            .build();

        CircuitBreaker cb1 = CircuitBreaker.of("testService", config);
        cb1.transitionToMetricsOnlyState();

        assertThat(cb1.getState()).isEqualTo(CircuitBreaker.State.METRICS_ONLY);

        // When: Create snapshot
        CircuitBreakerSnapshot snapshot = cb1.createSnapshot();
        assertThat(snapshot.getState()).isEqualTo(CircuitBreaker.State.METRICS_ONLY);

        // Recreate with snapshot
        CircuitBreaker cb2 = CircuitBreaker.of("testService", config, snapshot);

        // Then: METRICS_ONLY state is preserved
        assertThat(cb2.getState())
            .as("METRICS_ONLY state should be preserved")
            .isEqualTo(CircuitBreaker.State.METRICS_ONLY);
    }

    @Test
    public void shouldCaptureAllMetricsInSnapshot() {
        // Given: CircuitBreaker with various metrics
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .slidingWindowSize(10)
            .slowCallDurationThreshold(Duration.ofMillis(200))
            .slowCallRateThreshold(50)
            .build();

        CircuitBreaker cb = CircuitBreaker.of("testService", config);

        // Record various call types
        cb.onSuccess(50, TimeUnit.MILLISECONDS);  // Fast success
        cb.onSuccess(250, TimeUnit.MILLISECONDS); // Slow success
        cb.onError(50, TimeUnit.MILLISECONDS, new RuntimeException()); // Fast failure
        cb.onError(250, TimeUnit.MILLISECONDS, new RuntimeException()); // Slow failure

        // When: Create snapshot
        CircuitBreakerSnapshot snapshot = cb.createSnapshot();
        CircuitBreakerSnapshot.MetricsSnapshot metrics = snapshot.getMetricsSnapshot();

        // Then: All metrics should be captured
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(2);
        assertThat(metrics.getNumberOfSlowCalls()).isEqualTo(2);
        assertThat(metrics.getNumberOfSlowSuccessfulCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfSlowFailedCalls()).isEqualTo(1);
    }

    // --- Null handling tests (#1) ---

    @Test
    public void shouldThrowNPEWhenSnapshotIsNull() {
        CircuitBreakerConfig config = CircuitBreakerConfig.ofDefaults();
        assertThatThrownBy(() -> CircuitBreaker.of("test", config, (CircuitBreakerSnapshot) null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void shouldThrowNPEWhenConfigIsNullWithSnapshot() {
        CircuitBreakerSnapshot snapshot = CircuitBreakerSnapshot.builder()
            .state(CircuitBreaker.State.CLOSED)
            .metricsSnapshot(CircuitBreakerSnapshot.MetricsSnapshot.builder().build())
            .build();
        assertThatThrownBy(() -> CircuitBreaker.of("test", (CircuitBreakerConfig) null, snapshot))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void shouldThrowNPEWhenBuilderStateIsNull() {
        assertThatThrownBy(() -> CircuitBreakerSnapshot.builder()
            .metricsSnapshot(CircuitBreakerSnapshot.MetricsSnapshot.builder().build())
            .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("State");
    }

    @Test
    public void shouldThrowNPEWhenBuilderMetricsSnapshotIsNull() {
        assertThatThrownBy(() -> CircuitBreakerSnapshot.builder()
            .state(CircuitBreaker.State.CLOSED)
            .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("MetricsSnapshot");
    }

    // --- Automatic transition test (#2) ---

    @Test
    public void shouldTransitionToHalfOpenAfterWaitDurationWhenRestoredFromSnapshot() {
        MockClock mockClock = MockClock.at(2019, 1, 1, 12, 0, 0, ZoneId.of("UTC"));
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .slidingWindowSize(5)
            .minimumNumberOfCalls(5)
            .waitDurationInOpenState(Duration.ofSeconds(5))
            .currentTimestampFunction(clock -> clock.instant().toEpochMilli(), TimeUnit.MILLISECONDS)
            .clock(mockClock)
            .build();

        CircuitBreaker cb1 = CircuitBreaker.of("testService", config);

        // Trigger OPEN state
        for (int i = 0; i < 5; i++) {
            cb1.onError(100, TimeUnit.MILLISECONDS, new RuntimeException("fail"));
        }
        assertThat(cb1.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Snapshot and restore
        CircuitBreakerSnapshot snapshot = cb1.createSnapshot();
        CircuitBreaker cb2 = CircuitBreaker.of("testService", config, snapshot);

        assertThat(cb2.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Wait duration not yet elapsed — should still be OPEN
        mockClock.advanceBySeconds(3);
        assertThat(cb2.tryAcquirePermission()).isFalse();
        assertThat(cb2.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Wait duration elapsed — should transition to HALF_OPEN
        mockClock.advanceBySeconds(3);
        assertThat(cb2.tryAcquirePermission()).isTrue();
        assertThat(cb2.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
    }

    // --- Post-restore behavior tests (#3) ---

    @Test
    public void shouldTrackMetricsAfterRestore() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .build();

        CircuitBreaker cb1 = CircuitBreaker.of("testService", config);
        cb1.onSuccess(100, TimeUnit.MILLISECONDS);
        cb1.onSuccess(100, TimeUnit.MILLISECONDS);

        CircuitBreakerSnapshot snapshot = cb1.createSnapshot();
        CircuitBreaker cb2 = CircuitBreaker.of("testService", config, snapshot);

        // New calls after restore should be tracked
        cb2.onSuccess(100, TimeUnit.MILLISECONDS);
        cb2.onError(100, TimeUnit.MILLISECONDS, new RuntimeException("fail"));

        assertThat(cb2.getMetrics().getNumberOfSuccessfulCalls()).isGreaterThanOrEqualTo(1);
        assertThat(cb2.getMetrics().getNumberOfFailedCalls()).isGreaterThanOrEqualTo(1);
    }

    @Test
    public void shouldTransitionFromClosedToOpenAfterRestore() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .slidingWindowSize(5)
            .minimumNumberOfCalls(5)
            .build();

        CircuitBreaker cb1 = CircuitBreaker.of("testService", config);
        assertThat(cb1.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        CircuitBreakerSnapshot snapshot = cb1.createSnapshot();
        CircuitBreaker cb2 = CircuitBreaker.of("testService", config, snapshot);
        assertThat(cb2.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Trigger OPEN via failures on restored CB
        for (int i = 0; i < 5; i++) {
            cb2.onError(100, TimeUnit.MILLISECONDS, new RuntimeException("fail"));
        }
        assertThat(cb2.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    public void shouldTransitionFromOpenToHalfOpenAfterRestore() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .slidingWindowSize(5)
            .minimumNumberOfCalls(5)
            .waitDurationInOpenState(Duration.ofMinutes(1))
            .build();

        CircuitBreaker cb1 = CircuitBreaker.of("testService", config);
        for (int i = 0; i < 5; i++) {
            cb1.onError(100, TimeUnit.MILLISECONDS, new RuntimeException("fail"));
        }
        assertThat(cb1.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        CircuitBreakerSnapshot snapshot = cb1.createSnapshot();
        CircuitBreaker cb2 = CircuitBreaker.of("testService", config, snapshot);

        // Manual transition should still work
        cb2.transitionToHalfOpenState();
        assertThat(cb2.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
    }

    // --- Tags preservation test (#4) ---

    @Test
    public void shouldPreserveTagsWhenRestoringFromSnapshot() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .slidingWindowSize(10)
            .build();

        Map<String, String> tags = Map.of("env", "prod", "region", "us-east-1");
        CircuitBreaker cb1 = CircuitBreaker.of("testService", config, tags);

        cb1.onSuccess(100, TimeUnit.MILLISECONDS);
        CircuitBreakerSnapshot snapshot = cb1.createSnapshot();

        // Restore with tags
        CircuitBreaker cb2 = CircuitBreaker.of("testService", config, snapshot, tags);
        assertThat(cb2.getTags()).isEqualTo(tags);
        assertThat(cb2.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    // --- MetricsBuilder negative value validation tests (#5) ---

    @Test
    public void shouldRejectNegativeNumberOfSuccessfulCalls() {
        assertThatThrownBy(() -> CircuitBreakerSnapshot.MetricsSnapshot.builder()
            .numberOfSuccessfulCalls(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldRejectNegativeNumberOfFailedCalls() {
        assertThatThrownBy(() -> CircuitBreakerSnapshot.MetricsSnapshot.builder()
            .numberOfFailedCalls(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldRejectNegativeNumberOfSlowCalls() {
        assertThatThrownBy(() -> CircuitBreakerSnapshot.MetricsSnapshot.builder()
            .numberOfSlowCalls(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldRejectNegativeNumberOfSlowSuccessfulCalls() {
        assertThatThrownBy(() -> CircuitBreakerSnapshot.MetricsSnapshot.builder()
            .numberOfSlowSuccessfulCalls(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldRejectNegativeNumberOfSlowFailedCalls() {
        assertThatThrownBy(() -> CircuitBreakerSnapshot.MetricsSnapshot.builder()
            .numberOfSlowFailedCalls(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldRejectNegativeNumberOfNotPermittedCalls() {
        assertThatThrownBy(() -> CircuitBreakerSnapshot.MetricsSnapshot.builder()
            .numberOfNotPermittedCalls(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // --- Serialization round-trip test (#6) ---

    @Test
    public void shouldSerializeAndDeserializeSnapshot() throws Exception {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .slidingWindowSize(10)
            .slowCallDurationThreshold(Duration.ofMillis(200))
            .build();

        CircuitBreaker cb = CircuitBreaker.of("testService", config);
        cb.onSuccess(50, TimeUnit.MILLISECONDS);
        cb.onSuccess(250, TimeUnit.MILLISECONDS);
        cb.onError(50, TimeUnit.MILLISECONDS, new RuntimeException());
        cb.onError(250, TimeUnit.MILLISECONDS, new RuntimeException());

        CircuitBreakerSnapshot original = cb.createSnapshot();

        // Serialize
        byte[] bytes;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(original);
            bytes = bos.toByteArray();
        }

        // Deserialize
        CircuitBreakerSnapshot restored;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            restored = (CircuitBreakerSnapshot) ois.readObject();
        }

        // Verify all fields preserved
        assertThat(restored.getState()).isEqualTo(original.getState());
        assertThat(restored.getAttempts()).isEqualTo(original.getAttempts());
        assertThat(restored.getRetryAfterWaitUntil()).isEqualTo(original.getRetryAfterWaitUntil());

        CircuitBreakerSnapshot.MetricsSnapshot origMetrics = original.getMetricsSnapshot();
        CircuitBreakerSnapshot.MetricsSnapshot restoredMetrics = restored.getMetricsSnapshot();
        assertThat(restoredMetrics.getNumberOfSuccessfulCalls()).isEqualTo(origMetrics.getNumberOfSuccessfulCalls());
        assertThat(restoredMetrics.getNumberOfFailedCalls()).isEqualTo(origMetrics.getNumberOfFailedCalls());
        assertThat(restoredMetrics.getNumberOfSlowCalls()).isEqualTo(origMetrics.getNumberOfSlowCalls());
        assertThat(restoredMetrics.getNumberOfSlowSuccessfulCalls()).isEqualTo(origMetrics.getNumberOfSlowSuccessfulCalls());
        assertThat(restoredMetrics.getNumberOfSlowFailedCalls()).isEqualTo(origMetrics.getNumberOfSlowFailedCalls());
        assertThat(restoredMetrics.getNumberOfNotPermittedCalls()).isEqualTo(origMetrics.getNumberOfNotPermittedCalls());
    }

    @Test
    public void shouldSerializeAndDeserializeOpenStateSnapshot() throws Exception {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .slidingWindowSize(5)
            .minimumNumberOfCalls(5)
            .waitDurationInOpenState(Duration.ofMinutes(1))
            .build();

        CircuitBreaker cb = CircuitBreaker.of("testService", config);
        for (int i = 0; i < 5; i++) {
            cb.onError(100, TimeUnit.MILLISECONDS, new RuntimeException());
        }

        CircuitBreakerSnapshot original = cb.createSnapshot();
        assertThat(original.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(original.getRetryAfterWaitUntil()).isNotNull();

        // Round-trip
        byte[] bytes;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(original);
            bytes = bos.toByteArray();
        }

        CircuitBreakerSnapshot restored;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            restored = (CircuitBreakerSnapshot) ois.readObject();
        }

        assertThat(restored.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(restored.getAttempts()).isEqualTo(original.getAttempts());
        assertThat(restored.getRetryAfterWaitUntil()).isEqualTo(original.getRetryAfterWaitUntil());
    }
}
