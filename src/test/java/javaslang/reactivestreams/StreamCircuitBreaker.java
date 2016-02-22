package javaslang.reactivestreams;

import javaslang.circuitbreaker.CircuitBreaker;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.subscriber.MultiSubscriptionSubscriber;
import reactor.rx.StreamSource;

import java.util.Objects;

final class StreamCircuitBreaker<T> extends StreamSource<T, T> {

    private final CircuitBreaker circuitBreaker;

    public StreamCircuitBreaker(Publisher<? extends T> source, CircuitBreaker circuitBreaker) {
        super(source);
        this.circuitBreaker = Objects.requireNonNull(circuitBreaker, "circuitBreaker must not be null");
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        CircuitBreakerSubscriber<T> parent = new CircuitBreakerSubscriber<>(source, s);
        s.onSubscribe(parent);
        source.subscribe(parent);
    }

    final class CircuitBreakerSubscriber<T>
            extends MultiSubscriptionSubscriber<T, T> {

        final Publisher<? extends T> source;

        public CircuitBreakerSubscriber(Publisher<? extends T> source, Subscriber<? super T> actual) {
            super(actual);
            this.source = source;
        }

        @Override
        public void onNext(T t) {
            circuitBreaker.recordSuccess();
            subscriber.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            circuitBreaker.recordFailure(t);
            subscriber.onError(t);
        }
    }
}
