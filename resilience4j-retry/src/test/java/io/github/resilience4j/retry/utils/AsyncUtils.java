/*
 *
 *  Copyright 2016 Robert Winkler
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

package io.github.resilience4j.retry.utils;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public class AsyncUtils {


    private static final long DEFAULT_TIMEOUT_SECONDS = 5;

    public static <T> T awaitResult(CompletionStage<T> completionStage, long timeoutSeconds) {
        try {
            return completionStage.toCompletableFuture().get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException e) {
            throw new AssertionError(e);
        } catch (ExecutionException e) {
            throw new RuntimeExecutionException(e.getCause());
        }
    }

    public static <T> T awaitResult(CompletionStage<T> completionStage) {
        return awaitResult(completionStage, DEFAULT_TIMEOUT_SECONDS);
    }

    public static <T> T awaitResult(Supplier<CompletionStage<T>> completionStageSupplier,
        long timeoutSeconds) {
        return awaitResult(completionStageSupplier.get(), timeoutSeconds);
    }

    public static <T> T awaitResult(Supplier<CompletionStage<T>> completionStageSupplier) {
        return awaitResult(completionStageSupplier, DEFAULT_TIMEOUT_SECONDS);
    }

    private static class RuntimeExecutionException extends RuntimeException {

        RuntimeExecutionException(Throwable cause) {
            super(cause);
        }
    }
}
