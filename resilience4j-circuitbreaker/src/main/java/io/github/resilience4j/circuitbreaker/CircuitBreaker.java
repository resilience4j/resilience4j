/*
 *
 *  Copyright 2017: Robert Winkler
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
package io.github.resilience4j.circuitbreaker;

import io.github.resilience4j.circuitbreaker.event.*;
import io.github.resilience4j.circuitbreaker.internal.CircuitBreakerStateMachine;
import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.functions.OnceConsumer;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A CircuitBreaker instance is thread-safe can be used to decorate multiple requests.
 * <p>
 * A {@link CircuitBreaker} manages the state of a backend system. The CircuitBreaker is implemented
 * via a finite state machine with five states: CLOSED, OPEN, HALF_OPEN, DISABLED AND FORCED_OPEN.
 * The CircuitBreaker does not know anything about the backend's state by itself, but uses the
 * information provided by the decorators via {@link CircuitBreaker#onSuccess} and {@link
 * CircuitBreaker#onError} events. Before communicating with the backend, the permission to do so
 * must be obtained via the method {@link CircuitBreaker#tryAcquirePermission()}.
 * <p>
 * The state of the CircuitBreaker changes from CLOSED to OPEN when the failure rate is greater than or
 * equal to a (configurable) threshold. Then, all access to the backend is rejected for a (configurable) time
 * duration. No further calls are permitted.
 * <p>
 * After the time duration has elapsed, the CircuitBreaker state changes from OPEN to HALF_OPEN and
 * allows a number of calls to see if the backend is still unavailable or has become available
 * again. If the failure rate is greater than or equal to the configured threshold, the state changes back to OPEN.
 * If the failure rate is below or equal to the threshold, the state changes back to CLOSED.
 */
public interface CircuitBreaker {
    /**
     * Returns a supplier which is decorated by a CircuitBreaker.
     *
     * @param circuitBreaker the CircuitBreaker
     * @param supplier       the original supplier
     * @param <T>            the type of the returned CompletionStage's result
     * @return a supplier which is decorated by a CircuitBreaker.
     */
    static <T> Supplier<CompletionStage<T>> decorateCompletionStage(
        CircuitBreaker circuitBreaker,
        Supplier<CompletionStage<T>> supplier
    ) {
        return () -> {

            final CompletableFuture<T> promise = new CompletableFuture<>();

            if (!circuitBreaker.tryAcquirePermission()) {
                promise.completeExceptionally(
                    CallNotPermittedException.createCallNotPermittedException(circuitBreaker));

            } else {
                final long start = circuitBreaker.getCurrentTimestamp();
                try {
                    supplier.get().whenComplete((result, throwable) -> {
                        long duration = circuitBreaker.getCurrentTimestamp() - start;
                        if (throwable != null) {
                            if (throwable instanceof Exception) {
                                circuitBreaker
                                    .onError(duration, circuitBreaker.getTimestampUnit(), throwable);
                            }
                            promise.completeExceptionally(throwable);
                        } else {
                            circuitBreaker.onResult(duration, circuitBreaker.getTimestampUnit(), result);
                            promise.complete(result);
                        }
                    });
                } catch (Exception exception) {
                    long duration = circuitBreaker.getCurrentTimestamp() - start;
                    circuitBreaker.onError(duration, circuitBreaker.getTimestampUnit(), exception);
                    promise.completeExceptionally(exception);
                }
            }

            return promise;
        };
    }

    /**
     * Returns a callable which is decorated by a CircuitBreaker.
     *
     * @param circuitBreaker the CircuitBreaker
     * @param callable       the original Callable
     * @param <T>            the result type of callable
     * @return a supplier which is decorated by a CircuitBreaker.
     */
    static <T> Callable<T> decorateCallable(CircuitBreaker circuitBreaker, Callable<T> callable) {
        return () -> {
            circuitBreaker.acquirePermission();
            final long start = circuitBreaker.getCurrentTimestamp();
            try {
                T result = callable.call();
                long duration = circuitBreaker.getCurrentTimestamp() - start;
                circuitBreaker.onResult(duration, circuitBreaker.getTimestampUnit(), result);
                return result;
            } catch (Exception exception) {
                // Do not handle java.lang.Error
                long duration = circuitBreaker.getCurrentTimestamp() - start;
                circuitBreaker.onError(duration, circuitBreaker.getTimestampUnit(), exception);
                throw exception;
            }
        };
    }

