package io.github.resilience4j.bulkhead.adaptive.internal;

import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;
import io.github.resilience4j.bulkhead.event.AdaptiveBulkheadEvent;

import java.util.concurrent.TimeUnit;

interface AdaptiveBulkheadState {

    boolean tryAcquirePermission();

    void acquirePermission();

    void releasePermission();

    void onError(long duration, TimeUnit durationUnit, Throwable throwable);

    void onSuccess(long duration, TimeUnit durationUnit);

    AdaptiveBulkhead.State getState();

    AdaptiveBulkheadMetrics getMetrics();

    /**
     * Should the AdaptiveBulkhead in this state publish events
     *
     * @return a boolean signaling if the events should be published
     */
    default boolean shouldPublishEvents(AdaptiveBulkheadEvent event) {
        return event.getEventType().forcePublish || getState().allowPublish;
    }

}
