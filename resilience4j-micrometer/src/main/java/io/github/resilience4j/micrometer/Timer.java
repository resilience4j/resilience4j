/*
 *
 *  Copyright 2023 Mariusz Kopylec
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
package io.github.resilience4j.micrometer;

import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.functions.CheckedConsumer;
import io.github.resilience4j.core.functions.CheckedFunction;
import io.github.resilience4j.core.functions.CheckedRunnable;
import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.micrometer.event.TimerEvent;
import io.github.resilience4j.micrometer.event.TimerOnFailureEvent;
import io.github.resilience4j.micrometer.event.TimerOnStartEvent;
import io.github.resilience4j.micrometer.event.TimerOnSuccessEvent;
import io.github.resilience4j.micrometer.internal.TimerImpl;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Collections.emptyMap;

/**
 * Collects metrics of the decorated operation.
 */
public interface Timer {

    /**
     * Creates a Timer with a default Timer configuration.
     *
     * @param name     the name of the Timer
     * @param registry the registry to bind Timer to
     * @return a Timer with a custom Timer configuration.
     */
    static Timer of(String name, MeterRegistry registry) {
        return of(name, registry, TimerConfig.ofDefaults(), emptyMap());
    }

    /**
     * Creates a Timer with a custom Timer configuration.
     *
     * @param name        the name of the Timer
     * @param registry    the registry to bind Timer to
     * @param timerConfig a custom Timer configuration
     * @return a Timer with a custom Timer configuration.
     */
    static Timer of(String name, MeterRegistry registry, TimerConfig timerConfig) {
        return of(name, registry, timerConfig, emptyMap());
    }

    /**
     * Creates a Timer with a custom Timer configuration.
     *
     * @param name        the name of the Timer
     * @param registry    the registry to bind Timer to
     * @param timerConfig a custom Timer configuration
     * @param tags        tags to assign to the Timer
     * @return a Timer with a custom Timer configuration.
     */
    static Timer of(String name, MeterRegistry registry, TimerConfig timerConfig, Map<String, String> tags) {
        return new TimerImpl(name, registry, timerConfig, tags);
    }

