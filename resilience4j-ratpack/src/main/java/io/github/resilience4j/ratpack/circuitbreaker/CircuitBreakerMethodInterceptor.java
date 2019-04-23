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
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.ratpack.recovery.RecoveryFunction;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import ratpack.exec.Promise;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * A {@link MethodInterceptor} to handle all methods annotated with {@link CircuitBreaker}. It will
 * handle methods that return a Promise, Observable, Flowable, CompletionStage, or value. It will execute the circuit breaker and
 * the fallback found in the annotation.
 */
public class CircuitBreakerMethodInterceptor implements MethodInterceptor {

    @Inject(optional = true)
    @Nullable
    private CircuitBreakerRegistry registry;

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        CircuitBreaker annotation = invocation.getMethod().getAnnotation(CircuitBreaker.class);
        RecoveryFunction<?> recoveryFunction = annotation.recovery().newInstance();
        if (registry == null) {
            registry = CircuitBreakerRegistry.ofDefaults();
        }
        io.github.resilience4j.circuitbreaker.CircuitBreaker breaker = registry.circuitBreaker(annotation.name());
        Class<?> returnType = invocation.getMethod().getReturnType();
        if (Promise.class.isAssignableFrom(returnType)) {
            Promise<?> result = (Promise<?>) proceed(invocation, breaker, recoveryFunction);
            if (result != null) {
                CircuitBreakerTransformer transformer = CircuitBreakerTransformer.of(breaker).recover(recoveryFunction);
                result = result.transform(transformer);
            }
            return result;
        } else if (Flux.class.isAssignableFrom(returnType)) {
            Flux<?> result = (Flux<?>) proceed(invocation, breaker, recoveryFunction);
            if (result != null) {
                CircuitBreakerOperator operator = CircuitBreakerOperator.of(breaker);
                result = recoveryFunction.onErrorResume(result.transform(operator));
            }
            return result;
        } else if (Mono.class.isAssignableFrom(returnType)) {
            Mono<?> result = (Mono<?>) proceed(invocation, breaker, recoveryFunction);
            if (result != null) {
                CircuitBreakerOperator operator = CircuitBreakerOperator.of(breaker);
                result = recoveryFunction.onErrorResume(result.transform(operator));
            }
            return result;
        } else if (CompletionStage.class.isAssignableFrom(returnType)) {
            final CompletableFuture promise = new CompletableFuture<>();
            if (breaker.tryObtainPermission()) {
                CompletionStage<?> result = (CompletionStage<?>) proceed(invocation, breaker, recoveryFunction);
                if (result != null) {
                    long start = System.nanoTime();
                    result.whenComplete((v, t) -> {
                        long durationInNanos = System.nanoTime() - start;
                        if (t != null) {
                            breaker.onError(durationInNanos, t);
                            try {
                                promise.complete(recoveryFunction.apply((Throwable) t));
                            } catch (Exception e) {
                                promise.completeExceptionally(e);
                            }
                        } else {
                            breaker.onSuccess(durationInNanos);
                            promise.complete(v);
                        }
                    });
                }
            } else {
                Throwable t = new CallNotPermittedException(breaker);
                try {
                    promise.complete(recoveryFunction.apply((Throwable) t));
                } catch (Throwable t2) {
                    promise.completeExceptionally(t2);
                }
            }
            return promise;
        }
        return proceed(invocation, breaker, recoveryFunction);
    }

    @Nullable
    private Object proceed(MethodInvocation invocation, io.github.resilience4j.circuitbreaker.CircuitBreaker breaker, RecoveryFunction<?> recoveryFunction) throws Throwable {
        Object result;
        long start = System.nanoTime();
        try {
            result = invocation.proceed();
        } catch (Exception e) {
            long durationInNanos = System.nanoTime() - start;
            breaker.onError(durationInNanos, e);
            return recoveryFunction.apply(e);
        }
        return result;
    }

}
