package io.github.resilience4j.internal;

import org.reactivestreams.Subscription;

/**
 * A disposed subscription.
 */
public enum DisposedSubscription implements Subscription {
    CANCELLED;

    @Override
    public void request(long n) {

    }

    @Override
    public void cancel() {

    }
}
