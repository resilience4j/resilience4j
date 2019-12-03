package io.github.resilience4j.bulkhead;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Abstraction to retrieve, copy and clean up values across thread boundary. This class is
 * specifically use for propagating {@link ThreadLocal} across different thread boundaries.
 *
 * @param <T> value type that is copied across thread boundary.
 */
public interface ContextPropagator<T> {

    /**
     * Retrieves value from the currently executing thread. This method should produce values (as
     * Supplier) that needs to be propagated to new thread.
     *
     * @return a Supplier producing the value from current thread
     */
    Supplier<Optional<T>> retrieve();

    /**
     * Copies value from the parent thread into new executing thread. This method is passed with the
     * values received from method {@link ContextPropagator#retrieve()} in the parent thread.
     *
     * @return a Consumer to set values in new thread.
     */
    Consumer<Optional<T>> copy();

    /**
     * CleanUp value before thread execution finish. This method is passed with the values received
     * from method {@link ContextPropagator#retrieve()} in the parent thread.
     *
     * @return a Consumer to cleanUp values.
     */
    Consumer<Optional<T>> cleanUp();

    /**
     * Method decorates supplier to copy variables across thread boundary.
     *
     * @param propagator the instance of {@link ContextPropagator}
     * @param supplier   the supplier to be decorated
     * @param <T>        the type of variable that cross thread boundary
     * @return decorated supplier of type T
     */
    static <T> Supplier<T> decorateSupplier(ContextPropagator propagator,
        Supplier<T> supplier) {
        final Optional value = (Optional) propagator.retrieve().get();
        return () -> {
            try {
                propagator.copy().accept(value);
                return supplier.get();
            } finally {
                propagator.cleanUp().accept(value);
            }
        };
    }

    /**
     * Method decorates runnable to copy variables across thread boundary.
     *
     * @param propagator the instance of {@link ContextPropagator}
     * @param runnable   the runnable to be decorated
     * @param <T>        the type of variable that cross thread boundary
     * @return decorated supplier of type T
     */
    static <T> Runnable decorateRunnable(ContextPropagator propagator,
        Runnable runnable) {
        final Optional value = (Optional) propagator.retrieve().get();
        return () -> {
            try {
                propagator.copy().accept(value);
                runnable.run();
            } finally {
                propagator.cleanUp().accept(value);
            }
        };
    }

    /**
     * An empty context propagator.
     *
     * @param <T> type.
     * @return an empty {@link ContextPropagator}
     */
    static <T> ContextPropagator<T> empty() {
        return new EmptyContextPropagator<>();
    }

    /**
     * A convenient implementation of empty {@link ContextPropagator}
     *
     * @param <T> type of class.
     */
    class EmptyContextPropagator<T> implements ContextPropagator<T> {

        @Override
        public Supplier<Optional<T>> retrieve() {
            return () -> Optional.empty();
        }

        @Override
        public Consumer<Optional<T>> copy() {
            return (t) -> {
            };
        }

        @Override
        public Consumer<Optional<T>> cleanUp() {
            return (t) -> {
            };
        }
    }
}
