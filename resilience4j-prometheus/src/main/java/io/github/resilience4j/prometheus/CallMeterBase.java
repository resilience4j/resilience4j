package io.github.resilience4j.prometheus;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public interface CallMeterBase {

    Timer startTimer();

    /**
     * Decorates and executes the decorated Runnable.
     *
     * @param runnable the original Callable
     */
    default void executeRunnable(Runnable runnable) throws Exception {
        CallMeter.decorateRunnable(this, runnable).run();
    }

    /**
     * Decorates and executes the decorated Callable.
     *
     * @param callable the original Callable
     * @param <T>      the type of results supplied by this Callable
     * @return the result of the decorated Callable.
     */
    default <T> T executeCallable(Callable<T> callable) throws Exception {
        return CallMeter.decorateCallable(this, callable).call();
    }

    /**
     * Decorates and executes the decorated Supplier.
     *
     * @param supplier the original Supplier
     * @param <T>      the type of results supplied by this supplier
     * @return the result of the decorated Supplier.
     */
    default <T> T executeSupplier(Supplier<T> supplier) {
        return CallMeter.decorateSupplier(this, supplier).get();
    }

    /**
     * Decorates and executes the decorated CompletionStage Supplier.
     *
     * @param supplier the CompletionStage Supplier
     * @param <T>      the type of results supplied by this supplier
     * @return the result of the decorated Supplier.
     */
    default <T> CompletionStage<T> executeCompletionStageSupplier(
        Supplier<CompletionStage<T>> supplier) {
        return CallMeter.decorateCompletionStageSupplier(this, supplier).get();
    }

    interface Timer {

        /**
         * Stops the Timer and records a failed call. This method must be invoked when a call
         * failed.
         */
        void onError();

        /**
         * Stops the Timer and records a successful call.
         */
        void onSuccess();
    }


}
