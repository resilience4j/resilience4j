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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.*;

public class CompletionStageUtils {

    private CompletionStageUtils() {
    }

    /**
     * Returns a CompletionStage that is recovered from any exception.
     *
     * @param completionStage the completionStage which should be recovered from any exception
     * @param exceptionHandler the function applied after callable has failed
     * @return a CompletionStage that is recovered from any exception.
     */
    public static <T> CompletionStage<T> recover(CompletionStage<T> completionStage, Function<Throwable, T> exceptionHandler){
        return completionStage.exceptionally(exceptionHandler);
    }

    /**
     * Returns a CompletionStage that is recovered from a specific exception.
     *
     * @param completionStage the completionStage which should be recovered from a certain exception
     * @param exceptionTypes the specific exception types that should be recovered
     * @param exceptionHandler the function applied after callable has failed
     * @return a CompletionStage that is recovered from a specific exception.
     */
    public static <T> CompletionStage<T> recover(CompletionStage<T> completionStage, List<Class<? extends Throwable>> exceptionTypes, Function<Throwable, T> exceptionHandler){
        CompletableFuture<T> promise = new CompletableFuture<>();
        completionStage.whenComplete((result, throwable) -> {
            if (throwable != null){
                if (throwable instanceof CompletionException || throwable instanceof ExecutionException) {
                    tryRecover(exceptionTypes, exceptionHandler, promise, throwable.getCause());
                }else{
                    tryRecover(exceptionTypes, exceptionHandler, promise, throwable);
                }

            } else {
                promise.complete(result);
            }
        });
        return promise;
    }

    /**
     * Returns a CompletionStage that is recovered from a specific exception.
     *
     * @param completionStage the completionStage which should be recovered from a certain exception
     * @param exceptionType the specific exception type that should be recovered
     * @param exceptionHandler the function applied after callable has failed
     * @return a CompletionStage that is recovered from a specific exception.
     */
    public static <X extends Throwable, T> CompletionStage<T> recover(CompletionStage<T> completionStage, Class<X> exceptionType, Function<Throwable, T> exceptionHandler){
        CompletableFuture<T> promise = new CompletableFuture<>();
        completionStage.whenComplete((result, throwable) -> {
            if (throwable != null){
                if (throwable instanceof CompletionException || throwable instanceof ExecutionException) {
                    tryRecover(exceptionType, exceptionHandler, promise, throwable.getCause());
                }else{
                    tryRecover(exceptionType, exceptionHandler, promise, throwable);
                }

            } else {
                promise.complete(result);
            }
        });
        return promise;
    }

    private static <T> void tryRecover(List<Class<? extends Throwable>> exceptionTypes,
        Function<Throwable, T> exceptionHandler, CompletableFuture<T> promise,
        Throwable throwable) {
        if(exceptionTypes.stream().anyMatch(exceptionType -> exceptionType.isAssignableFrom(throwable.getClass()))) {
            try {
                promise.complete(exceptionHandler.apply(throwable));
            } catch (Exception fallbackException) {
                promise.completeExceptionally(fallbackException);
            }
        }else{
            promise.completeExceptionally(throwable);
        }
    }

    private static <X extends Throwable, T> void tryRecover(Class<X> exceptionType,
        Function<Throwable, T> exceptionHandler, CompletableFuture<T> promise,
        Throwable throwable) {
        if(exceptionType.isAssignableFrom(throwable.getClass())) {
            try {
                promise.complete(exceptionHandler.apply(throwable));
            } catch (Exception fallbackException) {
                promise.completeExceptionally(fallbackException);
            }
        }else{
            promise.completeExceptionally(throwable);
        }
    }

    /**
     * Returns a decorated CompletionStage that is recovered from a specific exception.
     *
     * @param completionStageSupplier a supplier of the completionStage which should be recovered from a certain exception
     * @param exceptionHandler the function applied after callable has failed
     * @return a CompletionStage that is recovered from a specific exception.
     */
    public static <T> Supplier<CompletionStage<T>> recover(
        Supplier<CompletionStage<T>> completionStageSupplier,
        Function<Throwable, T> exceptionHandler) {
        return () -> recover(completionStageSupplier.get(), exceptionHandler);
    }

    /**
     * Returns a decorated CompletionStage that is recovered from a specific exception.
     *
     * @param completionStageSupplier a supplier of the completionStage which should be recovered from a certain exception
     * @param exceptionType the specific exception type that should be recovered
     * @param exceptionHandler the function applied after callable has failed
     * @return a CompletionStage that is recovered from a specific exception.
     */
    public static <T, X extends Throwable> Supplier<CompletionStage<T>> recover(
        Supplier<CompletionStage<T>> completionStageSupplier, Class<X> exceptionType,
        Function<Throwable, T> exceptionHandler) {
        return () -> recover(completionStageSupplier.get(), exceptionType, exceptionHandler);
    }

    /**
     * Returns a decorated CompletionStage that is recovered from a specific exception.
     *
     * @param completionStageSupplier a supplier of the completionStage which should be recovered from a certain exception
     * @param exceptionTypes the specific exception types that should be recovered
     * @param exceptionHandler the function applied after callable has failed
     * @return a CompletionStage that is recovered from a specific exception.
     */
    public static <T> Supplier<CompletionStage<T>> recover(
        Supplier<CompletionStage<T>> completionStageSupplier, List<Class<? extends Throwable>> exceptionTypes,
        Function<Throwable, T> exceptionHandler) {
        return () -> recover(completionStageSupplier.get(), exceptionTypes, exceptionHandler);
    }

    /**
     * Returns a composed CompletionStage that first executes the CompletionStage and optionally recovers from a specific result.
     *
     * @param <T>              return type of after
     * @param completionStage  the completionStage which should be recovered from a certain exception
     * @param resultPredicate the result predicate
     * @param resultHandler the result handler
     * @return a function composed of supplier and exceptionHandler
     */
    public static <T> CompletionStage<T> recover(
        CompletionStage<T> completionStage, Predicate<T> resultPredicate,
        UnaryOperator<T> resultHandler) {
        return completionStage.thenApply(result -> {
            if(resultPredicate.test(result)){
                return resultHandler.apply(result);
            }else{
                return result;
            }
        });
    }

    /**
     * Returns a composed CompletionStage that first executes the CompletionStage and optionally recovers from a specific result.
     *
     * @param <T>              return type of after
     * @param completionStageSupplier the CompletionStage supplier
     * @param resultPredicate the result predicate
     * @param resultHandler the result handler
     * @return a function composed of supplier and exceptionHandler
     */
    public static <T> Supplier<CompletionStage<T>> recover(
        Supplier<CompletionStage<T>> completionStageSupplier, Predicate<T> resultPredicate,
        UnaryOperator<T> resultHandler) {
        return () -> recover(completionStageSupplier.get(), resultPredicate, resultHandler);
    }

    /**
     * Returns a composed CompletionStage that first applies the CompletionStage and then applies {@linkplain
     * BiFunction} {@code after} to the result.
     *
     * @param <T>     return type of after
     * @param completionStageSupplier the CompletionStage supplier
     * @param handler the function applied after supplier
     * @return a function composed of supplier and handler
     */
    public static <T, R> Supplier<CompletionStage<R>> andThen(Supplier<CompletionStage<T>> completionStageSupplier,
        BiFunction<T, Throwable, R> handler) {
        return () -> completionStageSupplier.get().handle(handler);
    }
}
