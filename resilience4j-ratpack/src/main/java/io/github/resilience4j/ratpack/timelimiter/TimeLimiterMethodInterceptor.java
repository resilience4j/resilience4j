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

package io.github.resilience4j.ratpack.timelimiter;

import com.google.inject.Inject;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.ratpack.internal.AbstractMethodInterceptor;
import io.github.resilience4j.ratpack.recovery.DefaultRecoveryFunction;
import io.github.resilience4j.ratpack.recovery.RecoveryFunction;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import ratpack.exec.Execution;
import ratpack.exec.Promise;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;

/**
 * A {@link MethodInterceptor} to handle all methods annotated with {@link TimeLimiter}. It will
 * handle methods that return a {@link Promise}, {@link Flux}, {@link
 * Mono}, {@link CompletionStage}, or value.
 * <p>
 * Given a method like this:
 * <pre><code>
 *     {@literal @}TimeLimiter(name = "myService")
 *     public String fancyName(String name) {
 *         return "Sir Captain " + name;
 *     }
 * </code></pre>
 * each time the {@code #fancyName(String)} method is invoked, the method's execution will pass
 * through a a {@link TimeLimiter} according to the given
 * config.
 * <p>
 * The fallbackMethod signature must match either:
 * <p>
 * 1) The method parameter signature on the annotated method or 2) The method parameter signature
 * with a matching exception type as the last parameter on the annotated method
 * <p>
 * The return value can be a {@link Promise}, {@link CompletionStage}, {@link
 * Flux}, {@link Mono}, or an object value. Other
 * reactive types are not supported.
 * <p>
 * If the return value is one of the reactive types listed above, it must match the return value
 * type of the annotated method.
 */
public class TimeLimiterMethodInterceptor extends AbstractMethodInterceptor {

    @Inject(optional = true)
    @Nullable
    private TimeLimiterRegistry registry;

    @Nullable
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        TimeLimiter annotation = invocation.getMethod().getAnnotation(TimeLimiter.class);
        if (annotation == null) {
            annotation = invocation.getMethod().getDeclaringClass()
                .getAnnotation(TimeLimiter.class);
        }
        final RecoveryFunction<?> fallbackMethod = Optional
            .ofNullable(createRecoveryFunction(invocation, annotation.fallbackMethod()))
            .orElse(new DefaultRecoveryFunction<>());
        if (registry == null) {
            registry = TimeLimiterRegistry.ofDefaults();
        }
        io.github.resilience4j.timelimiter.TimeLimiter timeLimiter = registry.timeLimiter(annotation.name());
        Class<?> returnType = invocation.getMethod().getReturnType();
        if (Promise.class.isAssignableFrom(returnType)) {
            return invokeForPromise(invocation, fallbackMethod, timeLimiter);
        } else if (Flux.class.isAssignableFrom(returnType)) {
            return invokeForFlux(invocation, fallbackMethod, timeLimiter);
        } else if (Mono.class.isAssignableFrom(returnType)) {
            return invokeForMono(invocation, fallbackMethod, timeLimiter);
        } else if (CompletionStage.class.isAssignableFrom(returnType)) {
            return invokeForCompletionStage(invocation, fallbackMethod, timeLimiter);
        } else {
            throw new IllegalArgumentException(String.join(" ", returnType.getName(),
                invocation.getMethod().getName(),
                "has unsupported by @TimeLimiter return type.", "Promise, Mono, Flux, or CompletionStage expected."));
        }
    }

    @SuppressWarnings("unchecked")
    public Object invokeForPromise(MethodInvocation invocation,
                                   RecoveryFunction<?> fallbackMethod,
                                   io.github.resilience4j.timelimiter.TimeLimiter timeLimiter) throws Throwable {
        Promise<?> result = (Promise<?>) proceed(invocation);
        if (result != null) {
            TimeLimiterTransformer transformer = TimeLimiterTransformer.of(timeLimiter)
                .recover(fallbackMethod);
            result = result.transform(transformer);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public Object invokeForFlux(MethodInvocation invocation,
                                RecoveryFunction<?> fallbackMethod,
                                io.github.resilience4j.timelimiter.TimeLimiter timeLimiter) throws Throwable {
        Flux<?> result = (Flux<?>) proceed(invocation);
        if (result != null) {
            TimeLimiterOperator operator = TimeLimiterOperator.of(timeLimiter);
            result = fallbackMethod.onErrorResume(result.transform(operator));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public Object invokeForMono(MethodInvocation invocation,
                                RecoveryFunction<?> fallbackMethod,
                                io.github.resilience4j.timelimiter.TimeLimiter timeLimiter) throws Throwable {
        Mono<?> result = (Mono<?>) proceed(invocation);
        if (result != null) {
            TimeLimiterOperator operator = TimeLimiterOperator.of(timeLimiter);
            result = fallbackMethod.onErrorResume(result.transform(operator));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public Object invokeForCompletionStage(MethodInvocation invocation,
                                           RecoveryFunction<?> fallbackMethod,
                                           io.github.resilience4j.timelimiter.TimeLimiter timeLimiter) {
        ScheduledExecutorService scheduler = Execution.current().getController().getExecutor();
        CompletableFuture<?> future = timeLimiter.executeCompletionStage(scheduler, () -> {
            try {
                return (CompletionStage) proceed(invocation);
            } catch (Throwable t) {
                final CompletableFuture<?> promise = new CompletableFuture<>();
                promise.completeExceptionally(t);
                return (CompletionStage) promise;
            }
        }).toCompletableFuture();
        completeFailedFuture(new TimeoutException(), fallbackMethod, future);
        return future;
    }

}
