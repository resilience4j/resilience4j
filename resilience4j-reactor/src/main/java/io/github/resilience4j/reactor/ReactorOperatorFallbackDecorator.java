package io.github.resilience4j.reactor;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.retry.MaxRetriesExceededException;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * A decorator that applies the fallback logic for reactive operator.
 * Users need to call {@link #decorate(UnaryOperator)} and provide the operator to decorate after instantiating
 * a {@link ReactorOperatorFallbackDecorator } with the desired fallback publisher.
 *
 * @param <T> the value type of the upstream and downstream
 */
public class ReactorOperatorFallbackDecorator<T> implements UnaryOperator<Publisher<T>> {

    private final Map<Class<? extends Throwable>, Publisher<T>> FALLBACK_PUBLISHER_CACHE = new HashMap<>();


    private ReactorOperatorFallbackDecorator(Class<? extends Throwable> throwableType, Publisher<T> fallback) {
        FALLBACK_PUBLISHER_CACHE.put(throwableType, fallback);
    }

    /**
     * Adds a fallback publisher that will be used if the underline publisher completes with on error of this fallbackThrowable type
     *
     * @param throwableType the type of throwable that triggers the fallback
     * @param fallback      the publisher to subscribe to when a Throwable of type throwableClass is thrown
     * @return the function to apply to the stream (typically by calling {{@link Mono#transformDeferred(Function)}} or {@link Flux#transformDeferred(Function)}
     */
    public ReactorOperatorFallbackDecorator<T> withFallback(
        Class<? extends Throwable> throwableType,
        Publisher<T> fallback
    ) {
        FALLBACK_PUBLISHER_CACHE.put(throwableType, fallback);
        return this;
    }

    /**
     * Initializes a {@link ReactorOperatorFallbackDecorator} that will fallback when the publisher finishes with an error of the provided class.
     *
     * @param throwableType the type of throwable that triggers the fallback
     * @param fallback      the publisher to subscribe to when a Throwable of type throwableClass is thrown
     * @param <T>           the value type of the upstream and downstream
     * @return ReactorOperatorFallbackDecorator that can be used to decorate an operator with fallback
     * @see #decorate(UnaryOperator)
     */
    public static <T> ReactorOperatorFallbackDecorator<T> of(
        Class<? extends Throwable> throwableType,
        Publisher<T> fallback
    ) {
        return new ReactorOperatorFallbackDecorator<>(throwableType, fallback);
    }

    @Override
    public Publisher<T> apply(Publisher<T> publisher) {
        if (publisher instanceof Mono) {
            Mono<T> upstream = (Mono<T>) publisher;
            if (!FALLBACK_PUBLISHER_CACHE.isEmpty()) {
                for (Map.Entry<Class<? extends Throwable>, Publisher<T>> classPublisherEntry : FALLBACK_PUBLISHER_CACHE.entrySet()) {
                    upstream = upstream.onErrorResume(
                        classPublisherEntry.getKey(),
                        throwable -> (Mono<? extends T>) classPublisherEntry.getValue()
                    );
                }
            }
            return upstream;
        } else if (publisher instanceof Flux) {
            Flux<T> upstream = (Flux<T>) publisher;
            if (!FALLBACK_PUBLISHER_CACHE.isEmpty()) {
                for (Map.Entry<Class<? extends Throwable>, Publisher<T>> classPublisherEntry : FALLBACK_PUBLISHER_CACHE.entrySet()) {
                    upstream = upstream.onErrorResume(
                        classPublisherEntry.getKey(),
                        throwable -> classPublisherEntry.getValue()
                    );
                }
            }
            return upstream;
        } else {
            throw new IllegalPublisherException(publisher);
        }
    }

    /**
     * Applies the fallback behavior to the provided operator
     *
     * @param operator the operator to decorate with this fallback
     * @return the function to apply to the stream, typically by calling {@link Mono#transformDeferred(Function)} or {@link Flux#transformDeferred(Function)}
     */
    public Function<Publisher<T>, Publisher<T>> decorate(UnaryOperator<Publisher<T>> operator) {
        return compose(operator);
    }


    /**
     * a convenience method that initializes a default retry fallback behavior (after {@link MaxRetriesExceededException} is thrown
     *
     * @param retryOperator     the operator to decorate with fallbackPublisher
     * @param fallbackPublisher the publisher to use when {@link MaxRetriesExceededException} is thrown
     * @param <T>               the value type of the upstream and downstream
     * @return the function to apply to the stream, typically by calling {@link Mono#transformDeferred(Function)} or {@link Flux#transformDeferred(Function)}
     */
    public static <T> Function<Publisher<T>, Publisher<T>> decorateRetry(
        RetryOperator<T> retryOperator,
        Publisher<T> fallbackPublisher
    ) {
        return of(MaxRetriesExceededException.class, fallbackPublisher)
            .decorate(retryOperator);
    }

    /**
     * a convenience method that initializes a default circuitbreaker fallback behavior (after {@link CallNotPermittedException} is thrown
     *
     * @param circuitBreakerOperator the operator to decorate with fallbackPublisher
     * @param fallbackPublisher      the publisher to use when {@link CallNotPermittedException} is thrown
     * @param <T>                    the value type of the upstream and downstream
     * @return the function to apply to the stream, typically by calling {@link Mono#transformDeferred(Function)} or {@link Flux#transformDeferred(Function)}
     */
    public static <T> Function<Publisher<T>, Publisher<T>> decorateCircuitBreaker(
        CircuitBreakerOperator<T> circuitBreakerOperator,
        Publisher<T> fallbackPublisher
    ) {
        return of(CallNotPermittedException.class, fallbackPublisher)
            .decorate(circuitBreakerOperator);
    }

    /**
     * a convenience method that initializes a default timelimiter fallback behavior (after {@link TimeoutException} is thrown
     *
     * @param timeLimiterOperator the operator to decorate with fallbackPublisher
     * @param fallbackPublisher   the publisher to use when {@link TimeoutException} is thrown
     * @param <T>                 the value type of the upstream and downstream
     * @return the function to apply to the stream, typically by calling {@link Mono#transformDeferred(Function)} or {@link Flux#transformDeferred(Function)}
     */
    public static <T> Function<Publisher<T>, Publisher<T>> decorateTimeLimiter(
        TimeLimiterOperator<T> timeLimiterOperator,
        Publisher<T> fallbackPublisher
    ) {
        return of(TimeoutException.class, fallbackPublisher)
            .decorate(timeLimiterOperator);
    }
}
