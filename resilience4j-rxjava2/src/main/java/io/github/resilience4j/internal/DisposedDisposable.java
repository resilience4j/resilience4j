package io.github.resilience4j.internal;

import io.reactivex.disposables.Disposable;

/**
 * A disposed disposable.
 */
public enum DisposedDisposable implements Disposable {
    DISPOSED;

    @Override
    public void dispose() {
        // it is already disposed
    }

    @Override
    public boolean isDisposed() {
        return true;
    }
}
