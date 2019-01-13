package io.github.resilience4j.bulkhead.operator;

import static java.util.Objects.requireNonNull;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.reactivex.disposables.Disposable;

/**
 * A disposable bulkhead acting as a base class for bulkhead operators.
 *
 * @param <T> the type of the emitted event
 */
abstract class DisposableBulkhead<T> extends AbstractBulkheadOperator<T, Disposable> implements Disposable {

    DisposableBulkhead(Bulkhead bulkhead) {
        super(bulkhead);
    }

    @Override
    public final void dispose() {
        super.dispose();
    }

    @Override
    public final boolean isDisposed() {
        return super.isDisposed();
    }

    @Override
    protected Disposable getDisposedDisposable() {
        return DisposedDisposable.DISPOSED;
    }

    @Override
    protected Disposable getDisposable() {
        return this;
    }

    @Override
    protected void dispose(Disposable disposable) {
        disposable.dispose();
    }

    private enum DisposedDisposable implements Disposable {
        DISPOSED;

        @Override
        public void dispose() {

        }

        @Override
        public boolean isDisposed() {
            return true;
        }
    }
}
