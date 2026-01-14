/*
 * Copyright 2026 Bobae Kim
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.spring6.httpservice;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.functions.CheckedFunction;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Builder to help build stacked decorators. <br>
 *
 * <pre>
 * {
 *     &#64;code
 *     CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("backendName");
 *     RateLimiter rateLimiter = RateLimiter.ofDefaults("backendName");
 *     HttpServiceDecorators decorators = HttpServiceDecorators.builder()
 *             .withCircuitBreaker(circuitBreaker)
 *             .withRateLimiter(rateLimiter)
 *             .build();
 *     MyService myService = Resilience4jHttpService.builder(decorators)
 *             .restClient(restClient)
 *             .build(MyService.class);
 * }
 * </pre>
 * <p>
 * The order in which decorators are applied correspond to the order in which they are declared. For
 * example, calling {@link HttpServiceDecorators.Builder#withFallback(Object)} before {@link
 * HttpServiceDecorators.Builder#withCircuitBreaker(CircuitBreaker)} would mean that the fallback is
 * called when the HTTP request fails, but would no longer be reachable if the CircuitBreaker were
 * open. However, reversing the order would mean that the fallback is called both when the HTTP
 * request fails and when the CircuitBreaker is open. <br> So be wary of this when designing your
 * "resilience" strategy.
 */
public class HttpServiceDecorators implements HttpServiceDecorator {

    private final List<HttpServiceDecorator> decorators;

