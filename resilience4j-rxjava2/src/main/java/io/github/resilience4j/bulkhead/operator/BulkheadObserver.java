package io.github.resilience4j.bulkhead.operator;

import static java.util.Objects.requireNonNull;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * A RxJava {@link Observer} to wrap another observer in a bulkhead.
 *
 * @param <T> the value type of the upstream and downstream
 */
final class BulkheadObserver<T> extends DisposableBulkhead implements Observer<T> {
    private final Observer<? super T> childObserver;

    BulkheadObserver(Bulkhead bulkhead, Observer<? super T> childObserver) {
        super(bulkhead);
        this.childObserver = requireNonNull(childObserver);
    }

    @Override
    public void onSubscribe(Disposable disposable) {
        onSubscribe(disposable, childObserver::onSubscribe, childObserver::onError);
    }

    @Override
    public void onNext(T event) {
        if (isInvocationPermitted()) {
            childObserver.onNext(event);
        }
    }

    @Override
    public void onError(Throwable e) {
        onError(e, childObserver::onError);
    }

    @Override
    public void onComplete() {
        onComplete(childObserver::onComplete);
    }
}
