package io.github.resilience4j.internal;

import org.reactivestreams.Subscription;

/**
 * A disposed subscription.
 */
public enum DisposedSubscription implements Subscription {
    CANCELLED;

    @Override
    public void request(long n) {
        // does nothing as it is disposed
    }

    @Override
    public void cancel() {
        // it is already disposed
    }
}