    /**
     * Returns a supplier which is decorated by a CircuitBreaker.
     *
     * @param circuitBreaker the CircuitBreaker
     * @param supplier       the original supplier
     * @param <T>            the type of results supplied by this supplier
     * @return a supplier which is decorated by a CircuitBreaker.
     */
    static <T> Supplier<T> decorateSupplier(CircuitBreaker circuitBreaker, Supplier<T> supplier) {
        return () -> {
            circuitBreaker.acquirePermission();
            final long start = circuitBreaker.getCurrentTimestamp();
            try {
                T result = supplier.get();
                long duration = circuitBreaker.getCurrentTimestamp() - start;
                circuitBreaker.onResult(duration, circuitBreaker.getTimestampUnit(), result);
                return result;
            } catch (Exception exception) {
                // Do not handle java.lang.Error
                long duration = circuitBreaker.getCurrentTimestamp() - start;
                circuitBreaker.onError(duration, circuitBreaker.getTimestampUnit(), exception);
                throw exception;
            }
        };
    }

    /**
     * Returns a consumer which is decorated by a CircuitBreaker.
     *
     * @param circuitBreaker the CircuitBreaker
     * @param consumer       the original consumer
     * @param <T>            the type of the input to the consumer
     * @return a consumer which is decorated by a CircuitBreaker.
     */
    static <T> Consumer<T> decorateConsumer(CircuitBreaker circuitBreaker, Consumer<T> consumer) {
        return (t) -> {
            circuitBreaker.acquirePermission();
            final long start = circuitBreaker.getCurrentTimestamp();
            try {
                consumer.accept(t);
                long duration = circuitBreaker.getCurrentTimestamp() - start;
                circuitBreaker.onSuccess(duration, circuitBreaker.getTimestampUnit());
            } catch (Exception exception) {
                // Do not handle java.lang.Error
                long duration = circuitBreaker.getCurrentTimestamp() - start;
                circuitBreaker.onError(duration, circuitBreaker.getTimestampUnit(), exception);
                throw exception;
            }
        };
    }

    /**
     * Returns a runnable which is decorated by a CircuitBreaker.
     *
     * @param circuitBreaker the CircuitBreaker
     * @param runnable       the original runnable
     * @return a runnable which is decorated by a CircuitBreaker.
     */
    static Runnable decorateRunnable(CircuitBreaker circuitBreaker, Runnable runnable) {
        return () -> {
            circuitBreaker.acquirePermission();
            final long start = circuitBreaker.getCurrentTimestamp();
            try {
                runnable.run();
                long duration = circuitBreaker.getCurrentTimestamp() - start;
                circuitBreaker.onSuccess(duration, circuitBreaker.getTimestampUnit());
            } catch (Exception exception) {
                // Do not handle java.lang.Error
                long duration = circuitBreaker.getCurrentTimestamp() - start;
                circuitBreaker.onError(duration, circuitBreaker.getTimestampUnit(), exception);
                throw exception;
            }
        };
    }

    /**
     * Returns a function which is decorated by a CircuitBreaker.
     *
     * @param circuitBreaker the CircuitBreaker
     * @param function       the original function
     * @param <T>            the type of the input to the function
     * @param <R>            the type of the result of the function
     * @return a function which is decorated by a CircuitBreaker.
     */
    static <T, R> Function<T, R> decorateFunction(CircuitBreaker circuitBreaker,
        Function<T, R> function) {
        return (T t) -> {
            circuitBreaker.acquirePermission();
            final long start = circuitBreaker.getCurrentTimestamp();
            try {
                R returnValue = function.apply(t);
                long duration = circuitBreaker.getCurrentTimestamp() - start;
                circuitBreaker.onResult(duration, circuitBreaker.getTimestampUnit(), returnValue);
                return returnValue;
            } catch (Exception exception) {
                // Do not handle java.lang.Error
                long duration = circuitBreaker.getCurrentTimestamp() - start;
                circuitBreaker.onError(duration, circuitBreaker.getTimestampUnit(), exception);
                throw exception;
            }
        };
    }

    /**
     * Creates a CircuitBreaker with a default CircuitBreaker configuration.
     *
     * @param name the name of the CircuitBreaker
     * @return a CircuitBreaker with a default CircuitBreaker configuration.
     */
    static CircuitBreaker ofDefaults(String name) {
        return new CircuitBreakerStateMachine(name);
    }

