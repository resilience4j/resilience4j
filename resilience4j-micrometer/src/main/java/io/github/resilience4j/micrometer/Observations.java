package io.github.resilience4j.micrometer;

import io.github.resilience4j.core.functions.CheckedFunction;
import io.github.resilience4j.core.functions.CheckedRunnable;
import io.github.resilience4j.core.functions.CheckedSupplier;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utility class for using {@link Observation}s.
 *
 * @author Marcin Grzejszczak
 * @since 2.0.0
 */
public final class Observations {

    private Observations() {
        throw new IllegalStateException("Should not instantiate a utility class");
    }

    /**
     * Creates an observation of a provided ObservationRegistry
     *
     * @param name                the name of the observation
     * @param observationRegistry the ObservationRegistry
     * @return an Observation instance
     */
    static Observation ofObservationRegistry(String name, ObservationRegistry observationRegistry) {
        return Observation.createNotStarted(name, observationRegistry);
    }

    /**
     * Creates an observation of a provided ObservationRegistry
     *
     * @param name                the name of the observation
     * @param context             context to be passed to the observation
     * @param observationRegistry the ObservationRegistry
     * @return an Observation instance
     */
    static Observation ofObservationRegistry(String name, Supplier<Observation.Context> context, ObservationRegistry observationRegistry) {
        return Observation.createNotStarted(name, context, observationRegistry);
    }

    /**
     * Creates an observed checked supplier.
     *
     * @param observation the observation to use
     * @param supplier    the original supplier
     * @return an observed supplier
     */
    static <T> CheckedSupplier<T> decorateCheckedSupplier(Observation observation,
                                                          CheckedSupplier<T> supplier) {
        return () -> observation.observeChecked(supplier::get);
    }

    /**
     * Creates an observed runnable.
     *
     * @param observation the observation to use
     * @param runnable    the original runnable
     * @return an observed runnable
     */
    static CheckedRunnable decorateCheckedRunnable(Observation observation, CheckedRunnable runnable) {
        return () -> observation.observeChecked(runnable::run);
    }

    /**
     * Creates an observed checked supplier.
     *
     * @param observation the observation to use
     * @param supplier    the original supplier
     * @return an observed supplier
     */
    static <T> Supplier<T> decorateSupplier(Observation observation, Supplier<T> supplier) {
        return () -> observation.observe(supplier);
    }

    /**
     * Creates an observed Callable.
     *
     * @param observation the observation to use
     * @param callable    the original Callable
     * @return an observed Callable
     */
    static <T> Callable<T> decorateCallable(Observation observation, Callable<T> callable) {
        return () -> {
            observation.start();
            try {
                return callable.call();
            } catch (Exception e) {
                observation.error(e);
                throw e;
            } finally {
                observation.stop();
            }
        };
    }

    /**
     * Creates an observed runnable.
     *
     * @param observation the observation to use
     * @param runnable    the original runnable
     * @return an observed runnable
     */
    static Runnable decorateRunnable(Observation observation, Runnable runnable) {
        return () -> observation.observe(runnable);
    }

    /**
     * Creates an observed function.
     *
     * @param observation the observation to use
     * @param function    the original function
     * @return an observed function
     */
    static <T, R> Function<T, R> decorateFunction(Observation observation, Function<T, R> function) {
        return (T t) -> observation.observe(() -> function.apply(t));
    }

    /**
     * Creates an observed function.
     *
     * @param observation the observation to use
     * @param function    the original function
     * @return an observed function
     */
    static <T, R> CheckedFunction<T, R> decorateCheckedFunction(Observation observation,
                                                                CheckedFunction<T, R> function) {
        return (T t) -> observation.observeChecked(() -> function.apply(t));
    }

    /**
     * @param observation   the observation to use
     * @param stageSupplier the CompletionStage Supplier
     * @return a decorated completion stage
     */
    static <T> Supplier<CompletionStage<T>> decorateCompletionStageSupplier(Observation observation,
                                                                            Supplier<CompletionStage<T>> stageSupplier) {
        return () -> {
            observation.start();
            try {
                final CompletionStage<T> stage = stageSupplier.get();

                stage.whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        observation.error(throwable);
                    }
                    observation.stop();
                });

                return stage;
            } catch (Throwable throwable) {
                observation.error(throwable).stop();
                throw throwable;
            }
        };
    }

    /**
     * Decorates and executes the decorated Runnable.
     *
     * @param observation observation
     * @param runnable the original Callable
     */
    static void executeRunnable(Observation observation, Runnable runnable) {
        decorateRunnable(observation, runnable).run();
    }

    /**
     * Decorates and executes the decorated Callable.
     *
     * @param observation observation
     * @param callable the original Callable
     * @param <T>      the type of results supplied by this Callable
     * @return the result of the decorated Callable.
     */
    static <T> T executeCallable(Observation observation, Callable<T> callable) throws Exception {
        return decorateCallable(observation, callable).call();
    }

    /**
     * Decorates and executes the decorated Supplier.
     *
     * @param observation observation
     * @param supplier the original Supplier
     * @param <T>      the type of results supplied by this supplier
     * @return the result of the decorated Supplier.
     */
    static <T> T executeSupplier(Observation observation, Supplier<T> supplier) {
        return decorateSupplier(observation, supplier).get();
    }

    /**
     * Decorates and executes the decorated CompletionStage Supplier.
     *
     * @param observation observation
     * @param supplier the CompletionStage Supplier
     * @param <T>      the type of results supplied by this supplier
     * @return the result of the decorated Supplier.
     */
    static <T> CompletionStage<T> executeCompletionStageSupplier(Observation observation,
                                                                 Supplier<CompletionStage<T>> supplier) {
        return decorateCompletionStageSupplier(observation, supplier).get();
    }

}
