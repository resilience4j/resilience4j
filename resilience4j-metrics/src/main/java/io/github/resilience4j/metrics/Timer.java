package io.github.resilience4j.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
import io.github.resilience4j.metrics.internal.TimerImpl;
import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedRunnable;

import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.codahale.metrics.Timer.Context;

public interface Timer {

    /**
     * Starts the Timer
     */
    Context time();

    /**
     * Stops the Timer and records a failed call.
     * This method must be invoked when a call failed.
     */
    void onError(Context context);

    /**
     * Stops the Timer and records a successful call.
     */
    void onSuccess(Context context);

    /**
     * Returns the name of this Timer.
     *
     * @return the name of this Timer
     */
    String getName();

    /**
     * Returns the MetricRegistry of this Timer.
     *
     * @return the MetricRegistry of this Timer
     */
    MetricRegistry getMetricRegistry();

    /**
     * Returns the Metrics of this Timer.
     *
     * @return the Metrics of this Timer
     */
    Timer.Metrics getMetrics();

    /**
     * Creates a timer of a provided MetricRegistry
     *
     * @param name the name of the timer
     * @param metricRegistry the MetricRegistry
     * @return a Bulkhead instance
     */
    static Timer ofMetricRegistry(String name, MetricRegistry metricRegistry) {
        return new TimerImpl(name, metricRegistry);
    }

    /**
     * Creates a timer of a default MetricRegistry
     *
     * @param name the name of the timer
     * @return a Bulkhead instance
     */
    static Timer of(String name) {
        return new TimerImpl(name, new MetricRegistry());
    }


    /**
     * Decorates and executes the decorated Callable.
     *
     * @param callable the original Callable
     * @param <T> the type of results supplied by this Callable
     * @return the result of the decorated Callable.
     */
    default <T> T executeCallable(Callable<T> callable) throws Exception {
        return decorateCallable(this, callable).call();
    }

    /**
     * Creates a timed checked supplier.

     * @param timer the timer to use
     * @param supplier the original supplier
     * @return a timed supplier
     */
    static <T> CheckedFunction0<T> decorateCheckedSupplier(Timer timer, CheckedFunction0<T> supplier){
        return () -> {
            Context context = timer.time();
            try {
                T returnValue = supplier.apply();
                timer.onSuccess(context);
                return returnValue;
            }catch (Throwable e){
                timer.onError(context);
                throw e;
            }
        };
    }

    /**
     * Creates a timed runnable.

     * @param timer the timer to use
     * @param runnable the original runnable
     * @return a timed runnable
     */
    static CheckedRunnable decorateCheckedRunnable(Timer timer, CheckedRunnable runnable){
        return () -> {
            Context context = timer.time();
            try {
                runnable.run();
                timer.onSuccess(context);
            }catch (Throwable e){
                timer.onError(context);
                throw e;
            }
        };
    }

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
     * Creates a timed checked supplier.

     * @param timer the timer to use
     * @param supplier the original supplier
     * @return a timed supplier
     */
    static <T> Supplier<T> decorateSupplier(Timer timer, Supplier<T> supplier){
        return () -> {
            Context context = timer.time();
            try {
                T returnValue = supplier.get();
                timer.onSuccess(context);
                return returnValue;
            }catch (Throwable e){
                timer.onError(context);
                throw e;
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
            Context context = timer.time();
            try {
                T returnValue = callable.call();
                timer.onSuccess(context);
                return returnValue;
            }catch (Throwable e){
                timer.onError(context);
                throw e;
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
            Context context = timer.time();
            try {
                runnable.run();
                timer.onSuccess(context);
            }catch (Throwable e){
                timer.onError(context);
                throw e;
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
            Context context = timer.time();
            try {
                R returnValue = function.apply(t);
                timer.onSuccess(context);
                return returnValue;
            }catch (Throwable e){
                timer.onError(context);
                throw e;
            }
        };
    }

    /**
     * Creates a timed function.

     * @param timer the timer to use
     * @param function the original function
     * @return a timed function
     */
    static <T, R> CheckedFunction1<T, R> decorateCheckedFunction(Timer timer, CheckedFunction1<T, R> function){
        return (T t) -> {
            Context context = timer.time();
            try {
                R returnValue = function.apply(t);
                timer.onSuccess(context);
                return returnValue;
            }catch (Throwable e){
                timer.onError(context);
                throw e;
            }
        };
    }

    interface Metrics {

        /**
         * Returns the current number of total calls.
         *
         * @return the current number of total calls
         */
        long getNumberOfTotalCalls();

        /**
         * Returns the current number of successful calls.
         *
         * @return the current number of successful calls
         */
        long getNumberOfSuccessfulCalls();


        /**
         * Returns the current number of failed calls.
         *
         * @return the current number of failed calls
         */
        long getNumberOfFailedCalls();

        /**
         * Returns the fifteen-minute exponentially-weighted moving average rate at which events have
         * occurred since the meter was created.
         * <p/>
         * This rate has the same exponential decay factor as the fifteen-minute load average in the
         * {@code top} Unix command.
         *
         * @return the fifteen-minute exponentially-weighted moving average rate at which events have
         *         occurred since the meter was created
         */
        double getFifteenMinuteRate();

        /**
         * Returns the five-minute exponentially-weighted moving average rate at which events have
         * occurred since the meter was created.
         * <p/>
         * This rate has the same exponential decay factor as the five-minute load average in the {@code
         * top} Unix command.
         *
         * @return the five-minute exponentially-weighted moving average rate at which events have
         *         occurred since the meter was created
         */
        double getFiveMinuteRate();

        /**
         * Returns the mean rate at which events have occurred since the meter was created.
         *
         * @return the mean rate at which events have occurred since the meter was created
         */
        double getMeanRate();

        /**
         * Returns the one-minute exponentially-weighted moving average rate at which events have
         * occurred since the meter was created.
         * <p/>
         * This rate has the same exponential decay factor as the one-minute load average in the {@code
         * top} Unix command.
         *
         * @return the one-minute exponentially-weighted moving average rate at which events have
         *         occurred since the meter was created
         */
        double getOneMinuteRate();

        /**
         * Returns a snapshot of the values.
         *
         * @return a snapshot of the values
         */
        Snapshot getSnapshot();
    }
}
