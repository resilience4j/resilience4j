package io.github.resilience4j.bulkhead.adaptive.internal;

import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class CongestionAvoidanceState implements AdaptiveBulkheadState {

    private final AdaptiveBulkheadStateMachine adaptiveBulkheadStateMachine;
    private final AtomicBoolean active;

    CongestionAvoidanceState(AdaptiveBulkheadStateMachine adaptiveBulkheadStateMachine) {
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
     * Transitions to SLOW_START state when Minimum Concurrency Limit have been reached.
     *
     * @param result the Result
     */
    private void checkIfThresholdsExceeded(AdaptiveBulkheadMetrics.Result result) {
        adaptiveBulkheadStateMachine.logStateDetails(result);
        if (active.get()) {
            switch (result) {
                case BELOW_THRESHOLDS:
                    if (adaptiveBulkheadStateMachine.isConcurrencyLimitTooLow()) {
                        if (active.compareAndSet(true, false)) {
                            adaptiveBulkheadStateMachine.transitionToSlowStart();
                        }
                    } else {
                        adaptiveBulkheadStateMachine.incrementConcurrencyLimit();
                    }
                    break;
                case ABOVE_THRESHOLDS:
                    adaptiveBulkheadStateMachine.decreaseConcurrencyLimit();
                    break;
            }
        }
    }

    /**
     * Get the state of the AdaptiveBulkhead
     */
    @Override
    public AdaptiveBulkhead.State getState() {
        return AdaptiveBulkhead.State.CONGESTION_AVOIDANCE;
    }

}