    /**
     * Decorates CompletionStageSupplier with Timer
     *
     * @param timer    the timer
     * @param supplier completion stage supplier
     * @param <T>      type of completion stage result
     * @return decorated supplier
     */
    static <T> Supplier<CompletionStage<T>> decorateCompletionStage(Timer timer, Supplier<CompletionStage<T>> supplier) {
        return () -> {
            CompletableFuture<T> promise = new CompletableFuture<>();
            Context context = timer.createContext();
            try {
                supplier.get().whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        context.onFailure(throwable);
                        promise.completeExceptionally(throwable);
                    } else {
                        context.onSuccess();
                        promise.complete(result);
                    }
                });
            } catch (Exception e) {
                context.onFailure(e);
                promise.completeExceptionally(e);
            }
            return promise;
        };
    }

    /**
     * Decorates CheckedSupplier with Timer
     *
     * @param timer    the timer
     * @param supplier the supplier
     * @param <T>      the supplier result
     * @return decorated supplier
     */
    static <T> CheckedSupplier<T> decorateCheckedSupplier(Timer timer, CheckedSupplier<T> supplier) {
        return () -> {
            Context context = timer.createContext();
            try {
                T result = supplier.get();
                context.onSuccess();
                return result;
            } catch (Exception e) {
                context.onFailure(e);
                throw e;
            }
        };
    }

    /**
     * Decorates CheckedRunnable with Timer
     *
     * @param timer    the timer
     * @param runnable the original runnable
     * @return decorated runnable
     */
    static CheckedRunnable decorateCheckedRunnable(Timer timer, CheckedRunnable runnable) {
        return () -> {
            Context context = timer.createContext();
            try {
                runnable.run();
                context.onSuccess();
            } catch (Exception e) {
                context.onFailure(e);
                throw e;
            }
        };
    }

    /**
     * Decorates CheckedFunction with Timer
     *
     * @param timer    the timer
     * @param function the original function
     * @param <T>      the type of the input to the function
     * @param <R>      the result type of the function
     * @return a decorated function
     */
    static <T, R> CheckedFunction<T, R> decorateCheckedFunction(Timer timer, CheckedFunction<T, R> function) {
        return (input) -> {
            Context context = timer.createContext();
            try {
                R result = function.apply(input);
                context.onSuccess();
                return result;
            } catch (Exception e) {
                context.onFailure(e);
                throw e;
            }
        };
    }

    /**
     * Decorates Supplier with Timer
     *
     * @param timer    the timer
     * @param supplier the original supplier
     * @param <T>      the type of results supplied by this supplier
     * @return a decorated supplier
     */
    static <T> Supplier<T> decorateSupplier(Timer timer, Supplier<T> supplier) {
        return () -> {
            Context context = timer.createContext();
            try {
                T result = supplier.get();
                context.onSuccess();
                return result;
            } catch (Exception e) {
                context.onFailure(e);
                throw e;
            }
        };
    }

    /**
     * Decorates Callable with Timer
     *
     * @param timer    the timer
     * @param callable the original callable
     * @param <T>      the type of results supplied by this callable
     * @return a decorated callable
     */
    static <T> Callable<T> decorateCallable(Timer timer, Callable<T> callable) {
        return () -> {
            Context context = timer.createContext();
            try {
                T result = callable.call();
                context.onSuccess();
                return result;
            } catch (Exception e) {
                context.onFailure(e);
                throw e;
            }
        };
    }

    /**
     * Decorates Runnable with Timer
     *
     * @param timer    the timer
     * @param runnable the original runnable
     * @return a decorated runnable
     */
    static Runnable decorateRunnable(Timer timer, Runnable runnable) {
        return () -> {
            Context context = timer.createContext();
            try {
                runnable.run();
                context.onSuccess();
            } catch (Exception e) {
                context.onFailure(e);
                throw e;
            }
        };
    }

    /**
     * Decorates Function with Timer
     *
     * @param timer    the timer
     * @param function the original function
     * @param <T>      the type of the input to the function
     * @param <R>      the result type of the function
     * @return a decorated function
     */
    static <T, R> Function<T, R> decorateFunction(Timer timer, Function<T, R> function) {
        return (input) -> {
            Context context = timer.createContext();
            try {
                R result = function.apply(input);
                context.onSuccess();
                return result;
            } catch (Exception e) {
                context.onFailure(e);
                throw e;
            }
        };
    }

    /**
     * Decorates Consumer with Timer
     *
     * @param timer    the timer
     * @param consumer the original consumer
     * @param <T>      the type of the input to the consumer
     * @return a decorated consumer
     */
    static <T> Consumer<T> decorateConsumer(Timer timer, Consumer<T> consumer) {
        return (input) -> {
            Context context = timer.createContext();
            try {
                consumer.accept(input);
                context.onSuccess();
            } catch (Exception e) {
                context.onFailure(e);
                throw e;
            }
        };
    }

    /**
     * Decorates CheckedConsumer with Timer
     *
     * @param timer    the timer
     * @param consumer the original consumer
     * @param <T>      the type of the input to the consumer
     * @return a decorated consumer
     */
    static <T> CheckedConsumer<T> decorateCheckedConsumer(Timer timer, CheckedConsumer<T> consumer) {
        return (input) -> {
            Context context = timer.createContext();
            try {
                consumer.accept(input);
                context.onSuccess();
            } catch (Exception e) {
                context.onFailure(e);
                throw e;
            }
        };
    }

    /**
     * Returns the name of this Timer.
     *
     * @return the name of this Timer
     */
    String getName();

    /**
     * Returns the TimerConfig of this Timer.
     *
     * @return the TimerConfig of this Timer
     */
    TimerConfig getTimerConfig();

    /**
     * Returns an unmodifiable map with tags assigned to this Timer.
     *
     * @return the tags assigned to this Timer in an unmodifiable map
     */
    Map<String, String> getTags();

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
     * Creates a Timer context and starts the timer.
     *
     * @return the Timer context
     */
    Context createContext();

    interface Context {

        /**
         * Records a decorated operation success.
         */
        void onSuccess();

        /**
         * Records a decorated operation failure.
         *
         * @param throwable The throwable thrown from the decorated operation
         */
        void onFailure(Throwable throwable);
    }

    /**
     * Returns an EventPublisher can be used to register event consumers.
     *
     * @return an EventPublisher
     */
    EventPublisher getEventPublisher();

    /**
     * An EventPublisher which subscribes to the reactive stream of TimerEvents and can be used to
     * register event consumers.
     * <p>
     * To understand when the handlers are called, see the documentation of the respective events.
     */
    interface EventPublisher extends io.github.resilience4j.core.EventPublisher<TimerEvent> {

        EventPublisher onStart(EventConsumer<TimerOnStartEvent> eventConsumer);

        EventPublisher onSuccess(EventConsumer<TimerOnSuccessEvent> eventConsumer);

        EventPublisher onFailure(EventConsumer<TimerOnFailureEvent> eventConsumer);
    }
}
