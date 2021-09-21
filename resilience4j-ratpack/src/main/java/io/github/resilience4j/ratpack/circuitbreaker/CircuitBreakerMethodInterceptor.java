/*
 * Copyright 2017 Dan Maas
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
package io.github.resilience4j.ratpack.circuitbreaker;

import com.google.inject.Inject;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.ratpack.internal.AbstractMethodInterceptor;
import io.github.resilience4j.ratpack.recovery.DefaultRecoveryFunction;
import io.github.resilience4j.ratpack.recovery.RecoveryFunction;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import ratpack.exec.Promise;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static io.github.resilience4j.circuitbreaker.CallNotPermittedException.createCallNotPermittedException;

/**
 * A {@link MethodInterceptor} to handle all methods annotated with {@link CircuitBreaker}. It will
 * handle methods that return a {@link Promise}, {@link reactor.core.publisher.Flux}, {@link
 * reactor.core.publisher.Mono}, {@link java.util.concurrent.CompletionStage}, or value.
 * <p>
 * The CircuitBreakerRegistry is used to retrieve an instance of a CircuitBreaker for a specific
 * name.
 * <p>
 * Given a method like this:
 * <pre><code>
 *     {@literal @}CircuitBreaker(name = "myService")
 *     public String fancyName(String name) {
 *         return "Sir Captain " + name;
 *     }
 * </code></pre>
 * each time the {@code #fancyName(String)} method is invoked, the method's execution will pass
 * through a a {@link io.github.resilience4j.circuitbreaker.CircuitBreaker} according to the given
 * config.
 * <p>
 * The fallbackMethod parameter signature must match either:
 * <p>
 * 1) The method parameter signature on the annotated method or 2) The method parameter signature
 * with a matching exception type as the last parameter on the annotated method
 * <p>
 * The return value can be a {@link Promise}, {@link java.util.concurrent.CompletionStage}, {@link
 * reactor.core.publisher.Flux}, {@link reactor.core.publisher.Mono}, or an object value. Other
 * reactive types are not supported.
 * <p>
 * If the return value is one of the reactive types listed above, it must match the return value
 * type of the annotated method.
 */
public class CircuitBreakerMethodInterceptor extends AbstractMethodInterceptor {

    @Inject(optional = true)
    @Nullable
    private CircuitBreakerRegistry registry;

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        CircuitBreaker annotation = invocation.getMethod().getAnnotation(CircuitBreaker.class);
        if (annotation == null) {
            annotation = invocation.getMethod().getDeclaringClass()
                .getAnnotation(CircuitBreaker.class);
        }
        final RecoveryFunction<?> fallbackMethod = Optional
            .ofNullable(createRecoveryFunction(invocation, annotation.fallbackMethod()))
            .orElse(new DefaultRecoveryFunction<>());
        if (registry == null) {
            registry = CircuitBreakerRegistry.ofDefaults();
        }
        io.github.resilience4j.circuitbreaker.CircuitBreaker breaker = registry
            .circuitBreaker(annotation.name());
        Class<?> returnType = invocation.getMethod().getReturnType();
        if (Promise.class.isAssignableFrom(returnType)) {
            Promise<?> result = (Promise<?>) proceed(invocation, breaker);
            if (result != null) {
                CircuitBreakerTransformer transformer = CircuitBreakerTransformer.of(breaker)
                    .recover(fallbackMethod);
                result = result.transform(transformer);
            }
            return result;
        } else if (Flux.class.isAssignableFrom(returnType)) {
            Flux<?> result = (Flux<?>) proceed(invocation, breaker);
            if (result != null) {
                CircuitBreakerOperator operator = CircuitBreakerOperator.of(breaker);
                result = fallbackMethod.onErrorResume(result.transform(operator));
            }
            return result;
        } else if (Mono.class.isAssignableFrom(returnType)) {
            Mono<?> result = (Mono<?>) proceed(invocation, breaker);
            if (result != null) {
                CircuitBreakerOperator operator = CircuitBreakerOperator.of(breaker);
                result = fallbackMethod.onErrorResume(result.transform(operator));
            }
            return result;
        } else if (CompletionStage.class.isAssignableFrom(returnType)) {
            final CompletableFuture promise = new CompletableFuture<>();
            if (breaker.tryAcquirePermission()) {
                CompletionStage<?> result = (CompletionStage<?>) proceed(invocation, breaker);
                if (result != null) {
                    long start = System.nanoTime();
                    result.whenComplete((v, t) -> {
                        long durationInNanos = System.nanoTime() - start;
                        if (t != null) {
                            breaker.onError(durationInNanos, TimeUnit.NANOSECONDS, t);
                            completeFailedFuture(t, fallbackMethod, promise);
                        } else {
                            breaker.onResult(durationInNanos, TimeUnit.NANOSECONDS, v);
                            promise.complete(v);
                        }
                    });
                }
            } else {
                Throwable t = createCallNotPermittedException(breaker);
                completeFailedFuture(t, fallbackMethod, promise);
            }
            return promise;
        } else {
            return handleProceedWithException(invocation, breaker, fallbackMethod);
        }
    }

    @Nullable
    private Object proceed(MethodInvocation invocation,
        io.github.resilience4j.circuitbreaker.CircuitBreaker breaker) throws Throwable {
        Class<?> returnType = invocation.getMethod().getReturnType();
        Object result;
        long start = System.nanoTime();
        try {
            result = invocation.proceed();
        } catch (Exception e) {
            long durationInNanos = System.nanoTime() - start;
            breaker.onError(durationInNanos, TimeUnit.NANOSECONDS, e);
            if (Promise.class.isAssignableFrom(returnType)) {
                return Promise.error(e);
            } else if (Flux.class.isAssignableFrom(returnType)) {
                return Flux.error(e);
            } else if (Mono.class.isAssignableFrom(returnType)) {
                return Mono.error(e);
            } else if (CompletionStage.class.isAssignableFrom(returnType)) {
                CompletableFuture<?> future = new CompletableFuture<>();
                future.completeExceptionally(e);
                return future;
            } else {
                throw e;
            }
        }
        return result;
    }

    @Nullable
    private Object handleProceedWithException(MethodInvocation invocation,
        io.github.resilience4j.circuitbreaker.CircuitBreaker breaker,
        RecoveryFunction<?> recoveryFunction) throws Throwable {
        try {
            return io.github.resilience4j.circuitbreaker.CircuitBreaker.decorateCheckedSupplier(breaker, invocation::proceed).get();
        } catch (Throwable throwable) {
            return recoveryFunction.apply(throwable);
        }
    }
}
