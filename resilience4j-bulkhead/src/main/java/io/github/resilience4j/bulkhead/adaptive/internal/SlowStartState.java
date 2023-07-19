package io.github.resilience4j.bulkhead.adaptive.internal;

import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;

/**
 * Although the strategy is referred to as slow start, its congestion window growth is quite
 * aggressive, more aggressive than the congestion avoidance phase.
 */
class SlowStartState<T extends StateMachine & ConcurrencyLimit> implements AdaptiveBulkheadState {

    private final StateMachine stateMachine;
    private final ConcurrencyLimit concurrencyLimit;
    private final Activity activity;

    SlowStartState(T stateMachine) {
        this.stateMachine = stateMachine;
        this.concurrencyLimit = stateMachine;
        this.activity = new Activity();
    }

    @Override
    public void onBelowThresholds() {
        concurrencyLimit.increaseLimit();
    }

    /**
     * Transits to CONGESTION_AVOIDANCE state when thresholds have been exceeded.
     */
    @Override
    public void onAboveThresholds() {
        if (activity.tryDeactivate()) {
            concurrencyLimit.decreaseLimit();
            stateMachine.transitionToCongestionAvoidance();
        }
    }

    /**
     * Get the state of the AdaptiveBulkhead
     */
    @Override
    public AdaptiveBulkhead.State getState() {
        return AdaptiveBulkhead.State.SLOW_START;
    }

    @Override
    public boolean isActive() {
        return activity.isActive();
    }

}
