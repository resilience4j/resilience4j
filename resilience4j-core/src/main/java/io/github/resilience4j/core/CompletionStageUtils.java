/*
 *
 *  Copyright 2020: Robert Winkler
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.core;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class CompletionStageUtils {

    private CompletionStageUtils() {
    }

    /**
     * Returns a CompletionStage that is recovered from any exception.
     *
     * @param <T>              return type of after
     * @param exceptionHandler the function applied after callable has failed
     * @return a CompletionStage that is recovered from any exception.
     */
    public static <T> CompletionStage<T> recover(CompletionStage<T> completionStage, Function<Throwable, T> exceptionHandler){
        CompletableFuture<T> promise = new CompletableFuture<>();
        completionStage.whenComplete((result, throwable) -> {
            if (throwable != null) {
                try {
                    promise.complete(exceptionHandler.apply(throwable));
                } catch (Exception fallbackException) {
                    promise.completeExceptionally(fallbackException);
                }
            } else {
                promise.complete(result);
            }
        });
        return promise;
    }
}
