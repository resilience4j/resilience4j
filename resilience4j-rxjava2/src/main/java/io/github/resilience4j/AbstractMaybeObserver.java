package io.github.resilience4j;

import io.reactivex.MaybeObserver;

import static java.util.Objects.requireNonNull;

public abstract class AbstractMaybeObserver<T> extends AbstractDisposable implements
    MaybeObserver<T> {

    private final MaybeObserver<? super T> downstreamObserver;

    public AbstractMaybeObserver(MaybeObserver<? super T> downstreamObserver) {
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

    @Override
    public void onComplete() {
        whenNotCompleted(() -> {
            hookOnComplete();
            downstreamObserver.onComplete();
        });
    }

    protected abstract void hookOnComplete();

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
