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
package io.github.resilience4j.metrics;

import com.codahale.metrics.Timer;

import java.util.function.Function;
import java.util.function.Supplier;

import javaslang.control.Try;

public interface Metrics {

    /**
     * Creates a timed checked supplier.

     * @param timer the timer to use
     * @param supplier the original supplier
     * @return a timed supplier
     */
    static <T> Try.CheckedSupplier<T> decorateCheckedSupplier(Timer timer, Try.CheckedSupplier<T> supplier){
        return () -> {
            Timer.Context context = timer.time();
            try {
                return supplier.get();
            } finally{
                context.stop();
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
            Timer.Context context = timer.time();
            try{
                runnable.run();
            } finally{
                context.stop();
            }
        };
    }

    /**
     * Creates a timed checked supplier.

     * @param timer the timer to use
     * @param supplier the original supplier
     * @return a timed supplier
     */
    static <T> Supplier<T> decorateSupplier(Timer timer, Supplier<T> supplier){
        return () -> {
            Timer.Context context = timer.time();
            try {
                return supplier.get();
            } finally{
                context.stop();
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
            Timer.Context context = timer.time();
            try{
                runnable.run();
            } finally{
                context.stop();
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
            Timer.Context context = timer.time();
            try{
                return function.apply(t);
            } finally{
                context.stop();
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
            Timer.Context context = timer.time();
            try{
                return function.apply(t);
            } finally{
                context.stop();
            }
        };
    }
}