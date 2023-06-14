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
package io.github.resilience4j.monitor;

import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.functions.CheckedConsumer;
import io.github.resilience4j.core.functions.CheckedFunction;
import io.github.resilience4j.core.functions.CheckedRunnable;
import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.monitor.event.MonitorEvent;
import io.github.resilience4j.monitor.event.MonitorOnFailureEvent;
import io.github.resilience4j.monitor.event.MonitorOnStartEvent;
import io.github.resilience4j.monitor.event.MonitorOnSuccessEvent;
import io.github.resilience4j.monitor.internal.MonitorImpl;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.System.nanoTime;
import static java.time.Duration.ofNanos;
import static java.util.Collections.emptyMap;

/**
 * Collects general information about the executed operation.
 * It is thread-safe and can be used to decorate multiple operations.
 */
public interface Monitor {

    String NONE_INPUT = "none";
    String NONE_OUTPUT = "none";

    /**
     * Creates a Monitor with a custom Monitor configuration.
     *
     * @param name          the name of the Monitor
     * @param monitorConfig a custom Monitor configuration
     * @return a Monitor with a custom Monitor configuration.
     */
    static Monitor of(String name, MonitorConfig monitorConfig) {
        return of(name, monitorConfig, emptyMap());
    }

    /**
     * Creates a Monitor with a custom Monitor configuration.
     *
     * @param name          the name of the Monitor
     * @param monitorConfig a custom Monitor configuration
     * @param tags          tags to assign to the Monitor
     * @return a Monitor with a custom Monitor configuration.
     */
    static Monitor of(String name, MonitorConfig monitorConfig, Map<String, String> tags) {
        return new MonitorImpl(name, monitorConfig, tags);
    }

    /**
     * Creates a Monitor with a custom Monitor configuration.
     *
     * @param name                  the name of the Monitor
     * @param monitorConfigSupplier a supplier of a custom Monitor configuration
     * @return a Monitor with a custom Monitor configuration.
     */
    static Monitor of(String name, Supplier<MonitorConfig> monitorConfigSupplier) {
        return of(name, monitorConfigSupplier.get(), emptyMap());
    }

    /**
     * Creates a Monitor with a custom Monitor configuration.
     *
     * @param name                  the name of the Monitor
     * @param monitorConfigSupplier a supplier of a custom Monitor configuration
     * @param tags                  tags to assign to the Monitor
     * @return a Monitor with a custom Monitor configuration.
     */
    static Monitor of(String name, Supplier<MonitorConfig> monitorConfigSupplier, Map<String, String> tags) {
        return new MonitorImpl(name, monitorConfigSupplier.get(), tags);
    }

    /**
     * Creates a Monitor with default configuration.
     *
     * @param name the name of the Monitor
     * @return a Monitor with default configuration
     */
    static Monitor ofDefaults(String name) {
        return of(name, MonitorConfig.ofDefaults(), emptyMap());
    }

    /**
     * Decorates CompletionStageSupplier with Monitor
     *
     * @param monitor  the monitor
     * @param supplier completion stage supplier
     * @param <T>      type of completion stage result
     * @return decorated supplier
     */
    static <T> Supplier<CompletionStage<T>> decorateCompletionStage(Monitor monitor, Supplier<CompletionStage<T>> supplier) {
        return () -> {
            CompletableFuture<T> promise = new CompletableFuture<>();
            long start = nanoTime();
            try {
                supplier.get().whenComplete((output, throwable) -> {
                    if (throwable != null) {
                        recordFailure(monitor, start, NONE_INPUT, throwable);
                        promise.completeExceptionally(throwable);
                    } else {
                        recordSuccess(monitor, start, NONE_INPUT, output);
                        promise.complete(output);
                    }
                });
            } catch (Exception e) {
                recordFailure(monitor, start, NONE_INPUT, e);
                promise.completeExceptionally(e);
            }
            return promise;
        };
    }

