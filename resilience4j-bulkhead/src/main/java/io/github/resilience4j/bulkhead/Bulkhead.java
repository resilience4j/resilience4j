/*
 *
 *  Copyright 2017: Robert Winkler, Lucas Lech
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
package io.github.resilience4j.bulkhead;

import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.bulkhead.internal.SemaphoreBulkhead;
import io.github.resilience4j.bulkhead.utils.BulkheadUtils;
import io.reactivex.Flowable;
import javaslang.control.Try;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A {@link Bulkhead} represent an entity limiting the amount of parallel operations. It does not assume nor does it mandate usage
 * of any particular concurrency and/or io model. These details are left for the client to manage. This bulkhead, depending on the
 * underlying concurrency/io model can be used to shed load, and, where it makes sense, limit resource use (i.e. limit amount of
 * threads/actors involved in a particular flow, etc).
 *
 * In order to execute an operation protected by this bulkhead, a permission must be obtained by calling {@link Bulkhead#isCallPermitted()}
 * If the bulkhead is full, no additional operations will be permitted to execute until space is available.
 *
 * Once the operation is complete, regardless of the result, client needs to call {@link Bulkhead#onComplete()} in order to maintain
 * integrity of internal bulkhead state.
 *
 */
public interface Bulkhead {

    /**
     * Attempts to acquire a permit, which allows an call to be executed.
     *
     * @return boolean whether a call should be executed
     */
    boolean isCallPermitted();

    /**
     * Records a completed call.
     */
    void onComplete();

    /**
     * Returns the name of this bulkhead.
     *
     * @return the name of this bulkhead
     */
    String getName();

    /**
     * Returns a configured max amount of parallel executions that this bulkhead supports.
     *
      * @return bulkhead depth
     */
    int getConfiguredDepth();

    /**
     * Returns the number of parallel executions this bulkhead can support at this point in time.
     *
     * @return remaining bulkhead depth
     */
    int getRemainingDepth();

    /**
     * Returns a reactive stream of BulkheadEvent events.
     *
     * @return a stream of events
     */
    Flowable<BulkheadEvent> getEventStream();

    /**
     * Decorates and executes the decorated Supplier.
     *
     * @param supplier the original Supplier
     * @param <T> the type of results supplied by this supplier
     * @return the result of the decorated Supplier.
     */
    default <T> T executeSupplier(Supplier<T> supplier){
        return decorateSupplier(this, supplier).get();
    }

    /**
     * Decorates and executes the decorated Callable.
     *
     * @param callable the original Callable
     *
     * @return the result of the decorated Callable.
     * @param <T> the result type of callable
     * @throws Exception if unable to compute a result
     */
    default <T> T executeCallable(Callable<T> callable) throws Exception{
        return decorateCallable(this, callable).call();
    }

    /**
     * Decorates and executes the decorated Runnable.
     *
     * @param runnable the original Runnable
     */
    default void executeRunnable(Runnable runnable){
        decorateRunnable(this, runnable).run();
    }

    /**
     * Decorates and executes the decorated CompletionStage.
     *
     * @param supplier the original CompletionStage
     * @param <T> the type of results supplied by this supplier
     * @return the decorated CompletionStage.
     */
    default <T> CompletionStage<T> executeCompletionStage(Supplier<CompletionStage<T>> supplier){
        return decorateCompletionStage(this, supplier).get();
    }

    /**
     * Returns a supplier which is decorated by a bulkhead.
     *
     * @param bulkhead the Bulkhead
     * @param supplier the original supplier
     * @param <T> the type of results supplied by this supplier
     * @return a supplier which is decorated by a Bulkhead.
     */
    static <T> Try.CheckedSupplier<T> decorateCheckedSupplier(Bulkhead bulkhead, Try.CheckedSupplier<T> supplier){
        return () -> {
            BulkheadUtils.isCallPermitted(bulkhead);
            try {
                return supplier.get();
            }
            finally {
                bulkhead.onComplete();
            }
        };
    }

    /**
     * Returns a supplier which is decorated by a bulkhead.
     *
     * @param bulkhead the bulkhead
     * @param supplier the original supplier
     * @param <T> the type of the returned CompletionStage's result
     * @return a supplier which is decorated by a Bulkhead.
     */
    static <T> Supplier<CompletionStage<T>> decorateCompletionStage(Bulkhead bulkhead, Supplier<CompletionStage<T>> supplier) {
        return () -> {

            final CompletableFuture<T> promise = new CompletableFuture<>();

            if (!bulkhead.isCallPermitted()) {
                promise.completeExceptionally(new BulkheadFullException(String.format("Bulkhead '%s' is open", bulkhead.getName())));
            }
            else {
                try {
                    supplier.get()
                            .whenComplete(
                                (result, throwable) -> {
                                    bulkhead.onComplete();
                                    if (throwable != null) {
                                        promise.completeExceptionally(throwable);
                                    }
                                    else {
                                        promise.complete(result);
                                    }
                                }
                            );
                }
                catch (Throwable throwable) {
                    bulkhead.onComplete();
                    promise.completeExceptionally(throwable);
                }
            }

            return promise;
        };
    }

