/*
 *
 *  Copyright 2019: Mahmoud Romeh
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
package io.github.resilience4j.bulkhead.adaptive;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.adaptive.event.*;
import io.github.resilience4j.bulkhead.adaptive.internal.AdaptiveBulkheadStateMachine;
import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.EventPublisher;
import io.github.resilience4j.core.functions.CheckedConsumer;
import io.github.resilience4j.core.functions.CheckedFunction;
import io.github.resilience4j.core.functions.CheckedRunnable;
import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.core.functions.OnceConsumer;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A Bulkhead instance is thread-safe can be used to decorate multiple requests.
 * <p>
 * A {@link AdaptiveBulkhead} represent an entity limiting the amount of parallel operations. It
 * does not assume nor does it mandate usage of any particular concurrency and/or io model. These
 * details are left for the client to manage. This bulkhead, depending on the underlying
 * concurrency/io model can be used to shed load, and, where it makes sense, limit resource use
 * (i.e. limit amount of threads/actors involved in a particular flow, etc).
 * <p>
 * In order to execute an operation protected by this bulkhead, a permission must be obtained by
 * calling {@link AdaptiveBulkhead#tryAcquirePermission()} ()} If the bulkhead is full, no
 * additional operations will be permitted to execute until space is available.
 * <p>
 * Once the operation is complete, regardless of the result (Success or Failure), client needs to
 * call {@link AdaptiveBulkhead#onSuccess(long, TimeUnit)} or {@link AdaptiveBulkhead#onError(long,
 * TimeUnit, Throwable)}  in order to maintain integrity of internal bulkhead state which is handled
 * by invoking the configured adaptive limit policy.
 * <p>
 */
public interface AdaptiveBulkhead {

    /**
     * Acquires a permission to execute a call, only if one is available at the time of invocation.
     *
     * @return {@code true} if a permission was acquired and {@code false} otherwise
     */

    boolean tryAcquirePermission();

    /**
     * Acquires a permission to execute a call, only if one is available at the time of invocation
     *
     * @throws BulkheadFullException when the Bulkhead is full and no further calls are permitted.
     */
    void acquirePermission();

    /**
     * Releases a permission and increases the number of available permits by one.
     * <p>
     * Should only be used when a permission was acquired but not used. Otherwise use {@link
     * AdaptiveBulkhead#onSuccess(long, TimeUnit)} to signal a completed call and release a
     * permission.
     */
    void releasePermission();

    /**
     * Records a successful call and releases a permission.
     */
    void onSuccess(long startTime, TimeUnit durationUnit);

    /**
     * Records a failed call and releases a permission.
     */
    void onError(long startTime, TimeUnit durationUnit, Throwable throwable);

    /**
     * Returns the name of this bulkhead.
     *
     * @return the name of this bulkhead
     */
    String getName();

    /**
     * Returns the AdaptiveBulkheadConfig of this Bulkhead.
     *
     * @return bulkhead config
     */
    AdaptiveBulkheadConfig getBulkheadConfig();

    /**
     * Get the Metrics of this Bulkhead.
     *
     * @return the Metrics of this Bulkhead
     */
    Metrics getMetrics();

    /**
     * Returns an EventPublisher which subscribes to the reactive stream of
     * BulkheadEvent/AdaptiveBulkheadEvent events and can be used to register event consumers.
     *
     * @return an AdaptiveEventPublisher
     */
    AdaptiveEventPublisher getEventPublisher();

    long getCurrentTimestamp();

    TimeUnit getTimestampUnit();

    /**
     * Decorates and executes the decorated Supplier.
     *
     * @param supplier the original Supplier
     * @param <T>      the type of results supplied by this supplier
     * @return the result of the decorated Supplier.
     */
    default <T> T executeSupplier(Supplier<T> supplier) {
        return decorateSupplier(this, supplier).get();
    }

    /**
     * Decorates and executes the decorated Callable.
     *
     * @param callable the original Callable
     * @param <T>      the result type of callable
     * @return the result of the decorated Callable.
     * @throws Exception if unable to compute a result
     */
    default <T> T executeCallable(Callable<T> callable) throws Exception {
        return decorateCallable(this, callable).call();
    }

    /**
     * Decorates and executes the decorated Runnable.
     *
     * @param runnable the original Runnable
     */
    default void executeRunnable(Runnable runnable) {
        decorateRunnable(this, runnable).run();
    }

