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

    private static final int SLOW_CALL_DURATION_THRESHOLD = 200;
    public static final int RATE_THRESHOLD = 50;

    private AdaptiveBulkheadStateMachine bulkhead;
    private final List<BulkheadOnLimitChangedEvent> limitChanges = new LinkedList<>();
    private final List<BulkheadOnStateTransitionEvent> stateTransitions = new LinkedList<>();

    @Before
    public void setup() {
        AdaptiveBulkheadConfig config = AdaptiveBulkheadConfig.custom()
            .maxConcurrentCalls(100)
            .minConcurrentCalls(2)
            .initialConcurrentCalls(12)
            .slidingWindowSize(5)
            .slidingWindowType(AdaptiveBulkheadConfig.SlidingWindowType.TIME_BASED)
            .minimumNumberOfCalls(3)
            .failureRateThreshold(RATE_THRESHOLD)
            .slowCallRateThreshold(RATE_THRESHOLD)
            .slowCallDurationThreshold(Duration.ofMillis(SLOW_CALL_DURATION_THRESHOLD))
            .build();
        bulkhead = (AdaptiveBulkheadStateMachine) AdaptiveBulkhead.of("test", config);
        bulkhead.getEventPublisher().onLimitChanged(limitChanges::add);
        bulkhead.getEventPublisher().onStateTransition(stateTransitions::add);
    }

    @Test
    public void testTransitionToCongestionAvoidance() {
        Throwable failure = new Throwable();

        for (int i = 0; i < 10; i++) {
            onSuccess();
        }
        for (int i = 0; i < 10; i++) {
            onError(failure);
        }

        assertThat(limitChanges)
            .extracting(BulkheadOnLimitChangedEvent::getNewMaxConcurrentCalls)
            .containsExactly(24, 48, 96, 48, 24, 12);
        assertThat(limitChanges)
            .extracting(BulkheadOnLimitChangedEvent::isIncrease)
            .containsExactly(true, true, true, false, false, false);
        assertThat(stateTransitions)
            .extracting(BulkheadOnStateTransitionEvent::getNewState)
            .containsExactly(AdaptiveBulkhead.State.CONGESTION_AVOIDANCE);
    }

    @Test
    public void testCongestionAvoidanceBelowThresholds() {
        bulkhead.transitionToCongestionAvoidance();

        for (int i = 0; i < 10; i++) {
            onSuccess();
        }

        assertThat(limitChanges)
            .extracting(BulkheadOnLimitChangedEvent::getNewMaxConcurrentCalls)
            .containsExactly(13, 14, 15);
        assertThat(limitChanges)
            .extracting(BulkheadOnLimitChangedEvent::isIncrease)
            .containsExactly(true, true, true);
        assertThat(stateTransitions)
            .extracting(BulkheadOnStateTransitionEvent::getNewState)
            .containsExactly(AdaptiveBulkhead.State.CONGESTION_AVOIDANCE);
        assertThat(bulkhead.getMetrics().getFailureRate()).isEqualTo(-1f);
    }

    @Test
    public void testCongestionAvoidanceAboveThresholds() {
        Throwable failure = new Throwable();
        bulkhead.transitionToCongestionAvoidance();

        for (int i = 0; i < 10; i++) {
            onError(failure);
        }

        assertThat(limitChanges)
            .extracting(BulkheadOnLimitChangedEvent::getNewMaxConcurrentCalls)
            .containsExactly(6, 3, 2);
        assertThat(limitChanges)
            .extracting(BulkheadOnLimitChangedEvent::isIncrease)
            .containsExactly(false, false, false);
        assertThat(stateTransitions)
            .extracting(BulkheadOnStateTransitionEvent::getNewState)
            .containsExactly(AdaptiveBulkhead.State.CONGESTION_AVOIDANCE);
        assertThat(bulkhead.getMetrics().getFailureRate()).isEqualTo(-1f);
    }

    private void onSuccess() {
        bulkhead.onSuccess(bulkhead.getCurrentTimestamp(), bulkhead.getTimestampUnit());
    }

    private void onError(Throwable failure) {
        bulkhead.onError(bulkhead.getCurrentTimestamp(), bulkhead.getTimestampUnit(), failure);
    }

}