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
import io.github.resilience4j.ratpack.recovery.RecoveryFunction;
import io.github.resilience4j.retry.RetryRegistry;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import ratpack.exec.Promise;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * A {@link MethodInterceptor} to handle all methods annotated with {@link Retry}. It will
 * handle methods that return a Promise, CompletionStage, or value. It will execute the retry and
 * the fallback found in the annotation.
 */
public class RetryMethodInterceptor implements MethodInterceptor {

    @Inject(optional = true)
    private RetryRegistry registry;

    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Retry annotation = invocation.getMethod().getAnnotation(Retry.class);
        RecoveryFunction<?> recoveryFunction = annotation.recovery().newInstance();
        if (registry == null) {
            registry = RetryRegistry.ofDefaults();
        }
        io.github.resilience4j.retry.Retry retry = registry.newRetry(annotation.name());
        if (retry == null) {
            return invocation.proceed();
        }
        Class<?> returnType = invocation.getMethod().getReturnType();
        if (Promise.class.isAssignableFrom(returnType)) {
            Promise<?> result = (Promise<?>) proceed(invocation, retry, recoveryFunction);
            if (result != null) {
                RetryTransformer transformer = RetryTransformer.of(retry).recover(recoveryFunction);
                result = result.transform(transformer);
            }
            return result;
        } else if (Observable.class.isAssignableFrom(returnType)) {
            Observable<?> result = (Observable<?>) proceed(invocation, retry, recoveryFunction);
            if (result != null) {
                io.github.resilience4j.retry.transformer.RetryTransformer transformer = io.github.resilience4j.retry.transformer.RetryTransformer.of(retry);
                result = result.compose(transformer).onErrorReturn(t -> recoveryFunction.apply((Throwable) t));
            }
            return result;
        } else if (Flowable.class.isAssignableFrom(returnType)) {
            Flowable<?> result = (Flowable<?>) proceed(invocation, retry, recoveryFunction);
            if (result != null) {
                io.github.resilience4j.retry.transformer.RetryTransformer transformer = io.github.resilience4j.retry.transformer.RetryTransformer.of(retry);
                result = result.compose(transformer).onErrorReturn(t -> recoveryFunction.apply((Throwable) t));
            }
            return result;
        } else if (Single.class.isAssignableFrom(returnType)) {
            Single<?> result = (Single<?>) proceed(invocation, retry, recoveryFunction);
            if (result != null) {
                io.github.resilience4j.retry.transformer.RetryTransformer transformer = io.github.resilience4j.retry.transformer.RetryTransformer.of(retry);
                result = result.compose(transformer).onErrorReturn(t -> recoveryFunction.apply((Throwable) t));
            }
            return result;
        }
        else if (CompletionStage.class.isAssignableFrom(returnType)) {
            CompletionStage stage = (CompletionStage) proceed(invocation, retry, recoveryFunction);
            return executeCompletionStage(invocation, stage, retry, recoveryFunction);
        }
        return proceed(invocation, retry, recoveryFunction);
    }

    @SuppressWarnings("unchecked")
    private CompletionStage<?> executeCompletionStage(MethodInvocation invocation, CompletionStage<?> stage, io.github.resilience4j.retry.Retry retry, RecoveryFunction<?> recoveryFunction) {
        final CompletableFuture promise = new CompletableFuture();
        stage.whenComplete((v, t) -> {
            if (t != null) {
                try {
                    retry.onError((Exception) t);
                    CompletionStage next = (CompletionStage) invocation.proceed();
                    CompletableFuture temp = executeCompletionStage(invocation, next, retry, recoveryFunction).toCompletableFuture();
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
                promise.complete(v);
            }
        });
        return promise;
    }

    private Object proceed(MethodInvocation invocation, io.github.resilience4j.retry.Retry retry, RecoveryFunction<?> recoveryFunction) throws Throwable {
        try {
            return invocation.proceed();
        } catch (Exception e) {
            // exception thrown, we know a direct value was attempted to be returned
            Object result;
            retry.onError(e);
            while (true) {
                try {
                    result = invocation.proceed();
                    retry.onSuccess();
                    return result;
                } catch (Exception e1) {
                    try {
                        retry.onError(e1);
                    } catch (Throwable t) {
                        return recoveryFunction.apply(t);
                    }
                }
            }
        }
    }

}
