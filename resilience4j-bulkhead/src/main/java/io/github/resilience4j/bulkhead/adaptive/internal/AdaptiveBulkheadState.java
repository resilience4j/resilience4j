package io.github.resilience4j.bulkhead.adaptive.internal;

import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;

import java.util.concurrent.TimeUnit;

interface AdaptiveBulkheadState {

    boolean tryAcquirePermission();

    void acquirePermission();

    void releasePermission();

    void onError(long startTime, TimeUnit timeUnit, Throwable throwable);

    void onSuccess(long startTime, TimeUnit timeUnit);

    AdaptiveBulkhead.State getState();

}
