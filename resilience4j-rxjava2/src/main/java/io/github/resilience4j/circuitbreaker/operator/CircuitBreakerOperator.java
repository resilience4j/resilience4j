package io.github.resilience4j.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.reactivex.*;
import org.reactivestreams.Publisher;

import static java.util.Objects.requireNonNull;

public class CircuitBreakerTransformer<T> implements FlowableTransformer<T, T>, SingleTransformer<T, T>, MaybeTransformer<T, T>, CompletableTransformer{

    private final CircuitBreaker circuitBreaker;

    /**
     * Creates a CircuitBreakerTransformer.
     *
     * @param circuitBreaker the CircuitBreaker
     * @return a CircuitBreakerOperator
     */
    public static CircuitBreakerTransformer of(CircuitBreaker circuitBreaker) {
        return new CircuitBreakerTransformer<>(circuitBreaker);
    }

    private CircuitBreakerTransformer(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = requireNonNull(circuitBreaker);
    }

    @Override
    public Publisher<T> apply(Flowable<T> upstream) {
        return new FlowableCircuitBreakerOperator<>(upstream, circuitBreaker);
    }


    @Override
    public SingleSource<T> apply(Single<T> upstream) {
        return new SingleCircuitBreakerOperator<>(upstream, circuitBreaker);
    }

    @Override
    public CompletableSource apply(Completable upstream) {
        return new CompletableCircuitBreakerOperator(upstream, circuitBreaker);
    }

    @Override
    public MaybeSource<T> apply(Maybe<T> upstream) {
        return new MaybeCircuitBreakerOperator<>(upstream, circuitBreaker);
    }
}
