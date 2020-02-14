package io.github.resilience4j.feign;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.CompletionStageUtils;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.vavr.NotImplementedError;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.*;

// TODO WIP
public class PostponedDecorators<T> {

    private final List<UnaryOperator<Supplier<CompletionStage<T>>>> operations;
    @Nullable
    private Function<Supplier<T>, Supplier<CompletionStage<T>>> completionStageWrapper;

    public static PostponedDecorators<?> builder() {
        return new PostponedDecorators<>();
    }

    private PostponedDecorators() {
        this.operations = new ArrayList<>();
    }

    public PostponedDecorators<T> withThreadPoolBulkhead(ThreadPoolBulkhead threadPoolBulkhead) {
        if (this.completionStageWrapper != null) {
            throw new IllegalStateException(
                "CompletionStageWrapper (threadPoolBulkhead) already defined!");
        }
        this.completionStageWrapper = threadPoolBulkhead::decorateSupplier;
        return this;
    }

    public PostponedDecorators<T> withCircuitBreaker(CircuitBreaker circuitBreaker) {
        operations.add(supplier ->
            CircuitBreaker.decorateCompletionStage(circuitBreaker, supplier));
        return this;
    }

    public PostponedDecorators<T> withRetry(Retry retryContext,
        ScheduledExecutorService scheduler) {
        operations.add(supplier ->
            Retry.decorateCompletionStage(retryContext, scheduler, supplier));
        return this;
    }

    public PostponedDecorators<T> withBulkhead(Bulkhead bulkhead) {
        operations.add(supplier -> Bulkhead.decorateCompletionStage(bulkhead, supplier));
        return this;
    }

    public PostponedDecorators<T> withTimeLimiter(TimeLimiter timeLimiter,
        ScheduledExecutorService scheduler) {
        operations.add(supplier -> timeLimiter.decorateCompletionStage(scheduler, supplier));
        return this;
    }

    public PostponedDecorators<T> withRateLimiter(RateLimiter rateLimiter) {
        return withRateLimiter(rateLimiter, 1);
    }

    public PostponedDecorators<T> withRateLimiter(RateLimiter rateLimiter, int permits) {
        operations.add(supplier ->
            RateLimiter.decorateCompletionStage(rateLimiter, permits, supplier));
        return this;
    }

    public PostponedDecorators<T> withFallback(Predicate<T> resultPredicate,
        UnaryOperator<T> resultHandler) {
        operations.add(supplier ->
            CompletionStageUtils.recover(supplier, resultPredicate, resultHandler));
        return this;
    }

    public PostponedDecorators<T> withFallback(
        BiFunction<T, Throwable, T> handler) {
        operations.add(supplier -> CompletionStageUtils.andThen(supplier, handler));
        return this;
    }

    public PostponedDecorators<T> withFallback(List<Class<? extends Throwable>> exceptionTypes,
        Function<Throwable, T> exceptionHandler) {
        operations.add(supplier ->
            CompletionStageUtils.recover(supplier, exceptionTypes, exceptionHandler));
        return this;
    }

    public PostponedDecorators<T> withFallback(Function<Throwable, T> exceptionHandler) {
        operations.add(supplier -> CompletionStageUtils.recover(supplier, exceptionHandler));
        return this;
    }

    public <X extends Throwable> PostponedDecorators<T> withFallback(Class<X> exceptionType,
        Function<Throwable, T> exceptionHandler) {
        operations.add(supplier ->
            CompletionStageUtils.recover(supplier, exceptionType, exceptionHandler));
        return this;
    }

    public PostponedDecorators<T> withFallbackFactory(Function<Exception, ?> fallbackFactory) {
        throw new NotImplementedError("withFallbackFactory");
//        decorators.add(new FallbackDecorator<>(new FallbackFactory<>(fallbackFactory)));
//        return this;
    }

    public PostponedDecorators<T> withFallback(Object fallback) {
        throw new NotImplementedError("withFallback Object");
//        decorators.add(new FallbackDecorator<>(new DefaultFallbackHandler<>(fallback)));
//        return this;
    }

    public Supplier<CompletionStage<T>> build(Supplier<T> supplier) {
        if (completionStageWrapper != null) {
            return compose(completionStageWrapper.apply(supplier));
        } else {
            return compose(() -> CompletableFuture.completedFuture(supplier.get()));
        }
    }


    private Supplier<CompletionStage<T>> compose(Supplier<CompletionStage<T>> supplier) {
        return operations.stream().reduce(
            (UnaryOperator<Supplier<CompletionStage<T>>>) x -> x,
            (a, b) -> b.compose(a)::apply,
            (ignore, me) -> null
        ).apply(supplier);
    }

}
