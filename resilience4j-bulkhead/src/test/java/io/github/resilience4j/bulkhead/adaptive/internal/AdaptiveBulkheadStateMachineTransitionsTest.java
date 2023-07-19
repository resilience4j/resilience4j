package io.github.resilience4j.bulkhead.adaptive.internal;

import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead.State;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.event.BulkheadOnLimitChangedEvent;
import io.github.resilience4j.bulkhead.adaptive.event.BulkheadOnStateTransitionEvent;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AdaptiveBulkheadStateMachineTransitionsTest {

    private static final Throwable ERROR = new Throwable("test");
    private static final int MULTIPLIER = 2;
    private static final int INITIAL_CONCURRENT_CALLS = 8;
    private static final int MIN_CONCURRENT_CALLS = 2;
    private static final int MAX_CONCURRENT_CALLS = 20;

    private AdaptiveBulkheadStateMachine bulkhead;
    private final List<BulkheadOnLimitChangedEvent> limitChanges = new LinkedList<>();
    private final List<BulkheadOnStateTransitionEvent> stateTransitions = new LinkedList<>();

    @Before
    public void setup() {
        AdaptiveBulkheadConfig config = AdaptiveBulkheadConfig.custom()
            .initialConcurrentCalls(INITIAL_CONCURRENT_CALLS)
            .minConcurrentCalls(MIN_CONCURRENT_CALLS)
            .maxConcurrentCalls(MAX_CONCURRENT_CALLS)
            .minimumNumberOfCalls(2)
            .increaseMultiplier(MULTIPLIER)
            .decreaseMultiplier(1f / MULTIPLIER)
            .build();
        bulkhead = (AdaptiveBulkheadStateMachine) AdaptiveBulkhead.of("test", config);
        bulkhead.getEventPublisher().onLimitChanged(limitChanges::add);
        bulkhead.getEventPublisher().onStateTransition(stateTransitions::add);
    }

    private void onSuccess() {
        bulkhead.onSuccess(bulkhead.getCurrentTimestamp(), bulkhead.getTimestampUnit());
    }

    private void onError() {
        bulkhead.onError(bulkhead.getCurrentTimestamp(), bulkhead.getTimestampUnit(), ERROR);
    }

    @Test
    public void shouldIdleOnError() {
        onError();

        assertThat(limitChanges).isEmpty();
        assertThat(stateTransitions).isEmpty();
        assertThat(bulkhead.getMetrics().getFailureRate()).isEqualTo(-1f);
        assertThat(bulkhead.getMetrics().getSlowCallRate()).isEqualTo(-1f);
    }

    @Test
    public void shouldDecreaseLimitOn2Errors() {
        onError();
        onError();

        assertThat(limitChanges)
            .extracting(BulkheadOnLimitChangedEvent::getNewMaxConcurrentCalls)
            .containsExactly(4)
            .last().isEqualTo(INITIAL_CONCURRENT_CALLS / MULTIPLIER);
        assertThat(stateTransitions)
            .extracting(BulkheadOnStateTransitionEvent::getNewState)
            .containsExactly(State.CONGESTION_AVOIDANCE);
    }

    @Test
    public void shouldDecreaseLimitOn3Errors() {
        onError();
        onError();
        onError();

        assertThat(limitChanges)
            .extracting(BulkheadOnLimitChangedEvent::getNewMaxConcurrentCalls)
            .containsExactly(4, 2)
            .last().isEqualTo(INITIAL_CONCURRENT_CALLS / MULTIPLIER / MULTIPLIER);
        assertThat(stateTransitions)
            .extracting(BulkheadOnStateTransitionEvent::getNewState)
            .containsExactly(State.CONGESTION_AVOIDANCE);
    }

    @Test
    public void shouldIdleOnSuccess() {
        onSuccess();

        assertThat(limitChanges).isEmpty();
        assertThat(stateTransitions).isEmpty();
        assertThat(bulkhead.getMetrics().getFailureRate()).isEqualTo(-1f);
        assertThat(bulkhead.getMetrics().getSlowCallRate()).isEqualTo(-1f);
    }

    @Test
    public void shouldIncreaseLimitOn2Successes() {
        onSuccess();
        onSuccess();

        assertThat(limitChanges)
            .extracting(BulkheadOnLimitChangedEvent::getNewMaxConcurrentCalls)
            .containsExactly(16)
            .last().isEqualTo(INITIAL_CONCURRENT_CALLS * MULTIPLIER);
        assertThat(stateTransitions).isEmpty();
    }

    @Test
    public void shouldIncreaseLimitOn3Successes() {
        onSuccess();
        onSuccess();
        onSuccess();

        assertThat(limitChanges)
            .extracting(BulkheadOnLimitChangedEvent::getNewMaxConcurrentCalls)
            .containsExactly(16, 20)
            .last().isEqualTo(MAX_CONCURRENT_CALLS);
        assertThat(stateTransitions).isEmpty();
    }

    @Test
    public void shouldDecreaseLimitOnManyErrorsAndSuccesses() {
        onError();
        onError();
        onSuccess();
        onSuccess();
        onSuccess();

        assertThat(limitChanges)
            .extracting(BulkheadOnLimitChangedEvent::getNewMaxConcurrentCalls)
            .containsExactly(4, 2)
            .last().isEqualTo(INITIAL_CONCURRENT_CALLS / MULTIPLIER / MULTIPLIER);
        assertThat(stateTransitions)
            .extracting(BulkheadOnStateTransitionEvent::getNewState)
            .containsExactly(State.CONGESTION_AVOIDANCE, State.SLOW_START);
    }

}