    /**
     * Decorates CheckedSupplier with Monitor
     *
     * @param monitor  the monitor
     * @param supplier the supplier
     * @param <T>      the supplier result
     * @return decorated supplier
     */
    static <T> CheckedSupplier<T> decorateCheckedSupplier(Monitor monitor, CheckedSupplier<T> supplier) {
        return () -> {
            long start = nanoTime();
            try {
                T output = supplier.get();
                recordSuccess(monitor, start, NONE_INPUT, output);
                return output;
            } catch (Exception e) {
                recordFailure(monitor, start, NONE_INPUT, e);
                throw e;
            }
        };
    }

    /**
     * Decorates CheckedRunnable with Monitor
     *
     * @param monitor  the monitor
     * @param runnable the original runnable
     * @return decorated runnable
     */
    static CheckedRunnable decorateCheckedRunnable(Monitor monitor, CheckedRunnable runnable) {
        return () -> {
            long start = nanoTime();
            try {
                runnable.run();
                recordSuccess(monitor, start, NONE_INPUT, NONE_OUTPUT);
            } catch (Exception e) {
                recordFailure(monitor, start, NONE_INPUT, e);
                throw e;
            }
        };
    }

    /**
     * Decorates CheckedFunction with Monitor
     *
     * @param monitor  the monitor
     * @param function the original function
     * @param <T>      the type of the input to the function
     * @param <R>      the result type of the function
     * @return a decorated function
     */
    static <T, R> CheckedFunction<T, R> decorateCheckedFunction(Monitor monitor, CheckedFunction<T, R> function) {
        return (input) -> {
            long start = nanoTime();
            try {
                R output = function.apply(input);
                recordSuccess(monitor, start, input, output);
                return output;
            } catch (Exception e) {
                recordFailure(monitor, start, input, e);
                throw e;
            }
        };
    }

    /**
     * Decorates Supplier with Monitor
     *
     * @param monitor  the monitor
     * @param supplier the original supplier
     * @param <T>      the type of results supplied by this supplier
     * @return a decorated supplier
     */
    static <T> Supplier<T> decorateSupplier(Monitor monitor, Supplier<T> supplier) {
        return () -> {
            long start = nanoTime();
            try {
                T output = supplier.get();
                recordSuccess(monitor, start, NONE_INPUT, output);
                return output;
            } catch (Exception e) {
                recordFailure(monitor, start, NONE_INPUT, e);
                throw e;
            }
        };
    }

    /**
     * Decorates Callable with Monitor
     *
     * @param monitor  the monitor
     * @param callable the original callable
     * @param <T>      the type of results supplied by this callable
     * @return a decorated callable
     */
    static <T> Callable<T> decorateCallable(Monitor monitor, Callable<T> callable) {
        return () -> {
            long start = nanoTime();
            try {
                T output = callable.call();
                recordSuccess(monitor, start, NONE_INPUT, output);
                return output;
            } catch (Exception e) {
                recordFailure(monitor, start, NONE_INPUT, e);
                throw e;
            }
        };
    }

    /**
     * Decorates Runnable with Monitor
     *
     * @param monitor  the monitor
     * @param runnable the original runnable
     * @return a decorated runnable
     */
    static Runnable decorateRunnable(Monitor monitor, Runnable runnable) {
        return () -> {
            long start = nanoTime();
            try {
                runnable.run();
                recordSuccess(monitor, start, NONE_INPUT, NONE_OUTPUT);
            } catch (Exception e) {
                recordFailure(monitor, start, NONE_INPUT, e);
                throw e;
            }
        };
    }

    /**
     * Decorates Function with Monitor
     *
     * @param monitor  the monitor
     * @param function the original function
     * @param <T>      the type of the input to the function
     * @param <R>      the result type of the function
     * @return a decorated function
     */
    static <T, R> Function<T, R> decorateFunction(Monitor monitor, Function<T, R> function) {
        return (input) -> {
            long start = nanoTime();
            try {
                R output = function.apply(input);
                recordSuccess(monitor, start, input, output);
                return output;
            } catch (Exception e) {
                recordFailure(monitor, start, input, e);
                throw e;
            }
        };
    }

