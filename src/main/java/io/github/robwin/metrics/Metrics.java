package io.github.robwin.metrics;

import com.codahale.metrics.Timer;
import javaslang.control.Try;

import java.util.function.Function;
import java.util.function.Supplier;

public interface Metrics {

    /**
     * Creates a timed checked supplier.
     *
     * @param supplier the original supplier
     * @param timer the timer to use
     * @return a timed supplier
     */
    static <T> Try.CheckedSupplier<T> timedCheckedSupplier(Try.CheckedSupplier<T> supplier, Timer timer){
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
     *
     * @param runnable the original runnable
     * @param timer the timer to use
     * @return a timed runnable
     */
    static Try.CheckedRunnable timedCheckedRunnable(Try.CheckedRunnable runnable, Timer timer){
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
     *
     * @param supplier the original supplier
     * @param timer the timer to use
     * @return a timed supplier
     */
    static <T> Supplier<T> timedSupplier(Supplier<T> supplier, Timer timer){
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
     *
     * @param runnable the original runnable
     * @param timer the timer to use
     * @return a timed runnable
     */
    static Runnable timedRunnable(Runnable runnable, Timer timer){
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
     *
     * @param function the original function
     * @param timer the timer to use
     * @return a timed function
     */
    static <T, R> Function<T, R> timedFunction(Function<T, R> function, Timer timer){
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
     *
     * @param function the original function
     * @param timer the timer to use
     * @return a timed function
     */
    static <T, R> Try.CheckedFunction<T, R> timedCheckedFunction(Try.CheckedFunction<T, R> function, Timer timer){
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
