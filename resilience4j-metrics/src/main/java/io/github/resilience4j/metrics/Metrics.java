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
package io.github.resilience4j.metrics;

import com.codahale.metrics.Timer;

import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

import javaslang.control.Try;

public interface Metrics {

    /**
     * Decorates and executes the decorated Callable.
     *
     * @param timer the timer to use
     * @param callable the original Callable
     * @param <T> the type of results supplied by this Callable
     * @return the result of the decorated Callable.
     */
    static <T> T executeCallable(Timer timer, Callable<T> callable) throws Exception {
        return decorateCallable(timer, callable).call();
    }

    /**
     * Creates a timed checked supplier.

     * @param timer the timer to use
     * @param supplier the original supplier
     * @return a timed supplier
     */
    static <T> Try.CheckedSupplier<T> decorateCheckedSupplier(Timer timer, Try.CheckedSupplier<T> supplier){
        return () -> {
            try (Timer.Context context = timer.time()) {
                return supplier.get();
            }
        };
    }

    /**
     * Creates a timed runnable.

     * @param timer the timer to use
     * @param runnable the original runnable
     * @return a timed runnable
     */
    static Try.CheckedRunnable decorateCheckedRunnable(Timer timer, Try.CheckedRunnable runnable){
        return () -> {
            try (Timer.Context context = timer.time()) {
                runnable.run();
            }
        };
    }

    /**
     * Decorates and executes the decorated Supplier.
     *
     * @param timer the timer to use
     * @param supplier the original Supplier
     * @param <T> the type of results supplied by this supplier
     * @return the result of the decorated Supplier.
     */
    static <T> T executeSupplier(Timer timer, Supplier<T> supplier){
        return decorateSupplier(timer, supplier).get();
    }

    /**
     * Creates a timed checked supplier.

     * @param timer the timer to use
     * @param supplier the original supplier
     * @return a timed supplier
     */
    static <T> Supplier<T> decorateSupplier(Timer timer, Supplier<T> supplier){
        return () -> {
            try (Timer.Context context = timer.time()) {
                return supplier.get();
            }
        };
    }

    /**
     * Creates a timed Callable.

     * @param timer the timer to use
     * @param callable the original Callable
     * @return a timed Callable
     */
    static <T> Callable<T> decorateCallable(Timer timer, Callable<T> callable){
        return () -> {
            try (Timer.Context context = timer.time()) {
                return callable.call();
            }
        };
    }


    /**
     * Creates a timed runnable.

     * @param timer the timer to use
     * @param runnable the original runnable
     * @return a timed runnable
     */
    static Runnable decorateRunnable(Timer timer, Runnable runnable){
        return () -> {
            try (Timer.Context context = timer.time()) {
                runnable.run();
            }
        };
    }


    /**
     * Creates a timed function.

     * @param timer the timer to use
     * @param function the original function
     * @return a timed function
     */
    static <T, R> Function<T, R> decorateFunction(Timer timer, Function<T, R> function){
        return (T t) -> {
            try (Timer.Context context = timer.time()) {
                return function.apply(t);
            }
        };
    }

    /**
     * Creates a timed function.

     * @param timer the timer to use
     * @param function the original function
     * @return a timed function
     */
    static <T, R> Try.CheckedFunction<T, R> decorateCheckedFunction(Timer timer, Try.CheckedFunction<T, R> function){
        return (T t) -> {
            try (Timer.Context context = timer.time()) {
                return function.apply(t);
            }
        };
    }
}