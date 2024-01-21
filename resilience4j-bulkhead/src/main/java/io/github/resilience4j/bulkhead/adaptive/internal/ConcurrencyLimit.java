package io.github.resilience4j.bulkhead.adaptive.internal;

interface ConcurrencyLimit {

    boolean isMinimumLimit();

    void decreaseLimit();

    void increaseLimit();

    void incrementLimit();

}
