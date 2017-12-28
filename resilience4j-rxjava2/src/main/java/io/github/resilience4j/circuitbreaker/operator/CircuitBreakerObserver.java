package io.github.resilience4j.circuitbreaker.operator;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.atomic.AtomicBoolean;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;
import io.github.resilience4j.core.StopWatch;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A RxJava {@link Observer} to protect another observer by a CircuitBreaker.
 *
 * @param <T> the value type of the upstream and downstream
 */
final class CircuitBreakerObserver<T> implements Observer<T>, Disposable {
    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakerObserver.class);
    private final CircuitBreaker circuitBreaker;
    private final Observer<? super T> childObserver;
    private Disposable disposable;
    private AtomicBoolean cancelled = new AtomicBoolean(false);
    private StopWatch stopWatch;

    CircuitBreakerObserver(CircuitBreaker circuitBreaker, Observer<? super T> childObserver) {
        this.circuitBreaker = requireNonNull(circuitBreaker);
        this.childObserver = requireNonNull(childObserver);
    }

    @Override
    public void onSubscribe(Disposable disposable) {
        this.disposable = disposable;
        LOG.debug("onSubscribe");
        if (circuitBreaker.isCallPermitted()) {
            stopWatch = StopWatch.start(circuitBreaker.getName());
            childObserver.onSubscribe(this);
        } else {
            disposable.dispose();
            childObserver.onSubscribe(this);
            childObserver.onError(new CircuitBreakerOpenException(
                    String.format("CircuitBreaker '%s' is open", circuitBreaker.getName())));
        }
    }

    @Override
    public void onNext(T event) {
        LOG.debug("onNext: {}", event);
        if (!isDisposed()) {
            childObserver.onNext(event);
        }
    }

    @Override
    public void onError(Throwable e) {
        LOG.debug("onError", e);
        if (!isDisposed()) {
            circuitBreaker.onError(stopWatch.stop().getProcessingDuration().toNanos(), e);
            childObserver.onError(e);
        }
    }

    @Override
    public void onComplete() {
        LOG.debug("onComplete");
        if (!isDisposed()) {
            circuitBreaker.onSuccess(stopWatch.stop().getProcessingDuration().toNanos());
            childObserver.onComplete();
        }
    }

    @Override
    public void dispose() {
        if (!cancelled.get()) {
            cancelled.set(true);
            disposable.dispose();
        }
    }

    @Override
    public boolean isDisposed() {
        return cancelled.get();
    }
}
