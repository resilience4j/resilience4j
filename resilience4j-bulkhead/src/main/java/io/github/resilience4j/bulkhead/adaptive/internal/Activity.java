package io.github.resilience4j.bulkhead.adaptive.internal;

import java.util.concurrent.atomic.AtomicBoolean;

final class Activity {

    private final AtomicBoolean active = new AtomicBoolean(true);

    boolean isActive() {
        return active.get();
    }

    boolean tryDeactivate() {
        return active.compareAndSet(true, false);
    }

    @Override
    public String toString() {
        return "Activity{" +
            "active=" + active +
            '}';
    }

}
