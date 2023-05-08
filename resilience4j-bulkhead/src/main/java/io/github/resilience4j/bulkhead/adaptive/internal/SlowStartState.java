package io.github.resilience4j.bulkhead.adaptive.internal;

import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Although the strategy is referred to as slow start, its congestion window growth is quite
 * aggressive, more aggressive than the congestion avoidance phase.
 */
class SlowStartState implements AdaptiveBulkheadState {

    private final AdaptiveBulkheadStateMachine adaptiveBulkheadStateMachine;
    private final AtomicBoolean active;

    SlowStartState(AdaptiveBulkheadStateMachine adaptiveBulkheadStateMachine) {
        this.adaptiveBulkheadStateMachine = adaptiveBulkheadStateMachine;
        this.active = new AtomicBoolean(true);
    }

    @Override
    public boolean tryAcquirePermission() {
        return active.get() && adaptiveBulkheadStateMachine.inner().tryAcquirePermission();
    }

    @Override
    public void acquirePermission() {
        adaptiveBulkheadStateMachine.inner().acquirePermission();
    }

    @Override
    public void releasePermission() {
        adaptiveBulkheadStateMachine.inner().releasePermission();
    }

    @Override
    public void onError(long startTime, TimeUnit timeUnit, Throwable throwable) {
        checkIfThresholdsExceeded(adaptiveBulkheadStateMachine.recordError(startTime, timeUnit));
    }

    @Override
    public void onSuccess(long startTime, TimeUnit timeUnit) {
        checkIfThresholdsExceeded(adaptiveBulkheadStateMachine.recordSuccess(startTime, timeUnit));
    }

    /**
     * Transitions to CONGESTION_AVOIDANCE state when thresholds have been exceeded.
     *
     * @param result the Result
     */
    private void checkIfThresholdsExceeded(AdaptiveBulkheadMetrics.Result result) {
        adaptiveBulkheadStateMachine.logStateDetails(result);
        if (active.get()) {
            switch (result) {
                case BELOW_THRESHOLDS:
                    adaptiveBulkheadStateMachine.increaseConcurrencyLimit();
                    break;
                case ABOVE_THRESHOLDS:
                    if (active.compareAndSet(true, false)) {
                        adaptiveBulkheadStateMachine.decreaseConcurrencyLimit();
                        adaptiveBulkheadStateMachine.transitionToCongestionAvoidance();
                    }
                    break;
            }
        }
    }

    /**
     * Get the state of the AdaptiveBulkhead
     */
    @Override
    public AdaptiveBulkhead.State getState() {
        return AdaptiveBulkhead.State.SLOW_START;
    }

}