    /**
     * Creates a CircuitBreaker with a custom CircuitBreaker configuration.
     *
     * @param name                 the name of the CircuitBreaker
     * @param circuitBreakerConfig a custom CircuitBreaker configuration
     * @return a CircuitBreaker with a custom CircuitBreaker configuration.
     */
    static CircuitBreaker of(String name, CircuitBreakerConfig circuitBreakerConfig) {
        return new CircuitBreakerStateMachine(name, circuitBreakerConfig);
    }

    /**
     * Creates a CircuitBreaker with a custom CircuitBreaker configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name                 the name of the CircuitBreaker
     * @param circuitBreakerConfig a custom CircuitBreaker configuration
     * @param tags                 tags added to the Retry
     * @return a CircuitBreaker with a custom CircuitBreaker configuration.
     */
    static CircuitBreaker of(String name, CircuitBreakerConfig circuitBreakerConfig, Map<String, String> tags) {
        return new CircuitBreakerStateMachine(name, circuitBreakerConfig, tags);
    }

    /**
     * Creates a CircuitBreaker with a custom CircuitBreaker configuration.
     *
     * @param name                         the name of the CircuitBreaker
     * @param circuitBreakerConfigSupplier a supplier of a custom CircuitBreaker configuration
     * @return a CircuitBreaker with a custom CircuitBreaker configuration.
     */
    static CircuitBreaker of(String name,
        Supplier<CircuitBreakerConfig> circuitBreakerConfigSupplier) {
        return new CircuitBreakerStateMachine(name, circuitBreakerConfigSupplier);
    }

    /**
     * Creates a CircuitBreaker with a custom CircuitBreaker configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name                         the name of the CircuitBreaker
     * @param circuitBreakerConfigSupplier a supplier of a custom CircuitBreaker configuration
     * @param tags                         tags added to the CircuitBreaker
     * @return a CircuitBreaker with a custom CircuitBreaker configuration.
     */
    static CircuitBreaker of(String name,
        Supplier<CircuitBreakerConfig> circuitBreakerConfigSupplier, Map<String, String> tags) {
        return new CircuitBreakerStateMachine(name, circuitBreakerConfigSupplier, tags);
    }

    /**
     * Returns a supplier of type Future which is decorated by a CircuitBreaker. The elapsed time
     * includes {@link Future#get()} evaluation time even if the underlying call took less time to
     * return. Any delays in evaluating Future by caller will add towards total time.
     *
     * @param circuitBreaker the CircuitBreaker
     * @param supplier       the original supplier
     * @param <T>            the type of the returned Future's result
     * @return a supplier which is decorated by a CircuitBreaker.
     */
    static <T> Supplier<Future<T>> decorateFuture(CircuitBreaker circuitBreaker,
        Supplier<Future<T>> supplier) {
        return () -> {
            if (!circuitBreaker.tryAcquirePermission()) {
                CompletableFuture<T> promise = new CompletableFuture<>();
                promise.completeExceptionally(
                    CallNotPermittedException.createCallNotPermittedException(circuitBreaker));
                return promise;
            } else {
                final long start = circuitBreaker.getCurrentTimestamp();
                try {
                    return new CircuitBreakerFuture<>(circuitBreaker, supplier.get(), start);
                } catch (Exception e) {
                    long duration = circuitBreaker.getCurrentTimestamp() - start;
                    circuitBreaker.onError(duration, circuitBreaker.getTimestampUnit(), e);
                    throw e;
                }
            }
        };
    }

    /**
     * Acquires a permission to execute a call, only if one is available at the time of invocation.
     * If a call is not permitted, the number of not permitted calls is increased.
     * <p>
     * Returns false when the state is OPEN or FORCED_OPEN. Returns true when the state is CLOSED or
     * DISABLED. Returns true when the state is HALF_OPEN and further test calls are allowed.
     * Returns false when the state is HALF_OPEN and the number of test calls has been reached. If
     * the state is HALF_OPEN, the number of allowed test calls is decreased. Important: Make sure
     * to call onSuccess or onError after the call is finished. If the call is cancelled before it
     * is invoked, you have to release the permission again.
     *
     * @return {@code true} if a permission was acquired and {@code false} otherwise
     */
    boolean tryAcquirePermission();

