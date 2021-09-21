/*
 * Copyright 2019 Kyuhyen Hwang
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
package io.github.resilience4j.fallback;

import io.github.resilience4j.core.functions.CheckedSupplier;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

/**
 * fallbackMethod decorator for {@link CompletionStage}
 */
public class CompletionStageFallbackDecorator implements FallbackDecorator {

    @Override
    public boolean supports(Class<?> target) {
        return CompletionStage.class.isAssignableFrom(target);
    }

    @SuppressWarnings("unchecked")
    @Override
    public CheckedSupplier<Object> decorate(FallbackMethod fallbackMethod,
                                            CheckedSupplier<Object> supplier) {
        return supplier.andThen(request -> {
            CompletionStage<Object> completionStage = (CompletionStage) request;
            CompletableFuture promise = new CompletableFuture();
            completionStage.whenComplete((result, throwable) -> {
                if (throwable != null){
                    if (throwable instanceof CompletionException || throwable instanceof ExecutionException) {
                        tryRecover(fallbackMethod, promise, throwable.getCause());
                    }else{
                        tryRecover(fallbackMethod, promise, throwable);
                    }
                } else {
                    promise.complete(result);
                }
            });

            return promise;
        });
    }

    @SuppressWarnings("unchecked")
    private void tryRecover(FallbackMethod fallbackMethod, CompletableFuture promise,
        Throwable throwable) {
        try {
            CompletionStage<Object> completionStage = (CompletionStage) fallbackMethod.fallback(throwable);
            completionStage.whenComplete((fallbackResult, fallbackThrowable) -> {
                    if (fallbackThrowable != null) {
                        promise.completeExceptionally(fallbackThrowable);
                    } else {
                        promise.complete(fallbackResult);
                    }
                });
        } catch (Throwable fallbackThrowable) {
            promise.completeExceptionally(fallbackThrowable);
        }
    }
}
