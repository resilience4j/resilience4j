package io.github.resilience4j.bulkhead.adaptive.internal;

import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;
import io.github.resilience4j.bulkhead.event.BulkheadOnLimitDecreasedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnLimitIncreasedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnStateTransitionEvent;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class AdaptiveBulkheadStateMachineTest {

    private static final int SLOW_CALL_DURATION_THRESHOLD = 200;
    public static final int RATE_THRESHOLD = 50;

    private AdaptiveBulkheadStateMachine bulkhead;
    private final List<BulkheadOnLimitIncreasedEvent> limitIncreases = new LinkedList<>();
    private final List<BulkheadOnLimitDecreasedEvent> limitDecreases = new LinkedList<>();
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
        bulkhead.getEventPublisher().onLimitIncreased(limitIncreases::add);
        bulkhead.getEventPublisher().onLimitDecreased(limitDecreases::add);
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
        
        assertThat(limitIncreases)
            .extracting(BulkheadOnLimitIncreasedEvent::getNewMaxConcurrentCalls)
            .containsExactly(24, 48, 96);
        assertThat(limitDecreases)
            .extracting(BulkheadOnLimitDecreasedEvent::getNewMaxConcurrentCalls)
            .containsExactly(48, 24, 12);
        assertThat(stateTransitions)
            .extracting(BulkheadOnStateTransitionEvent::getToState)
            .containsExactly(AdaptiveBulkhead.State.CONGESTION_AVOIDANCE);
    }

    @Test
    public void testCongestionAvoidanceBelowThresholds() {
        bulkhead.transitionToCongestionAvoidance();

        for (int i = 0; i < 10; i++) {
            onSuccess();
        }

        assertThat(limitDecreases)
            .isEmpty();
        assertThat(limitIncreases)
            .extracting(BulkheadOnLimitIncreasedEvent::getNewMaxConcurrentCalls)
            .containsExactly(13, 14, 15);
        assertThat(stateTransitions)
            .extracting(BulkheadOnStateTransitionEvent::getToState)
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

        assertThat(limitIncreases)
            .isEmpty();
        assertThat(limitDecreases)
            .extracting(BulkheadOnLimitDecreasedEvent::getNewMaxConcurrentCalls)
            .containsExactly(6, 3, 2);
        assertThat(stateTransitions)
            .extracting(BulkheadOnStateTransitionEvent::getToState)
            .containsExactly(AdaptiveBulkhead.State.CONGESTION_AVOIDANCE);
        assertThat(bulkhead.getMetrics().getFailureRate()).isEqualTo(-1f);
    }

    private void onSuccess() {
        bulkhead.onSuccess(1, TimeUnit.MILLISECONDS);
    }

    private void onError(Throwable failure) {
        bulkhead.onError(System.currentTimeMillis(), TimeUnit.MILLISECONDS, failure);
    }

}