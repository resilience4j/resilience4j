package io.github.resilience4j.bulkhead.operator;

import static java.util.Objects.requireNonNull;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.reactivex.CompletableObserver;
import io.reactivex.disposables.Disposable;

/**
 * A RxJava {@link CompletableObserver} to wrap another observer in a bulkhead.
 */
final class BulkheadCompletableObserver extends DisposableBulkhead implements CompletableObserver {
    private final CompletableObserver childObserver;

    BulkheadCompletableObserver(Bulkhead bulkhead, CompletableObserver childObserver) {
        super(bulkhead);
        this.childObserver = requireNonNull(childObserver);
    }

    @Override
    public void onSubscribe(Disposable disposable) {
        setDisposable(disposable);
        if (acquireCallPermit()) {
            childObserver.onSubscribe(this);
        } else {
            dispose();
            childObserver.onSubscribe(this);
            childObserver.onError(bulkheadFullException());
        }
    }

    @Override
    public void onComplete() {
        if (isInvocationPermitted()) {
            releaseBulkhead();
            childObserver.onComplete();
        }
    }

    @Override
    public void onError(Throwable e) {
        if (isInvocationPermitted()) {
            releaseBulkhead();
            childObserver.onError(e);
        }
    }
}
