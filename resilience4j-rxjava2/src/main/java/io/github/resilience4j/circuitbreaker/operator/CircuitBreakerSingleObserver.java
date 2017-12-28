package io.github.resilience4j.circuitbreaker.operator;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.atomic.AtomicBoolean;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;
import io.github.resilience4j.core.StopWatch;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A RxJava {@link SingleObserver} to protect another observer by a CircuitBreaker.
 *
 * @param <T> the value type of the upstream and downstream
 */
class CircuitBreakerSingleObserver<T> implements SingleObserver<T>, Disposable {
    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakerSingleObserver.class);
    private final CircuitBreaker circuitBreaker;
    private final SingleObserver<? super T> childObserver;
    private Disposable disposable;
    private AtomicBoolean cancelled = new AtomicBoolean(false);
    private StopWatch stopWatch;


    CircuitBreakerSingleObserver(CircuitBreaker circuitBreaker, SingleObserver<? super T> childObserver) {
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
    public void onError(Throwable e) {
        LOG.debug("onError", e);
        if (!isDisposed()) {
            circuitBreaker.onError(stopWatch.stop().getProcessingDuration().toNanos(), e);
            childObserver.onError(e);
        }
    }

    @Override
    public void onSuccess(T value) {
        LOG.debug("onComplete");
        if (!isDisposed()) {
            circuitBreaker.onSuccess(stopWatch.stop().getProcessingDuration().toNanos());
            childObserver.onSuccess(value);
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
