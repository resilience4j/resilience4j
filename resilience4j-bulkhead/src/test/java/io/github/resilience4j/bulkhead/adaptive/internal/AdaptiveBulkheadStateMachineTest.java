package io.github.resilience4j.bulkhead.adaptive.internal;

import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.event.BulkheadOnLimitChangedEvent;
import io.github.resilience4j.bulkhead.adaptive.event.BulkheadOnStateTransitionEvent;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AdaptiveBulkheadStateMachineTest {

    private static final Throwable ERROR = new Throwable("test");
    private static final int SLOW_CALL_DURATION_THRESHOLD = 200;
    private static final int RATE_THRESHOLD = 50;
    private static final int MIN_CONCURRENT_CALLS = 10;
    private static final int MAX_CONCURRENT_CALLS = 100;

    private AdaptiveBulkheadStateMachine bulkhead;
    private AdaptiveBulkheadMetrics metrics;
    private final List<BulkheadOnLimitChangedEvent> limitChanges = new LinkedList<>();
    private final List<BulkheadOnStateTransitionEvent> stateTransitions = new LinkedList<>();

    @Before
    public void setup() {
        AdaptiveBulkheadConfig config = AdaptiveBulkheadConfig.custom()
            .minConcurrentCalls(MIN_CONCURRENT_CALLS)
            .maxConcurrentCalls(MAX_CONCURRENT_CALLS)
            .initialConcurrentCalls(22)
            .slidingWindowSize(5)
            .slidingWindowType(AdaptiveBulkheadConfig.SlidingWindowType.TIME_BASED)
            .minimumNumberOfCalls(3)
            .failureRateThreshold(RATE_THRESHOLD)
            .slowCallRateThreshold(RATE_THRESHOLD)
            .slowCallDurationThreshold(Duration.ofMillis(SLOW_CALL_DURATION_THRESHOLD))
            .resetMetricsOnTransition(true)
            .build();
        bulkhead = (AdaptiveBulkheadStateMachine) AdaptiveBulkhead.of("test", config);
        bulkhead.getEventPublisher().onLimitChanged(limitChanges::add);
        bulkhead.getEventPublisher().onStateTransition(stateTransitions::add);
        metrics = (AdaptiveBulkheadMetrics) bulkhead.getMetrics();
    }

    private void onSuccess() {
        bulkhead.onSuccess(bulkhead.getCurrentTimestamp());
    }

    private void onError() {
        bulkhead.onError(bulkhead.getCurrentTimestamp(), ERROR);
    }

    @Test
    public void testTransitionToCongestionAvoidance() {
        for (int i = 0; i < MIN_CONCURRENT_CALLS; i++) {
            onSuccess();
        }
        for (int i = 0; i < 2 * MIN_CONCURRENT_CALLS; i++) {
            onError();
        }

        assertThat(limitChanges)
            .extracting(BulkheadOnLimitChangedEvent::getNewMaxConcurrentCalls)
            .containsExactly(44, 88, MAX_CONCURRENT_CALLS, 50, 25, 12, MIN_CONCURRENT_CALLS);
        assertThat(limitChanges)
            .extracting(BulkheadOnLimitChangedEvent::isIncrease)
            .containsExactly(true, true, true, false, false, false, false);
        assertThat(stateTransitions)
            .extracting(BulkheadOnStateTransitionEvent::getNewState)
            .containsExactly(AdaptiveBulkhead.State.CONGESTION_AVOIDANCE);
        assertThat(metrics.getSnapshot().getFailureRate()).isEqualTo(100f);
    }

    @Test
    public void testCongestionAvoidanceBelowThresholds() {
        bulkhead.transitionToCongestionAvoidance();

        for (int i = 0; i < MIN_CONCURRENT_CALLS; i++) {
            onSuccess();
        }

        assertThat(limitChanges)
            .extracting(BulkheadOnLimitChangedEvent::getNewMaxConcurrentCalls)
            .containsExactly(23, 24, 25);
        assertThat(limitChanges)
            .extracting(BulkheadOnLimitChangedEvent::isIncrease)
            .containsExactly(true, true, true);
        assertThat(stateTransitions)
            .extracting(BulkheadOnStateTransitionEvent::getNewState)
            .containsExactly(AdaptiveBulkhead.State.CONGESTION_AVOIDANCE);
        assertThat(metrics.getSnapshot().getFailureRate()).isZero();
    }

    @Test
    public void testCongestionAvoidanceAboveThresholds() {
        bulkhead.transitionToCongestionAvoidance();

        for (int i = 0; i < MIN_CONCURRENT_CALLS; i++) {
            onError();
        }

        assertThat(limitChanges)
            .extracting(BulkheadOnLimitChangedEvent::getNewMaxConcurrentCalls)
            .containsExactly(11, 10);
        assertThat(limitChanges)
            .extracting(BulkheadOnLimitChangedEvent::isIncrease)
            .containsExactly(false, false);
        assertThat(stateTransitions)
            .extracting(BulkheadOnStateTransitionEvent::getNewState)
            .containsExactly(AdaptiveBulkhead.State.CONGESTION_AVOIDANCE);
        assertThat(metrics.getSnapshot().getFailureRate()).isEqualTo(100f);
    }

}