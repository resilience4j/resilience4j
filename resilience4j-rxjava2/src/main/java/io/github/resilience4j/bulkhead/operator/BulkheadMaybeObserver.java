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
        onSubscribe(disposable, childObserver::onSubscribe, childObserver::onError);
    }

    @Override
    public void onError(Throwable e) {
        onError(e, childObserver::onError);
    }

    @Override
    public void onComplete() {
        onComplete(childObserver::onComplete);
    }

    @Override
    public void onSuccess(T value) {
        onSuccess(value, childObserver::onSuccess);
    }
}
