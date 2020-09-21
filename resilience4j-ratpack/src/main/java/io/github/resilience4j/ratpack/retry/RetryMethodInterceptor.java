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
package io.github.resilience4j.ratpack.retry;

import com.google.inject.Inject;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.ratpack.internal.AbstractMethodInterceptor;
import io.github.resilience4j.ratpack.recovery.DefaultRecoveryFunction;
import io.github.resilience4j.ratpack.recovery.RecoveryFunction;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.annotation.Retry;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import ratpack.exec.Promise;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * A {@link MethodInterceptor} to handle all methods annotated with {@link Retry}. It will handle
 * methods that return a {@link Promise}, {@link reactor.core.publisher.Flux}, {@link
 * reactor.core.publisher.Mono}, {@link java.util.concurrent.CompletionStage}, or value.
 * <p>
 * Given a method like this:
 * <pre><code>
 *     {@literal @}RateLimiter(name = "myService")
 *     public String fancyName(String name) {
 *         return "Sir Captain " + name;
 *     }
 * </code></pre>
 * each time the {@code #fancyName(String)} method is invoked, the method's execution will pass
 * through a a {@link io.github.resilience4j.retry.Retry} according to the given config.
 * <p>
 * The method parameter signature must match either:
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
public class RetryMethodInterceptor extends AbstractMethodInterceptor {

    @Inject(optional = true)
    @Nullable
    private RetryRegistry registry;

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Retry annotation = invocation.getMethod().getAnnotation(Retry.class);
        if (annotation == null) {
            annotation = invocation.getMethod().getDeclaringClass().getAnnotation(Retry.class);
        }
        if (registry == null) {
            registry = RetryRegistry.ofDefaults();
        }
        io.github.resilience4j.retry.Retry retry = registry.retry(annotation.name());
        final RecoveryFunction<?> fallbackMethod = Optional
            .ofNullable(createRecoveryFunction(invocation, annotation.fallbackMethod()))
            .orElse(new DefaultRecoveryFunction<>());
        Class<?> returnType = invocation.getMethod().getReturnType();
        if (Promise.class.isAssignableFrom(returnType)) {
            Promise<?> result = (Promise<?>) proceed(invocation);
            if (result != null) {
                RetryTransformer transformer = RetryTransformer.of(retry).recover(fallbackMethod);
                result = result.transform(transformer);
            }
            return result;
        } else if (Flux.class.isAssignableFrom(returnType)) {
            Flux<?> result = (Flux<?>) proceed(invocation);
            if (result != null) {
                RetryTransformer transformer = RetryTransformer.of(retry).recover(fallbackMethod);
                final Flux<?> temp = result;
                Promise<?> promise = Promise
                    .async(f -> temp.collectList().subscribe(f::success, f::error))
                    .transform(transformer);
                Flux next = Flux.create(subscriber ->
                    promise.onError(subscriber::error).then(value -> {
                        subscriber.next(value);
                        subscriber.complete();
                    })
                );
                result = fallbackMethod.onErrorResume(next);
            }
            return result;
        } else if (Mono.class.isAssignableFrom(returnType)) {
            Mono<?> result = (Mono<?>) proceed(invocation);
            if (result != null) {
                RetryTransformer transformer = RetryTransformer.of(retry).recover(fallbackMethod);
                final Mono<?> temp = result;
                Promise<?> promise = Promise.async(f -> temp.subscribe(f::success, f::error))
                    .transform(transformer);
                Mono next = Mono.create(subscriber ->
                    promise.onError(subscriber::error).then(subscriber::success)
                );
                result = fallbackMethod.onErrorResume(next);
            }
            return result;
        } else if (CompletionStage.class.isAssignableFrom(returnType)) {
            CompletionStage stage = (CompletionStage) proceed(invocation);
            return executeCompletionStage(invocation, stage, retry.context(), fallbackMethod);
        } else {
            return handleProceedWithException(invocation, retry, fallbackMethod);
        }
    }

    @SuppressWarnings("unchecked")
    private CompletionStage<?> executeCompletionStage(MethodInvocation invocation,
        CompletionStage<?> stage, io.github.resilience4j.retry.Retry.Context context,
        RecoveryFunction<?> recoveryFunction) {
        final CompletableFuture promise = new CompletableFuture();
        stage.whenComplete((v, t) -> {
            if (t != null) {
                try {
                    context.onError((Exception) t);
                    CompletionStage next = (CompletionStage) invocation.proceed();
                    CompletableFuture temp = executeCompletionStage(invocation, next, context,
                        recoveryFunction).toCompletableFuture();
                    promise.complete(temp.join());
                } catch (Throwable t2) {
                    completeFailedFuture(t2, recoveryFunction, promise);
                }
            } else {
                context.onComplete();
                promise.complete(v);
            }
        });
        return promise;
    }

    @Nullable
    private Object handleProceedWithException(MethodInvocation invocation,
        io.github.resilience4j.retry.Retry retry, RecoveryFunction<?> recoveryFunction)
        throws Throwable {
        try {
            return io.github.resilience4j.retry.Retry
                .decorateCheckedSupplier(retry, invocation::proceed).get();
        } catch (Throwable t) {
            return recoveryFunction.apply(t);
        }
    }

}