    /**
     * Releases a permission.
     * <p>
     * Should only be used when a permission was acquired but not used. Otherwise use {@link
     * CircuitBreaker#onSuccess(long, TimeUnit)} or
     * {@link CircuitBreaker#onError(long, TimeUnit, Throwable)} to signal a completed or failed call.
     * <p>
     * If the state is HALF_OPEN, the number of allowed test calls is increased by one.
     */
    void releasePermission();

    /**
     * Try to obtain a permission to execute a call. If a call is not permitted, the number of not
     * permitted calls is increased.
     * <p>
     * Throws a CallNotPermittedException when the state is OPEN or FORCED_OPEN. Returns when the
     * state is CLOSED or DISABLED. Returns when the state is HALF_OPEN and further test calls are
     * allowed. Throws a CallNotPermittedException when the state is HALF_OPEN and the number of
     * test calls has been reached. If the state is HALF_OPEN, the number of allowed test calls is
     * decreased. Important: Make sure to call onSuccess or onError after the call is finished. If
     * the call is cancelled before it is invoked, you have to release the permission again.
     *
     * @throws CallNotPermittedException when CircuitBreaker is OPEN or HALF_OPEN and no further
     *                                   test calls are permitted.
     */
    void acquirePermission();

    /**
     * Records a failed call. This method must be invoked when a call failed.
     *
     * @param duration     The elapsed time duration of the call
     * @param durationUnit The duration unit
     * @param throwable    The throwable which must be recorded
     */
    void onError(long duration, TimeUnit durationUnit, Throwable throwable);

    /**
     * Records a successful call. This method must be invoked when a call was
     * successful.
     *
     * @param duration     The elapsed time duration of the call
     * @param durationUnit The duration unit
     */
    void onSuccess(long duration, TimeUnit durationUnit);

    /**
     * This method must be invoked when a call returned a result
     * and the result predicate should decide if the call was successful or not.
     *
     * @param duration     The elapsed time duration of the call
     * @param durationUnit The duration unit
     * @param result       The result of the protected function
     */
    void onResult(long duration, TimeUnit durationUnit, Object result);

    /**
     * Returns the circuit breaker to its original closed state, losing statistics.
     * <p>
     * Should only be used, when you want to want to fully reset the circuit breaker without
     * creating a new one.
     */
    void reset();

    /**
     * Transitions the state machine to CLOSED state. This call is idempotent and will not have
     * any effect if the state machine is already in CLOSED state.
     * <p>
     * Should only be used, when you want to force a state transition. State transition are normally
     * done internally.
     */
    void transitionToClosedState();

    /**
     * Transitions the state machine to OPEN state. This call is idempotent and will not have
     * any effect if the state machine is already in OPEN state.
     * <p>
     * Should only be used, when you want to force a state transition. State transition are normally
     * done internally.
     */
    void transitionToOpenState();

    /**
     * Transitions the state machine to HALF_OPEN state. This call is idempotent and will not have
     * any effect if the state machine is already in HALF_OPEN state.
     * <p>
     * Should only be used, when you want to force a state transition. State transition are normally
     * done internally.
     */
    void transitionToHalfOpenState();

    /**
     * Transitions the state machine to a DISABLED state, stopping state transition, metrics and
     * event publishing. This call is idempotent and will not have any effect if the
     * state machine is already in DISABLED state.
     * <p>
     * Should only be used, when you want to disable the circuit breaker allowing all calls to pass.
     * To recover from this state you must force a new state transition
     */
    void transitionToDisabledState();

    /**
     * Transitions the state machine to METRICS_ONLY state, stopping all state transitions but
     * continues to capture metrics and publish events. This call is idempotent and will not have
     * any effect if the state machine is already in METRICS_ONLY state.
     * <p>
     * Should only be used when you want to collect metrics while keeping the circuit breaker
     * disabled, allowing all calls to pass.
     * To recover from this state you must force a new state transition.
     */
    void transitionToMetricsOnlyState();

    /**
     * Transitions the state machine to a FORCED_OPEN state,  stopping state transition, metrics and
     * event publishing. This call is idempotent and will not have any effect if the state machine
     * is already in FORCED_OPEN state.
     * <p>
     * Should only be used, when you want to disable the circuit breaker allowing no call to pass.
     * To recover from this state you must force a new state transition
     */
    void transitionToForcedOpenState();

    /**
     * Returns the name of this CircuitBreaker.
     *
     * @return the name of this CircuitBreaker
     */
    String getName();

