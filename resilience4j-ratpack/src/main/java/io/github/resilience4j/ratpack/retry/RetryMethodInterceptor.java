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
import io.github.resilience4j.ratpack.recovery.RecoveryFunction;
import io.github.resilience4j.retry.RetryRegistry;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import ratpack.exec.Promise;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * A {@link MethodInterceptor} to handle all methods annotated with {@link Retry}. It will
 * handle methods that return a Promise, CompletionStage, or value. It will execute the retry and
 * the fallback found in the annotation.
 */
public class RetryMethodInterceptor implements MethodInterceptor {

    @Inject(optional = true)
    @Nullable
    private RetryRegistry registry;

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Retry annotation = invocation.getMethod().getAnnotation(Retry.class);
        if(registry == null) {
            registry = RetryRegistry.ofDefaults();
        }
        io.github.resilience4j.retry.Retry retry = registry.retry(annotation.name());
        RecoveryFunction<?> recoveryFunction = annotation.recovery().newInstance();
        Class<?> returnType = invocation.getMethod().getReturnType();
        if (Promise.class.isAssignableFrom(returnType)) {
            Promise<?> result = (Promise<?>) proceed(invocation, retry, recoveryFunction);
            if (result != null) {
                RetryTransformer transformer = RetryTransformer.of(retry).recover(recoveryFunction);
                result = result.transform(transformer);
            }
            return result;
        } else if (Flux.class.isAssignableFrom(returnType)) {
            Flux<?> result = (Flux<?>) proceed(invocation, retry, recoveryFunction);
            if (result != null) {
                RetryTransformer transformer = RetryTransformer.of(retry).recover(recoveryFunction);
                final Flux<?> temp = result;
                Promise<?> promise = Promise.async(f -> temp.collectList().subscribe(f::success, f::error)).transform(transformer);
                Flux next = Flux.create(subscriber ->
                        promise.onError(subscriber::error).then(value -> {
                            subscriber.next(value);
                            subscriber.complete();
                        })
                );
                result = recoveryFunction.onErrorResume(next);
            }
            return result;
        } else if (Mono.class.isAssignableFrom(returnType)) {
            Mono<?> result = (Mono<?>) proceed(invocation, retry, recoveryFunction);
            if (result != null) {
                RetryTransformer transformer = RetryTransformer.of(retry).recover(recoveryFunction);
                final Mono<?> temp = result;
                Promise<?> promise = Promise.async(f -> temp.subscribe(f::success, f::error)).transform(transformer);
                Mono next = Mono.create(subscriber ->
                        promise.onError(subscriber::error).then(subscriber::success)
                );
                result = recoveryFunction.onErrorResume(next);
            }
            return result;
        }
        else if (CompletionStage.class.isAssignableFrom(returnType)) {
            CompletionStage stage = (CompletionStage) proceed(invocation, retry, recoveryFunction);
            return executeCompletionStage(invocation, stage, retry.context(), recoveryFunction);
        }
        return proceed(invocation, retry, recoveryFunction);
    }

    @SuppressWarnings("unchecked")

    private CompletionStage<?> executeCompletionStage(MethodInvocation invocation, CompletionStage<?> stage, io.github.resilience4j.retry.Retry.Context context, RecoveryFunction<?> recoveryFunction) {
        final CompletableFuture promise = new CompletableFuture();
        stage.whenComplete((v, t) -> {
            if (t != null) {
                try {
                    context.onError((Exception) t);
                    CompletionStage next = (CompletionStage) invocation.proceed();
                    CompletableFuture temp = executeCompletionStage(invocation, next, context, recoveryFunction).toCompletableFuture();
                    promise.complete(temp.join());
                } catch (Throwable t2) {
                    try {
                        Object result = recoveryFunction.apply(t);
                        promise.complete(result);
                    } catch (Throwable t3) {
                        promise.completeExceptionally(t3);
                    }
                }
            } else {
                context.onSuccess();
                promise.complete(v);
            }
        });
        return promise;
    }

    @Nullable
    private Object proceed(MethodInvocation invocation, io.github.resilience4j.retry.Retry retry, RecoveryFunction<?> recoveryFunction) throws Throwable {
        io.github.resilience4j.retry.Retry.Context context = retry.context();
        try {
            Object result = invocation.proceed();
            context.onSuccess();
            return result;
        } catch (Exception e) {
            // exception thrown, we know a direct value was attempted to be returned
            Object result;
            context.onError(e);
            while (true) {
                try {
                    result = invocation.proceed();
                    context.onSuccess();
                    return result;
                } catch (Exception e1) {
                    try {
                        context.onError(e1);
                    } catch (Throwable t) {
                        return recoveryFunction.apply(t);
                    }
                }
            }
        }
    }

}
