package io.github.resilience4j.bulkhead.operator;

import static java.util.Objects.requireNonNull;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.reactivex.MaybeObserver;
import io.reactivex.disposables.Disposable;

/**
 * A RxJava {@link MaybeObserver} to wrap another observer in a bulkhead.
 *
 * @param <T> the value type of the upstream and downstream
 */
final class BulkheadMaybeObserver<T> extends DisposableBulkhead implements MaybeObserver<T> {
    private final MaybeObserver<? super T> childObserver;

    BulkheadMaybeObserver(Bulkhead bulkhead, MaybeObserver<? super T> childObserver) {
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
    public void onError(Throwable e) {
        if (isInvocationPermitted()) {
            releaseBulkhead();
            childObserver.onError(e);
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
    public void onSuccess(T value) {
        if (isInvocationPermitted()) {
            releaseBulkhead();
            childObserver.onSuccess(value);
        }
    }
}
