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
package io.github.resilience4j.ratpack.internal;

import com.google.inject.Inject;
import io.github.resilience4j.ratpack.RecoveryFunction;
import io.github.resilience4j.ratpack.RetryTransformer;
import io.github.resilience4j.ratpack.annotation.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import ratpack.exec.Promise;

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
        Object result;
        try {
            result = invocation.proceed();
        } catch (Exception e) {
            // exception thrown, we know a direct value was attempted to be returned
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
        if (result instanceof Promise<?>) {
            RetryTransformer transformer = RetryTransformer.of(retry);
            if (!annotation.recovery().isAssignableFrom(DefaultRecoveryFunction.class)) {
                transformer = transformer.recover(recoveryFunction);
            }
            result = ((Promise<?>) result).transform(transformer);
        }
        // TODO drmaas - this will be fixed in a future PR. Commenting out for now.
//        else if (result instanceof CompletionStage) {
//            CompletionStage stage = (CompletionStage) result;
//            result = executeCompletionStage(invocation, stage, retry, recoveryFunction);
//        }
        else {
            retry.onSuccess();
        }
        return result;
    }

//    @SuppressWarnings("unchecked")
//    private CompletionStage<?> executeCompletionStage(MethodInvocation invocation, CompletionStage<?> stage, io.github.resilience4j.retry.Retry retry, RecoveryFunction<?> recoveryFunction) {
//        final CompletableFuture promise = new CompletableFuture();
//        stage.whenComplete((v, t) -> {
//            if (t != null) {
//                try {
//                    retry.onError((Exception) t);
//                    CompletionStage next = (CompletionStage)invocation.proceed();
//                    CompletableFuture temp = executeCompletionStage(invocation, next, retry, recoveryFunction).toCompletableFuture();
//                    promise.complete(temp.join());
//                } catch (Throwable t2) {
//                    try {
//                        Object result = recoveryFunction.apply(t);
//                        promise.complete(result);
//                    } catch (Throwable t3) {
//                        promise.completeExceptionally(t3);
//                    }
//                }
//            } else {
//                promise.complete(v);
//            }
//        });
//        return promise;
//    }

}
