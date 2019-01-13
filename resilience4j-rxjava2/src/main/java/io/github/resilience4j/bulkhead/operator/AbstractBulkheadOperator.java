package io.github.resilience4j.bulkhead.operator;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.internal.PermittedOperator;

import static java.util.Objects.requireNonNull;

/**
 * A disposable bulkhead acting as a base class for bulkhead operators.
 *
 * @param <T>          the type of the emitted event
 * @param <DISPOSABLE> the actual type of the disposable/subscription
 */
abstract class AbstractBulkheadOperator<T, DISPOSABLE> extends PermittedOperator<T, DISPOSABLE> {
    private final Bulkhead bulkhead;

    AbstractBulkheadOperator(Bulkhead bulkhead) {
        this.bulkhead = requireNonNull(bulkhead);
    }

    @Override
    protected boolean tryCallPermit() {
        return bulkhead.isCallPermitted();
    }

    @Override
    protected Exception notPermittedException() {
        return new BulkheadFullException(String.format("Bulkhead '%s' is full", bulkhead.getName()));
    }

    @Override
    protected void doOnSuccess() {
        bulkhead.onComplete();
    }

    @Override
    protected void doOnError(Throwable e) {
        bulkhead.onComplete();
    }

    @Override
    protected void doOnDispose() {
        bulkhead.onComplete();
    }
}