    /**
     * Returns a runnable which is decorated by a bulkhead.
     *
     * @param bulkhead the bulkhead
     * @param runnable the original runnable
     *
     * @return a runnable which is decorated by a Bulkhead.
     */
    static Try.CheckedRunnable decorateCheckedRunnable(Bulkhead bulkhead, Try.CheckedRunnable runnable){
        return () -> {
            BulkheadUtils.isCallPermitted(bulkhead);
            try{
                runnable.run();
            }
            finally {
                bulkhead.onComplete();
            }
        };
    }

    /**
     * Returns a callable which is decorated by a bulkhead.
     *
     * @param bulkhead the bulkhead
     * @param callable the original Callable
     * @param <T> the result type of callable
     *
     * @return a supplier which is decorated by a Bulkhead.
     */
    static <T> Callable<T> decorateCallable(Bulkhead bulkhead, Callable<T> callable){
        return () -> {
            BulkheadUtils.isCallPermitted(bulkhead);
            try {
                return callable.call();
            }
            finally {
                bulkhead.onComplete();
            }
        };
    }

    /**
     * Returns a supplier which is decorated by a bulkhead.
     *
     * @param bulkhead the bulkhead
     * @param supplier the original supplier
     * @param <T> the type of results supplied by this supplier
     *
     * @return a supplier which is decorated by a Bulkhead.
     */
    static <T> Supplier<T> decorateSupplier(Bulkhead bulkhead, Supplier<T> supplier){
        return () -> {
            BulkheadUtils.isCallPermitted(bulkhead);
            try {
                return supplier.get();
            }
            finally {
                bulkhead.onComplete();
            }
        };
    }

    /**
     * Returns a consumer which is decorated by a bulkhead.

     * @param bulkhead the bulkhead
     * @param consumer the original consumer
     * @param <T> the type of the input to the consumer
     *
     * @return a consumer which is decorated by a Bulkhead.
     */
    static <T> Consumer<T> decorateConsumer(Bulkhead bulkhead, Consumer<T> consumer){
        return (t) -> {
            BulkheadUtils.isCallPermitted(bulkhead);
            try {
                consumer.accept(t);
            }
            finally {
                bulkhead.onComplete();
            }
        };
    }

    /**
     * Returns a consumer which is decorated by a bulkhead.

     * @param bulkhead the bulkhead
     * @param consumer the original consumer
     * @param <T> the type of the input to the consumer
     *
     * @return a consumer which is decorated by a Bulkhead.
     */
    static <T> Try.CheckedConsumer<T> decorateCheckedConsumer(Bulkhead bulkhead, Try.CheckedConsumer<T> consumer){
        return (t) -> {
            BulkheadUtils.isCallPermitted(bulkhead);
            try {
                consumer.accept(t);
            }
            finally {
                bulkhead.onComplete();
            }
        };
    }

    /**
     * Returns a runnable which is decorated by a bulkhead.
     *
     * @param bulkhead the bulkhead
     * @param runnable the original runnable
     *
     * @return a runnable which is decorated by a bulkhead.
     */
    static Runnable decorateRunnable(Bulkhead bulkhead, Runnable runnable){
        return () -> {
            BulkheadUtils.isCallPermitted(bulkhead);
            try{
                runnable.run();
            }
            finally {
                bulkhead.onComplete();
            }
        };
    }

    /**
     * Returns a function which is decorated by a bulkhead.

     * @param bulkhead the bulkhead
     * @param function the original function
     * @param <T> the type of the input to the function
     * @param <R> the type of the result of the function
     * @return a function which is decorated by a bulkhead.
     */
    static <T, R> Function<T, R> decorateFunction(Bulkhead bulkhead, Function<T, R> function){
        return (T t) -> {
            BulkheadUtils.isCallPermitted(bulkhead);
            try{
                return function.apply(t);
            }
            finally {
                bulkhead.onComplete();
            }
        };
    }

    /**
     * Returns a function which is decorated by a bulkhead.
     *
     * @param bulkhead the bulkhead
     * @param function the original function
     * @param <T> the type of the input to the function
     * @param <R> the type of the result of the function
     * @return a function which is decorated by a bulkhead.
     */
    static <T, R> Try.CheckedFunction<T, R> decorateCheckedFunction(Bulkhead bulkhead, Try.CheckedFunction<T, R> function){
        return (T t) -> {
            BulkheadUtils.isCallPermitted(bulkhead);
            try{
                return function.apply(t);
            }
            finally {
                bulkhead.onComplete();
            }
        };
    }

    /**
     * Creates a bulkhead with a given name and depth
     *
     * @param name the name of the bulkhead
     * @param depth max depth of the bulkhead
     *
     * @return a bulkhead
     */
    static Bulkhead of(String name, int depth) {
        return new SemaphoreBulkhead(name, depth);
    }

}
