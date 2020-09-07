package io.github.resilience4j.rxjava3;

import io.reactivex.rxjava3.core.CompletableObserver;

import static java.util.Objects.requireNonNull;

public abstract class AbstractCompletableObserver extends AbstractDisposable implements
    CompletableObserver {

    private final CompletableObserver downstreamObserver;

    public AbstractCompletableObserver(CompletableObserver downstreamObserver) {
        this.downstreamObserver = requireNonNull(downstreamObserver);
    }

    @Override
    protected void hookOnSubscribe() {
        downstreamObserver.onSubscribe(this);
    }


    @Override
    public void onComplete() {
        whenNotCompleted(() -> {
            hookOnComplete();
            downstreamObserver.onComplete();
        });
    }

    protected abstract void hookOnComplete();

    @Override
    public void onError(Throwable e) {
        whenNotCompleted(() -> {
            hookOnError(e);
            downstreamObserver.onError(e);
        });
    }

    protected abstract void hookOnError(Throwable e);


}
