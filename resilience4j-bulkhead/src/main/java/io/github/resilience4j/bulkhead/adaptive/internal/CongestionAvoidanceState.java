package io.github.resilience4j.bulkhead.adaptive.internal;

import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;

class CongestionAvoidanceState<T extends StateMachine & ConcurrencyLimit> implements AdaptiveBulkheadState {

    private final StateMachine stateMachine;
    private final ConcurrencyLimit concurrencyLimit;
    private final Activity activity;

    CongestionAvoidanceState(T stateMachine) {
        this.stateMachine = stateMachine;
        this.concurrencyLimit = stateMachine;
        this.activity = new Activity();
    }

    /**
     * Transits to SLOW_START state when Minimum Concurrency Limit have been reached.
     */
    @Override
    public void onBelowThresholds() {
        if (concurrencyLimit.isMinimumLimit()) {
            if (activity.tryDeactivate()) {
                stateMachine.transitionToSlowStart();
            }
        } else {
            concurrencyLimit.incrementLimit();
        }
    }

    @Override
    public void onAboveThresholds() {
        concurrencyLimit.decreaseLimit();
    }

    /**
     * Get the state of the AdaptiveBulkhead
     */
    @Override
    public AdaptiveBulkhead.State getState() {
        return AdaptiveBulkhead.State.CONGESTION_AVOIDANCE;
    }

    @Override
    public boolean isActive() {
        return activity.isActive();
    }

}