    /**
     * Decorates Consumer with Monitor
     *
     * @param monitor  the monitor
     * @param consumer the original consumer
     * @param <T>      the type of the input to the consumer
     * @return a decorated consumer
     */
    static <T> Consumer<T> decorateConsumer(Monitor monitor, Consumer<T> consumer) {
        return (input) -> {
            long start = nanoTime();
            try {
                consumer.accept(input);
                recordSuccess(monitor, start, input, NONE_OUTPUT);
            } catch (Exception e) {
                recordFailure(monitor, start, input, e);
                throw e;
            }
        };
    }

    /**
     * Decorates CheckedConsumer with Monitor
     *
     * @param monitor  the monitor
     * @param consumer the original consumer
     * @param <T>      the type of the input to the consumer
     * @return a decorated consumer
     */
    static <T> CheckedConsumer<T> decorateCheckedConsumer(Monitor monitor, CheckedConsumer<T> consumer) {
        return (input) -> {
            long start = nanoTime();
            try {
                consumer.accept(input);
                recordSuccess(monitor, start, input, NONE_OUTPUT);
            } catch (Exception e) {
                recordFailure(monitor, start, input, e);
                throw e;
            }
        };
    }

    private static <T> void recordSuccess(Monitor monitor, long start, Object input, T output) {
        Duration duration = ofNanos(nanoTime() - start);
        String resultName = monitor.getMonitorConfig().getSuccessResultNameResolver().apply(output);
        monitor.onSuccess(duration, input, resultName, output);
    }

    private static void recordFailure(Monitor monitor, long start, Object input, Throwable throwable) {
        Duration duration = ofNanos(nanoTime() - start);
        String resultName = monitor.getMonitorConfig().getFailureResultNameResolver().apply(throwable);
        monitor.onFailure(duration, input, resultName, throwable);
    }

    /**
     * Returns the name of this Monitor.
     *
     * @return the name of this Monitor
     */
    String getName();

    /**
     * Returns the MonitorConfig of this Monitor.
     *
     * @return the MonitorConfig of this Monitor
     */
    MonitorConfig getMonitorConfig();

    /**
     * Returns an unmodifiable map with tags assigned to this Monitor.
     *
     * @return the tags assigned to this Monitor in an unmodifiable map
     */
    Map<String, String> getTags();

    /**
     * Returns an EventPublisher can be used to register event consumers.
     *
     * @return an EventPublisher
     */
    EventPublisher getEventPublisher();

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
     * Records a started operation execution. This method must be invoked when a call has started.
     *
     * @param input The input of the operation execution
     */
    void onStart(Object input);

    /**
     * Records a failed operation execution. This method must be invoked when a call failed.
     *
     * @param operationExecutionDuration The duration of the operation execution
     * @param input                      The input of the operation execution
     * @param resultName                 The result name of the operation execution
     * @param throwable                  The throwable thrown during the operation execution
     */
    void onFailure(Duration operationExecutionDuration, Object input, String resultName, Throwable throwable);

    /**
     * Records a successful operation execution. This method must be invoked when a call was successful.
     *
     * @param operationExecutionDuration The duration of the operation execution
     * @param input                      The input of the operation execution
     * @param resultName                 The result name of the operation execution
     * @param output                     The output of the operation execution
     */
    void onSuccess(Duration operationExecutionDuration, Object input, String resultName, Object output);

    /**
     * An EventPublisher which subscribes to the reactive stream of MonitorEvents and can be used to
     * register event consumers.
     * <p>
     * To understand when the handlers are called, see the documentation of the respective events.
     */
    interface EventPublisher extends io.github.resilience4j.core.EventPublisher<MonitorEvent> {

        EventPublisher onStart(EventConsumer<MonitorOnStartEvent> eventConsumer);

        EventPublisher onSuccess(EventConsumer<MonitorOnSuccessEvent> eventConsumer);

        EventPublisher onFailure(EventConsumer<MonitorOnFailureEvent> eventConsumer);
    }
}
