package io.github.resilience4j;

import io.reactivex.Observer;

import static java.util.Objects.requireNonNull;

public abstract class AbstractObserver<T> extends AbstractDisposable implements Observer<T> {

    private final Observer<? super T> downstreamObserver;

    public AbstractObserver(Observer<? super T> downstreamObserver) {
        this.downstreamObserver = requireNonNull(downstreamObserver);
    }

    @Override
    protected void hookOnSubscribe() {
        downstreamObserver.onSubscribe(this);
    }

    @Override
    public void onNext(T item) {
        whenNotDisposed(() -> downstreamObserver.onNext(item));
    }

    @Override
    public void onError(Throwable e) {
        whenNotCompleted(() -> {
            hookOnError(e);
            downstreamObserver.onError(e);
        });
    }

    protected abstract void hookOnError(Throwable e);

    @Override
    public void onComplete() {
        whenNotCompleted(() -> {
            hookOnComplete();
            downstreamObserver.onComplete();
        });
    }

    protected abstract void hookOnComplete();

}