    /**
     * Returns the state of this CircuitBreaker.
     *
     * @return the state of this CircuitBreaker
     */
    State getState();

    /**
     * Returns the CircuitBreakerConfig of this CircuitBreaker.
     *
     * @return the CircuitBreakerConfig of this CircuitBreaker
     */
    CircuitBreakerConfig getCircuitBreakerConfig();

    /**
     * Returns the Metrics of this CircuitBreaker.
     *
     * @return the Metrics of this CircuitBreaker
     */
    Metrics getMetrics();

    /**
     * Returns an unmodifiable map with tags assigned to this Retry.
     *
     * @return the tags assigned to this Retry in an unmodifiable map
     */
    Map<String, String> getTags();

    /**
     * Returns an EventPublisher which can be used to register event consumers.
     *
     * @return an EventPublisher
     */
    EventPublisher getEventPublisher();

    /**
     * Returns the current time with respect to the CircuitBreaker currentTimeFunction.
     * Returns System.nanoTime() by default.
     *
     * @return current timestamp
     */
    long getCurrentTimestamp();

    /**
     * Returns the timeUnit of current timestamp.
     * Default is TimeUnit.NANOSECONDS.
     *
     * @return the timeUnit of current timestamp
     */
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
     * Returns a supplier which is decorated by a CircuitBreaker.
     *
     * @param supplier the original supplier
     * @param <T>      the type of results supplied by this supplier
     * @return a supplier which is decorated by a CircuitBreaker.
     */
    default <T> Supplier<T> decorateSupplier(Supplier<T> supplier) {
        return decorateSupplier(this, supplier);
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
     * Returns a callable which is decorated by a CircuitBreaker.
     *
     * @param callable the original Callable
     * @param <T>      the result type of callable
     * @return a supplier which is decorated by a CircuitBreaker.
     */
    default <T> Callable<T> decorateCallable(Callable<T> callable) {
        return decorateCallable(this, callable);
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
     * Returns a runnable which is decorated by a CircuitBreaker.
     *
     * @param runnable the original runnable
     * @return a runnable which is decorated by a CircuitBreaker.
     */
    default Runnable decorateRunnable(Runnable runnable) {
        return decorateRunnable(this, runnable);
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
     * Returns a supplier which is decorated by a CircuitBreaker.
     *
     * @param supplier the original supplier
     * @param <T>      the type of the returned CompletionStage's result
     * @return a supplier which is decorated by a CircuitBreaker.
     */
    default <T> Supplier<CompletionStage<T>> decorateCompletionStage(
        Supplier<CompletionStage<T>> supplier) {
        return decorateCompletionStage(this, supplier);
    }

    /**
     * Returns a consumer which is decorated by a CircuitBreaker.
     *
     * @param consumer the original consumer
     * @param <T>      the type of the input to the consumer
     * @return a consumer which is decorated by a CircuitBreaker.
     */
    default <T> Consumer<T> decorateConsumer(Consumer<T> consumer) {
        return decorateConsumer(this, consumer);
    }

    /**
     * Returns a supplier of type Future which is decorated by a CircuitBreaker. The elapsed time
     * includes {@link Future#get()} evaluation time even if the underlying call took less time to
     * return. Any delays in evaluating Future by caller will add towards total time.
     *
     * @param supplier the original supplier
     * @param <T>      the type of the returned CompletionStage's result
     * @return a supplier which is decorated by a CircuitBreaker.
     */
    default <T> Supplier<Future<T>> decorateFuture(Supplier<Future<T>> supplier) {
        return decorateFuture(this, supplier);
    }

    /**
     * States of the CircuitBreaker state machine.
     */
    enum State {
        /**
         * A DISABLED breaker is not operating (no state transition, no events) and allowing all
         * requests through.
         */
        DISABLED(3, false),
        /**
         * A METRICS_ONLY breaker is collecting metrics, publishing events and allowing all requests
         * through but is not transitioning to other states.
         */
        METRICS_ONLY(5, true),
        /**
         * A CLOSED breaker is operating normally and allowing requests through.
         */
        CLOSED(0, true),
        /**
         * An OPEN breaker has tripped and will not allow requests through.
         */
        OPEN(1, true),
        /**
         * A FORCED_OPEN breaker is not operating (no state transition, no events) and not allowing
         * any requests through.
         */
        FORCED_OPEN(4, false),
        /**
         * A HALF_OPEN breaker has completed its wait interval and will allow requests
         */
        HALF_OPEN(2, true);

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

    /**
     * State transitions of the CircuitBreaker state machine.
     */
    enum StateTransition {
        CLOSED_TO_CLOSED(State.CLOSED, State.CLOSED),
        CLOSED_TO_OPEN(State.CLOSED, State.OPEN),
        CLOSED_TO_DISABLED(State.CLOSED, State.DISABLED),
        CLOSED_TO_METRICS_ONLY(State.CLOSED, State.METRICS_ONLY),
        CLOSED_TO_FORCED_OPEN(State.CLOSED, State.FORCED_OPEN),
        HALF_OPEN_TO_HALF_OPEN(State.HALF_OPEN, State.HALF_OPEN),
        HALF_OPEN_TO_CLOSED(State.HALF_OPEN, State.CLOSED),
        HALF_OPEN_TO_OPEN(State.HALF_OPEN, State.OPEN),
        HALF_OPEN_TO_DISABLED(State.HALF_OPEN, State.DISABLED),
        HALF_OPEN_TO_METRICS_ONLY(State.HALF_OPEN, State.METRICS_ONLY),
        HALF_OPEN_TO_FORCED_OPEN(State.HALF_OPEN, State.FORCED_OPEN),
        OPEN_TO_OPEN(State.OPEN, State.OPEN),
        OPEN_TO_CLOSED(State.OPEN, State.CLOSED),
        OPEN_TO_HALF_OPEN(State.OPEN, State.HALF_OPEN),
        OPEN_TO_DISABLED(State.OPEN, State.DISABLED),
        OPEN_TO_METRICS_ONLY(State.OPEN, State.METRICS_ONLY),
        OPEN_TO_FORCED_OPEN(State.OPEN, State.FORCED_OPEN),
        FORCED_OPEN_TO_FORCED_OPEN(State.FORCED_OPEN, State.FORCED_OPEN),
        FORCED_OPEN_TO_CLOSED(State.FORCED_OPEN, State.CLOSED),
        FORCED_OPEN_TO_OPEN(State.FORCED_OPEN, State.OPEN),
        FORCED_OPEN_TO_DISABLED(State.FORCED_OPEN, State.DISABLED),
        FORCED_OPEN_TO_METRICS_ONLY(State.FORCED_OPEN, State.METRICS_ONLY),
        FORCED_OPEN_TO_HALF_OPEN(State.FORCED_OPEN, State.HALF_OPEN),
        DISABLED_TO_DISABLED(State.DISABLED, State.DISABLED),
        DISABLED_TO_CLOSED(State.DISABLED, State.CLOSED),
        DISABLED_TO_OPEN(State.DISABLED, State.OPEN),
        DISABLED_TO_FORCED_OPEN(State.DISABLED, State.FORCED_OPEN),
        DISABLED_TO_HALF_OPEN(State.DISABLED, State.HALF_OPEN),
        DISABLED_TO_METRICS_ONLY(State.DISABLED, State.METRICS_ONLY),
        METRICS_ONLY_TO_METRICS_ONLY(State.METRICS_ONLY, State.METRICS_ONLY),
        METRICS_ONLY_TO_CLOSED(State.METRICS_ONLY, State.CLOSED),
        METRICS_ONLY_TO_FORCED_OPEN(State.METRICS_ONLY, State.FORCED_OPEN),
        METRICS_ONLY_TO_DISABLED(State.METRICS_ONLY, State.DISABLED);

        private static final Map<Map.Entry<State, State>, StateTransition> STATE_TRANSITION_MAP = Arrays
            .stream(StateTransition.values())
            .collect(Collectors.toMap(v -> Map.entry(v.fromState, v.toState), Function.identity()));
        private final State fromState;
        private final State toState;

        StateTransition(State fromState, State toState) {
            this.fromState = fromState;
            this.toState = toState;
        }

        public static StateTransition transitionBetween(String name, State fromState,
            State toState) {
            final StateTransition stateTransition = STATE_TRANSITION_MAP
                .get(Map.entry(fromState, toState));
            if (stateTransition == null) {
                throw new IllegalStateTransitionException(name, fromState, toState);
            }
            return stateTransition;
        }

        public State getFromState() {
            return fromState;
        }

        public State getToState() {
            return toState;
        }

        public static boolean isInternalTransition(final StateTransition transition) {
            return transition.getToState() == transition.getFromState();
        }

        @Override
        public String toString() {
            return String.format("State transition from %s to %s", fromState, toState);
        }
    }

    /**
     * An EventPublisher can be used to register event consumers.
     */
    interface EventPublisher extends
        io.github.resilience4j.core.EventPublisher<CircuitBreakerEvent> {

        EventPublisher onSuccess(EventConsumer<CircuitBreakerOnSuccessEvent> eventConsumer);

        EventPublisher onError(EventConsumer<CircuitBreakerOnErrorEvent> eventConsumer);

        EventPublisher onStateTransition(
            EventConsumer<CircuitBreakerOnStateTransitionEvent> eventConsumer);

        EventPublisher onReset(EventConsumer<CircuitBreakerOnResetEvent> eventConsumer);

        EventPublisher onIgnoredError(
            EventConsumer<CircuitBreakerOnIgnoredErrorEvent> eventConsumer);

        EventPublisher onCallNotPermitted(
            EventConsumer<CircuitBreakerOnCallNotPermittedEvent> eventConsumer);

        EventPublisher onFailureRateExceeded(
            EventConsumer<CircuitBreakerOnFailureRateExceededEvent> eventConsumer);

        EventPublisher onSlowCallRateExceeded(
            EventConsumer<CircuitBreakerOnSlowCallRateExceededEvent> eventConsumer);
    }

    interface Metrics {

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
         * Returns the current total number of buffered calls in the ring buffer.
         *
         * @return he current total number of buffered calls in the ring buffer
         */
        int getNumberOfBufferedCalls();

        /**
         * Returns the current number of failed buffered calls in the ring buffer.
         *
         * @return the current number of failed buffered calls in the ring buffer
         */
        int getNumberOfFailedCalls();

        /**
         * Returns the current number of not permitted calls, when the state is OPEN.
         * <p>
         * The number of denied calls is always 0, when the CircuitBreaker state is CLOSED or
         * HALF_OPEN. The number of denied calls is only increased when the CircuitBreaker state is
         * OPEN.
         *
         * @return the current number of not permitted calls
         */
        long getNumberOfNotPermittedCalls();

        /**
         * Returns the current number of successful buffered calls in the ring buffer.
         *
         * @return the current number of successful buffered calls in the ring buffer
         */
        int getNumberOfSuccessfulCalls();
    }

    /**
     * This class decorates future to add CircuitBreaking functionality around invocation.
     *
     * @param <T> of return type
     */
    final class CircuitBreakerFuture<T> implements Future<T> {

        final private Future<T> future;
        final private OnceConsumer<CircuitBreaker> onceToCircuitbreaker;
        final private long start;

        CircuitBreakerFuture(CircuitBreaker circuitBreaker, Future<T> future) {
            this(circuitBreaker, future, circuitBreaker.getCurrentTimestamp());
        }

        CircuitBreakerFuture(CircuitBreaker circuitBreaker, Future<T> future, long start) {
            Objects.requireNonNull(future, "Non null Future is required to decorate");
            this.onceToCircuitbreaker = OnceConsumer.of(circuitBreaker);
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
                T v = future.get();
                onceToCircuitbreaker
                    .applyOnce(cb ->
                        cb.onResult(cb.getCurrentTimestamp() - start, cb.getTimestampUnit(), v)
                    );
                return v;
            } catch (CancellationException | InterruptedException e) {
                onceToCircuitbreaker.applyOnce(cb -> cb.releasePermission());
                throw e;
            } catch (Exception e) {
                onceToCircuitbreaker.applyOnce(
                    cb -> cb.onError(cb.getCurrentTimestamp() - start, cb.getTimestampUnit(), e));
                throw e;
            }
        }

        @Override
        public T get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
            try {
                T v = future.get(timeout, unit);
                onceToCircuitbreaker
                    .applyOnce(cb ->
                        cb.onResult(cb.getCurrentTimestamp()  - start, cb.getTimestampUnit(), v)
                    );
                return v;
            } catch (CancellationException | InterruptedException e) {
                onceToCircuitbreaker.applyOnce(CircuitBreaker::releasePermission);
                throw e;
            } catch (Exception e) {
                onceToCircuitbreaker.applyOnce(
                    cb -> cb.onError(cb.getCurrentTimestamp() - start, cb.getTimestampUnit(), e));
                throw e;
            }
        }
    }
}