    private HttpServiceDecorators(List<HttpServiceDecorator> decorators) {
        this.decorators = decorators;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public CheckedFunction<Object[], Object> decorate(CheckedFunction<Object[], Object> fn,
                                                      Method method, HttpServiceTarget<?> target) {
        CheckedFunction<Object[], Object> decoratedFn = fn;
        for (final HttpServiceDecorator decorator : decorators) {
            decoratedFn = decorator.decorate(decoratedFn, method, target);
        }
        return decoratedFn;
    }

    public static final class Builder {

        private final List<HttpServiceDecorator> decorators = new ArrayList<>();

        /**
         * Adds a {@link Retry} to the decorator chain.
         *
         * @param retry a fully configured {@link Retry}.
         * @return the builder
         */
        public Builder withRetry(Retry retry) {
            addDecorator(fn -> Retry.decorateCheckedFunction(retry, fn));
            return this;
        }

        /**
         * Adds a {@link CircuitBreaker} to the decorator chain.
         *
         * @param circuitBreaker a fully configured {@link CircuitBreaker}.
         * @return the builder
         */
        public Builder withCircuitBreaker(CircuitBreaker circuitBreaker) {
            addDecorator(fn -> CircuitBreaker.decorateCheckedFunction(circuitBreaker, fn));
            return this;
        }

        /**
         * Adds a {@link RateLimiter} to the decorator chain.
         *
         * @param rateLimiter a fully configured {@link RateLimiter}.
         * @return the builder
         */
        public Builder withRateLimiter(RateLimiter rateLimiter) {
            addDecorator(fn -> RateLimiter.decorateCheckedFunction(rateLimiter, fn));
            return this;
        }

        /**
         * Adds a {@link Bulkhead} to the decorator chain.
         *
         * @param bulkhead a fully configured {@link Bulkhead}.
         * @return the builder
         */
        public Builder withBulkhead(Bulkhead bulkhead) {
            addDecorator(fn -> Bulkhead.decorateCheckedFunction(bulkhead, fn));
            return this;
        }

        /**
         * Adds a {@link TimeLimiter} to the decorator chain.
         * Note: TimeLimiter works best with CompletionStage return types.
         * For synchronous calls, it wraps the execution in a CompletableFuture.
         *
         * @param timeLimiter a fully configured {@link TimeLimiter}.
         * @param executor    the executor to use for async execution.
         * @return the builder
         */
        public Builder withTimeLimiter(TimeLimiter timeLimiter, ScheduledExecutorService executor) {
            decorators.add(new TimeLimiterDecorator(timeLimiter, executor));
            return this;
        }

        /**
         * Adds a fallback to the decorator chain. Multiple fallbacks can be applied with the next
         * fallback being called when the previous one fails.
         *
         * @param fallback must match the HTTP Service, i.e. the interface specified when
         *                 calling {@link Resilience4jHttpService.Builder#build(Class)}.
         * @return the builder
         */
        public Builder withFallback(Object fallback) {
            decorators.add(new FallbackDecorator<>(new DefaultFallbackHandler<>(fallback)));
            return this;
        }

        /**
         * Adds a fallback factory to the decorator chain. A factory can consume the exception
         * thrown on error. Multiple fallbacks can be applied with the next fallback being called
         * when the previous one fails.
         *
         * @param fallbackFactory must return an instance matching the HTTP Service.
         * @return the builder
         */
        public Builder withFallbackFactory(Function<Exception, ?> fallbackFactory) {
            decorators.add(new FallbackDecorator<>(new FallbackFactory<>(fallbackFactory)));
            return this;
        }

        /**
         * Adds a fallback to the decorator chain. Multiple fallbacks can be applied with the next
         * fallback being called when the previous one fails.
         *
         * @param fallback must match the HTTP Service.
         * @param filter   only {@link Exception}s matching the specified {@link Exception} will
         *                 trigger the fallback.
         * @return the builder
         */
        public Builder withFallback(Object fallback, Class<? extends Exception> filter) {
            decorators.add(new FallbackDecorator<>(new DefaultFallbackHandler<>(fallback), filter));
            return this;
        }

        /**
         * Adds a fallback factory to the decorator chain. A factory can consume the exception
         * thrown on error. Multiple fallbacks can be applied with the next fallback being called
         * when the previous one fails.
         *
         * @param fallbackFactory must return an instance matching the HTTP Service.
         * @param filter          only {@link Exception}s matching the specified {@link Exception}
         *                        will trigger the fallback.
         * @return the builder
         */
        public Builder withFallbackFactory(Function<Exception, ?> fallbackFactory,
                                           Class<? extends Exception> filter) {
            decorators.add(new FallbackDecorator<>(new FallbackFactory<>(fallbackFactory), filter));
            return this;
        }

        /**
         * Adds a fallback to the decorator chain. Multiple fallbacks can be applied with the next
         * fallback being called when the previous one fails.
         *
         * @param fallback must match the HTTP Service.
         * @param filter   the filter must return <code>true</code> for the fallback to be called.
         * @return the builder
         */
        public Builder withFallback(Object fallback, Predicate<Exception> filter) {
            decorators.add(new FallbackDecorator<>(new DefaultFallbackHandler<>(fallback), filter));
            return this;
        }

        /**
         * Adds a fallback factory to the decorator chain. A factory can consume the exception
         * thrown on error. Multiple fallbacks can be applied with the next fallback being called
         * when the previous one fails.
         *
         * @param fallbackFactory must return an instance matching the HTTP Service.
         * @param filter          the filter must return <code>true</code> for the fallback to be
         *                        called.
         * @return the builder
         */
        public Builder withFallbackFactory(Function<Exception, ?> fallbackFactory,
                                           Predicate<Exception> filter) {
            decorators.add(new FallbackDecorator<>(new FallbackFactory<>(fallbackFactory), filter));
            return this;
        }

        private void addDecorator(UnaryOperator<CheckedFunction<Object[], Object>> decorator) {
            decorators.add((fn, m, t) -> {
                // prevent default methods from being decorated
                // as they do not participate in actual web requests
                if (m.isDefault()) {
                    return fn;
                } else {
                    return decorator.apply(fn);
                }
            });
        }

        /**
         * Builds the decorator chain. This can then be used to setup an instance of {@link
         * Resilience4jHttpService}.
         *
         * @return the decorators.
         */
        public HttpServiceDecorators build() {
            return new HttpServiceDecorators(new ArrayList<>(decorators));
        }
    }

    /**
     * Internal decorator for TimeLimiter that handles both sync and async return types.
     */
    private record TimeLimiterDecorator(TimeLimiter timeLimiter,
                                        ScheduledExecutorService executor) implements HttpServiceDecorator {

        @Override
        @SuppressWarnings("unchecked")
        public CheckedFunction<Object[], Object> decorate(
                CheckedFunction<Object[], Object> invocationCall,
                Method method,
                HttpServiceTarget<?> target) {

            if (method.isDefault()) {
                return invocationCall;
            }

            Class<?> returnType = method.getReturnType();

            // For CompletionStage/CompletableFuture return types
            if (CompletionStage.class.isAssignableFrom(returnType)) {
                return args -> {
                    CompletableFuture<Object> future =
                            (CompletableFuture<Object>) invocationCall.apply(args);
                    return timeLimiter.executeCompletionStage(executor, () -> future);
                };
            }

            // For synchronous return types, wrap in future with timeout
            return args -> {
                CompletableFuture<Object> future = CompletableFuture.supplyAsync(
                        () -> {
                            try {
                                return invocationCall.apply(args);
                            } catch (Throwable t) {
                                if (t instanceof RuntimeException) {
                                    throw (RuntimeException) t;
                                }
                                throw new RuntimeException(t);
                            }
                        },
                        executor
                );
                return timeLimiter.executeCompletionStage(executor, () -> future)
                        .toCompletableFuture().join();
            };
        }
    }
}
