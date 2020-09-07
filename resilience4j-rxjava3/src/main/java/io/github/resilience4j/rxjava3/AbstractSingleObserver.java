package io.github.resilience4j.rxjava3;

import io.reactivex.rxjava3.core.SingleObserver;

import static java.util.Objects.requireNonNull;

public abstract class AbstractSingleObserver<T> extends AbstractDisposable implements
    SingleObserver<T> {

    private final SingleObserver<? super T> downstreamObserver;

    public AbstractSingleObserver(SingleObserver<? super T> downstreamObserver) {
        this.downstreamObserver = requireNonNull(downstreamObserver);
    }

    @Override
    protected void hookOnSubscribe() {
        downstreamObserver.onSubscribe(this);
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
    public void onSuccess(T value) {
        whenNotCompleted(() -> {
            hookOnSuccess();
            downstreamObserver.onSuccess(value);
        });
    }

    protected abstract void hookOnSuccess();

}