    /**
     * Decorates and executes the decorated Supplier.
     *
     * @param checkedSupplier the original Supplier
     * @param <T>             the type of results supplied by this supplier
     * @return the result of the decorated Supplier.
     * @throws Throwable if something goes wrong applying this function to the given arguments
     */
    default <T> T executeCheckedSupplier(CheckedSupplier<T> checkedSupplier) throws Throwable {
        return decorateCheckedSupplier(this, checkedSupplier).get();
    }

    /**
     * Decorates and executes the decorated CompletionStage.
     *
     * @param supplier the original CompletionStage
     * @param <T>      the type of results supplied by this supplier
     * @return the decorated CompletionStage.
     */
    default <T> CompletionStage<T> executeCompletionStage(Supplier<CompletionStage<T>> supplier) {
        return decorateCompletionStage(this, supplier).get();
    }

    /**
     * Returns a supplier which is decorated by a bulkhead.
     *
     * @param bulkhead the Bulkhead
     * @param supplier the original supplier
     * @param <T>      the type of results supplied by this supplier
     * @return a supplier which is decorated by a Bulkhead.
     */
    static <T> CheckedSupplier<T> decorateCheckedSupplier(AdaptiveBulkhead bulkhead,
                                                          CheckedSupplier<T> supplier) {
        return () -> {
            long start = 0;
            boolean isFailed = false;
            bulkhead.acquirePermission();
            try {
                start = System.currentTimeMillis();
                return supplier.get();
            } catch (Exception e) {
                bulkhead.onError(start, TimeUnit.MILLISECONDS, e);
                isFailed = true;
                throw e;
            } finally {
                if (start != 0 && !isFailed) {
                    bulkhead.onSuccess(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
                }
            }
        };
    }

    /**
     * Returns a supplier which is decorated by a bulkhead.
     *
     * @param bulkhead the bulkhead
     * @param supplier the original supplier
     * @param <T>      the type of the returned CompletionStage's result
     * @return a supplier which is decorated by a Bulkhead.
     */
    static <T> Supplier<CompletionStage<T>> decorateCompletionStage(AdaptiveBulkhead bulkhead,
                                                                    Supplier<CompletionStage<T>> supplier) {
        return () -> {

            final CompletableFuture<T> promise = new CompletableFuture<>();

            if (!bulkhead.tryAcquirePermission()) {
                promise.completeExceptionally(
                    BulkheadFullException.createBulkheadFullException(bulkhead));
            } else {
                long start = System.currentTimeMillis();
                try {
                    supplier.get()
                        .whenComplete(
                            (result, throwable) -> {
                                if (throwable != null) {
                                    bulkhead.onError(start, TimeUnit.MILLISECONDS, throwable);
                                    promise.completeExceptionally(throwable);
                                } else {
                                    bulkhead.onSuccess(System.currentTimeMillis() - start,
                                        TimeUnit.MILLISECONDS);
                                    promise.complete(result);
                                }
                            }
                        );
                } catch (Exception e) {
                    bulkhead.onError(start, TimeUnit.MILLISECONDS, e);
                    promise.completeExceptionally(e);
                }
            }
            return promise;
        };
    }

    /**
     * Returns a supplier of type Future which is decorated by a bulkhead. AdaptiveBulkhead will reserve permission until {@link Future#get()}
     * or {@link Future#get(long, TimeUnit)} is evaluated even if the underlying call took less time to return. Any delays in evaluating
     * future will result in holding of permission in the underlying Semaphore.
     *
     * @param bulkhead the bulkhead
     * @param supplier the original supplier
     * @param <T>      the type of the returned Future result
     * @return a supplier which is decorated by a AdaptiveBulkhead.
     */
    static <T> Supplier<Future<T>> decorateFuture(AdaptiveBulkhead bulkhead, Supplier<Future<T>> supplier) {
        return () -> {
            if (!bulkhead.tryAcquirePermission()) {
                final CompletableFuture<T> promise = new CompletableFuture<>();
                promise.completeExceptionally(BulkheadFullException.createBulkheadFullException(bulkhead));
                return promise;
            }
            long start = bulkhead.getCurrentTimestamp();
            try {
                return new BulkheadFuture<T>(bulkhead, supplier.get(), start);
            } catch (Throwable e) {
                bulkhead.onError(start, bulkhead.getTimestampUnit(), e);
                throw e;
            }
        };
    }

    /**
     * Returns a runnable which is decorated by a bulkhead.
     *
     * @param bulkhead the bulkhead
     * @param runnable the original runnable
     * @return a runnable which is decorated by a Bulkhead.
     */
    static CheckedRunnable decorateCheckedRunnable(AdaptiveBulkhead bulkhead,
                                                   CheckedRunnable runnable) {
        return () -> {
            long start = 0;
            boolean isFailed = false;
            bulkhead.acquirePermission();
            try {
                start = bulkhead.getCurrentTimestamp();
                runnable.run();
            } catch (Exception e) {
                isFailed = true;
                bulkhead.onError(start, bulkhead.getTimestampUnit(), e);
                throw e;
            } finally {
                if (start != 0 && !isFailed) {
                    bulkhead.onSuccess(bulkhead.getCurrentTimestamp() - start, bulkhead.getTimestampUnit());
                }
            }
        };
    }

    /**
     * Returns a callable which is decorated by a bulkhead.
     *
     * @param bulkhead the bulkhead
     * @param callable the original Callable
     * @param <T>      the result type of callable
     * @return a supplier which is decorated by a Bulkhead.
     */
    static <T> Callable<T> decorateCallable(AdaptiveBulkhead bulkhead, Callable<T> callable) {
        return () -> {
            long start = 0;
            boolean isFailed = false;
            bulkhead.acquirePermission();
            try {
                start = bulkhead.getCurrentTimestamp();
                return callable.call();
            } catch (Exception e) {
                isFailed = true;
                bulkhead.onError(start, bulkhead.getTimestampUnit(), e);
                throw e;
            } finally {
                if (start != 0 && !isFailed) {
                    bulkhead.onSuccess(bulkhead.getCurrentTimestamp() - start, bulkhead.getTimestampUnit());
                }
            }
        };
    }

    /**
     * Returns a supplier which is decorated by a bulkhead.
     *
     * @param bulkhead the bulkhead
     * @param supplier the original supplier
     * @param <T>      the type of results supplied by this supplier
     * @return a supplier which is decorated by a Bulkhead.
     */
    static <T> Supplier<T> decorateSupplier(AdaptiveBulkhead bulkhead, Supplier<T> supplier) {
        return () -> {
            long start = 0;
            boolean isFailed = false;
            bulkhead.acquirePermission();
            try {
                start = bulkhead.getCurrentTimestamp();
                return supplier.get();
            } catch (Exception e) {
                isFailed = true;
                bulkhead.onError(start, bulkhead.getTimestampUnit(), e);
                throw e;
            } finally {
                if (start != 0 && !isFailed) {
                    bulkhead.onSuccess(bulkhead.getCurrentTimestamp() - start, bulkhead.getTimestampUnit());
                }
            }
        };
    }

    /**
     * Returns a consumer which is decorated by a bulkhead.
     *
     * @param bulkhead the bulkhead
     * @param consumer the original consumer
     * @param <T>      the type of the input to the consumer
     * @return a consumer which is decorated by a Bulkhead.
     */
    static <T> Consumer<T> decorateConsumer(AdaptiveBulkhead bulkhead, Consumer<T> consumer) {
        return t -> {
            long start = 0;
            boolean failed = false;
            bulkhead.acquirePermission();
            try {
                start = bulkhead.getCurrentTimestamp();
                consumer.accept(t);
            } catch (Exception e) {
                failed = true;
                bulkhead.onError(start, bulkhead.getTimestampUnit(), e);
                throw e;
            } finally {
                if (start != 0 && !failed) {
                    bulkhead.onSuccess(bulkhead.getCurrentTimestamp() - start, bulkhead.getTimestampUnit());
                }
            }
        };
    }

    /**
     * Returns a consumer which is decorated by a bulkhead.
     *
     * @param bulkhead the bulkhead
     * @param consumer the original consumer
     * @param <T>      the type of the input to the consumer
     * @return a consumer which is decorated by a Bulkhead.
     */
    static <T> CheckedConsumer<T> decorateCheckedConsumer(AdaptiveBulkhead bulkhead,
                                                          CheckedConsumer<T> consumer) {
        return t -> {
            long start = 0;
            boolean failed = false;
            bulkhead.acquirePermission();
            try {
                start = bulkhead.getCurrentTimestamp();
                consumer.accept(t);
            } catch (Exception e) {
                failed = true;
                bulkhead.onError(start, bulkhead.getTimestampUnit(), e);
                throw e;
            } finally {
                if (start != 0 && !failed) {
                    bulkhead.onSuccess(bulkhead.getCurrentTimestamp() - start, bulkhead.getTimestampUnit());
                }
            }
        };
    }

    /**
     * Returns a runnable which is decorated by a bulkhead.
     *
     * @param bulkhead the bulkhead
     * @param runnable the original runnable
     * @return a runnable which is decorated by a bulkhead.
     */
    static Runnable decorateRunnable(AdaptiveBulkhead bulkhead, Runnable runnable) {
        return () -> {
            long start = 0;
            boolean failed = false;
            bulkhead.acquirePermission();
            try {
                start = bulkhead.getCurrentTimestamp();
                runnable.run();
            } catch (Exception e) {
                failed = true;
                bulkhead.onError(start, bulkhead.getTimestampUnit(), e);
                throw e;
            } finally {
                if (start != 0 && !failed) {
                    bulkhead.onSuccess(bulkhead.getCurrentTimestamp() - start, bulkhead.getTimestampUnit());
                }
            }
        };
    }

    /**
     * Returns a function which is decorated by a bulkhead.
     *
     * @param bulkhead the bulkhead
     * @param function the original function
     * @param <T>      the type of the input to the function
     * @param <R>      the type of the result of the function
     * @return a function which is decorated by a bulkhead.
     */
    static <T, R> Function<T, R> decorateFunction(AdaptiveBulkhead bulkhead,
                                                  Function<T, R> function) {
        return (T t) -> {
            long start = 0;
            boolean failed = false;
            bulkhead.acquirePermission();
            try {
                start = bulkhead.getCurrentTimestamp();
                return function.apply(t);
            } catch (Exception e) {
                failed = true;
                bulkhead.onError(start, bulkhead.getTimestampUnit(), e);
                throw e;
            } finally {
                if (start != 0 && !failed) {
                    bulkhead.onSuccess(bulkhead.getCurrentTimestamp() - start, bulkhead.getTimestampUnit());
                }
            }
        };
    }

    /**
     * Returns a function which is decorated by a bulkhead.
     *
     * @param bulkhead the bulkhead
     * @param function the original function
     * @param <T>      the type of the input to the function
     * @param <R>      the type of the result of the function
     * @return a function which is decorated by a bulkhead.
     */
    static <T, R> CheckedFunction<T, R> decorateCheckedFunction(AdaptiveBulkhead bulkhead,
                                                                CheckedFunction<T, R> function) {
        return (T t) -> {
            long start = 0;
            boolean failed = false;
            bulkhead.acquirePermission();
            try {
                start = bulkhead.getCurrentTimestamp();
                return function.apply(t);
            } catch (Exception e) {
                failed = true;
                bulkhead.onError(start, bulkhead.getTimestampUnit(), e);
                throw e;
            } finally {
                if (start != 0 && !failed) {
                    bulkhead.onSuccess(bulkhead.getCurrentTimestamp() - start, bulkhead.getTimestampUnit());
                }
            }
        };
    }

    /**
     * Create a Bulkhead with a default configuration.
     *
     * @param name the name of the bulkhead
     * @return a Bulkhead instance
     */
    static AdaptiveBulkhead ofDefaults(String name) {
        return new AdaptiveBulkheadStateMachine(name, AdaptiveBulkheadConfig.ofDefaults());
    }

    /**
     * Creates a bulkhead with a custom configuration
     *
     * @param name   the name of the bulkhead
     * @param config a custom BulkheadConfig configuration
     * @return a Bulkhead instance
     */
    static AdaptiveBulkhead of(String name, AdaptiveBulkheadConfig config) {
        return new AdaptiveBulkheadStateMachine(name, config);
    }

    /**
     * Creates a bulkhead with a custom configuration
     *
     * @param name                   the name of the bulkhead
     * @param bulkheadConfigSupplier custom configuration supplier
     * @return a Bulkhead instance
     */
    static AdaptiveBulkhead of(String name,
                               Supplier<AdaptiveBulkheadConfig> bulkheadConfigSupplier) {
        return new AdaptiveBulkheadStateMachine(name, bulkheadConfigSupplier.get());
    }

    void transitionToCongestionAvoidance();

    void transitionToSlowStart();

    /**
     * States of the AdaptiveBulkhead.
     */
    enum State {
        /**
         * A DISABLED adaptive bulkhead is not operating (no state transition, no events) and no
         * limit changes.
         */
        // TODO not implemented
        DISABLED(3, false),

        SLOW_START(1, true),

        CONGESTION_AVOIDANCE(2, true);

        public final boolean allowPublish;
        private final int order;

        /**
         * Order is a FIXED integer, it should be preserved regardless of the ordinal number of the
         * enumeration. While a State.ordinal() does mostly the same, it is prone to changing the
         * order based on how the programmer  sets the enum. If more states are added the "order"
         * should be preserved. For example, if there is a state inserted between CLOSED and
         * HALF_OPEN (say FIXED_OPEN) then the order of HALF_OPEN remains at 2 and the new state
         * takes 3 regardless of its order in the enum.
         *
         * @param order
         * @param allowPublish
         */
        State(int order, boolean allowPublish) {
            this.order = order;
            this.allowPublish = allowPublish;
        }

        public int getOrder() {
            return order;
        }
    }

    interface Metrics extends Bulkhead.Metrics {

        /**
         * Returns the current failure rate in percentage. If the number of measured calls is below
         * the minimum number of measured calls, it returns -1.
         *
         * @return the failure rate in percentage
         */
        float getFailureRate();

        /**
         * Returns the current percentage of calls which were slower than a certain threshold. If
         * the number of measured calls is below the minimum number of measured calls, it returns
         * -1.
         *
         * @return the failure rate in percentage
         */
        float getSlowCallRate();

        /**
         * Returns the current total number of calls which were slower than a certain threshold.
         *
         * @return the current total number of calls which were slower than a certain threshold
         */
        int getNumberOfSlowCalls();

        /**
         * Returns the current number of successful calls which were slower than a certain
         * threshold.
         *
         * @return the current number of successful calls which were slower than a certain threshold
         */
        int getNumberOfSlowSuccessfulCalls();

        /**
         * Returns the current number of failed calls which were slower than a certain threshold.
         *
         * @return the current number of failed calls which were slower than a certain threshold
         */
        int getNumberOfSlowFailedCalls();

        /**
         * Returns the current total number of buffered calls in the sliding window.
         *
         * @return he current total number of buffered calls in the sliding window
         */
        int getNumberOfBufferedCalls();

        /**
         * Returns the current number of failed buffered calls in the sliding window.
         *
         * @return the current number of failed buffered calls in the sliding window
         */
        int getNumberOfFailedCalls();

        /**
         * Returns the current number of successful buffered calls in the sliding window.
         *
         * @return the current number of successful buffered calls in the sliding window
         */
        int getNumberOfSuccessfulCalls();

        void resetRecords();
    }

    /**
     * An EventPublisher which can be used to register event consumers.
     */
    interface AdaptiveEventPublisher extends EventPublisher<AdaptiveBulkheadEvent> {

        EventPublisher onLimitChanged(EventConsumer<BulkheadOnLimitChangedEvent> eventConsumer);

        EventPublisher onSuccess(EventConsumer<BulkheadOnSuccessEvent> eventConsumer);

        EventPublisher onError(EventConsumer<BulkheadOnErrorEvent> eventConsumer);

        EventPublisher onIgnoredError(EventConsumer<BulkheadOnIgnoreEvent> eventConsumer);

        EventPublisher onStateTransition(
            EventConsumer<BulkheadOnStateTransitionEvent> eventConsumer);

    }

    /**
     * This class decorates future with AdaptiveBulkhead functionality around invocation.
     *
     * @param <T> of return type
     */
    final class BulkheadFuture<T> implements Future<T> {
        private final Future<T> future;
        private final OnceConsumer<AdaptiveBulkhead> onceToBulkhead;
        private final long start;

        BulkheadFuture(AdaptiveBulkhead bulkhead, Future<T> future) {
            this(bulkhead, future, bulkhead.getCurrentTimestamp());
        }

        BulkheadFuture(AdaptiveBulkhead bulkhead, Future<T> future, long start) {
            Objects.requireNonNull(future, "Non null Future is required to decorate");
            this.onceToBulkhead = OnceConsumer.of(bulkhead);
            this.future = future;
            this.start = start;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return future.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return future.isCancelled();
        }

        @Override
        public boolean isDone() {
            return future.isDone();
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            try {
                return future.get();
            } finally {
                // TODO onError?
                onceToBulkhead.applyOnce(b ->
                    b.onSuccess(b.getCurrentTimestamp() - start, b.getTimestampUnit()));
            }
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            try {
                return future.get(timeout, unit);
            } finally {
                // TODO onError?
                onceToBulkhead.applyOnce(b ->
                    b.onSuccess(b.getCurrentTimestamp() - start, b.getTimestampUnit()));
            }
        }
    }
}
