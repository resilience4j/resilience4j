package io.github.resilience4j.bulkhead.operator;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.reactivex.CompletableObserver;
import io.reactivex.disposables.Disposable;

import static java.util.Objects.requireNonNull;

/**
 * A RxJava {@link CompletableObserver} to wrap another observer in a bulkhead.
 */
final class BulkheadCompletableObserver extends DisposableBulkhead implements CompletableObserver {
    private final transient CompletableObserver childObserver;

    BulkheadCompletableObserver(Bulkhead bulkhead, CompletableObserver childObserver) {
        super(bulkhead);
        this.childObserver = requireNonNull(childObserver);
    }

    @Override
    public void onSubscribe(Disposable disposable) {
        onSubscribeWithPermit(disposable);
    }

    @Override
    protected void onSubscribeInner(Disposable disposable) {
        childObserver.onSubscribe(disposable);
    }

    @Override
    public void onComplete() {
        onCompleteInner();
    }

    @Override
    protected void permittedOnComplete() {
        childObserver.onComplete();
    }

    @Override
    public void onError(Throwable e) {
        onErrorInner(e);
    }

    @Override
    protected void permittedOnError(Throwable e) {
        childObserver.onError(e);
    }
}
