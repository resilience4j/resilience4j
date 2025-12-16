/*
 * Copyright 2024 Resilience4j Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.core;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Utility class for working with Scoped Values (JEP 506) in conjunction with
 * Resilience4j's context propagation framework.
 *
 * <p>This class provides convenience methods for:
 * <ul>
 *   <li>Capturing current scoped values from multiple propagators</li>
 *   <li>Running code with captured scoped values bound</li>
 *   <li>Decorating suppliers, callables, and runnables with scoped value bindings</li>
 * </ul>
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * // Define scoped values
 * private static final ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();
 * private static final ScopedValue<String> USER_ID = ScopedValue.newInstance();
 *
 * // Create propagators
 * List<ScopedValuePropagator<?>> propagators = Arrays.asList(
 *     ScopedValuePropagator.of(REQUEST_ID),
 *     ScopedValuePropagator.of(USER_ID)
 * );
 *
 * // Capture current context
 * ScopedValueContext context = ScopedValueContext.capture(propagators);
 *
 * // Run code with captured context
 * executor.submit(() -> context.runWithContext(() -> {
 *     // REQUEST_ID and USER_ID are bound here
 *     return doWork();
 * }));
 * }</pre>
 *
 * @see ScopedValuePropagator
 * @see ContextPropagator
 * @since 3.0.0
 */
public final class ScopedValueContext {

    private final Map<ScopedValue<Object>, Object> capturedValues;

    @SuppressWarnings("unchecked")
    private ScopedValueContext(List<? extends ScopedValuePropagator<?>> propagators) {
        this.capturedValues = new HashMap<>();
        for (ScopedValuePropagator<?> propagator : propagators) {
            Optional<?> value = propagator.retrieve().get();
            if (value.isPresent()) {
                capturedValues.put(
                    (ScopedValue<Object>) propagator.getScopedValue(),
                    value.get()
                );
            }
        }
    }

    /**
     * Captures the current scoped values from the given propagators.
     *
     * <p>This method should be called in the source thread before submitting
     * tasks to an executor. The captured context can then be used to rebind
     * the scoped values in the target thread.
     *
     * @param propagators the list of ScopedValuePropagators to capture from
     * @return a ScopedValueContext containing the captured values
     * @throws NullPointerException if propagators is null
     */
    public static ScopedValueContext capture(List<? extends ScopedValuePropagator<?>> propagators) {
        Objects.requireNonNull(propagators, "Propagators list cannot be null");
        return new ScopedValueContext(propagators);
    }

    /**
     * Captures the current scoped values from the given propagators.
     *
     * @param propagators the propagators to capture from
     * @return a ScopedValueContext containing the captured values
     */
    @SafeVarargs
    public static ScopedValueContext capture(ScopedValuePropagator<?>... propagators) {
        Objects.requireNonNull(propagators, "Propagators cannot be null");
        return capture(List.of(propagators));
    }

    /**
     * Creates an empty context with no captured values.
     *
     * @return an empty ScopedValueContext
     */
    public static ScopedValueContext empty() {
        return new ScopedValueContext(List.of());
    }

    /**
     * Checks if this context has any captured values.
     *
     * @return true if there are captured values, false otherwise
     */
    public boolean hasValues() {
        return !capturedValues.isEmpty();
    }

    /**
     * Gets the number of captured values.
     *
     * @return the number of captured scoped values
     */
    public int size() {
        return capturedValues.size();
    }

    /**
     * Runs the given runnable with all captured scoped values bound.
     *
     * @param runnable the runnable to execute
     */
    public void runWithContext(Runnable runnable) {
        Objects.requireNonNull(runnable, "Runnable cannot be null");
        if (capturedValues.isEmpty()) {
            runnable.run();
            return;
        }
        buildCarrierAndRun(runnable);
    }

    /**
     * Calls the given supplier with all captured scoped values bound.
     *
     * @param supplier the supplier to execute
     * @param <T>      the return type
     * @return the result of the supplier
     */
    public <T> T callWithContext(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "Supplier cannot be null");
        if (capturedValues.isEmpty()) {
            return supplier.get();
        }
        return buildCarrierAndCall(supplier);
    }

    /**
     * Calls the given callable with all captured scoped values bound.
     *
     * @param callable the callable to execute
     * @param <T>      the return type
     * @return the result of the callable
     * @throws Exception if the callable throws an exception
     */
    public <T> T callWithContext(Callable<T> callable) throws Exception {
        Objects.requireNonNull(callable, "Callable cannot be null");
        if (capturedValues.isEmpty()) {
            return callable.call();
        }
        return buildCarrierAndCallChecked(callable);
    }

    /**
     * Decorates a runnable to execute with this context's scoped values bound.
     *
     * @param runnable the runnable to decorate
     * @return a decorated runnable
     */
    public Runnable decorateRunnable(Runnable runnable) {
        Objects.requireNonNull(runnable, "Runnable cannot be null");
        if (capturedValues.isEmpty()) {
            return runnable;
        }
        return () -> runWithContext(runnable);
    }

    /**
     * Decorates a supplier to execute with this context's scoped values bound.
     *
     * @param supplier the supplier to decorate
     * @param <T>      the return type
     * @return a decorated supplier
     */
    public <T> Supplier<T> decorateSupplier(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "Supplier cannot be null");
        if (capturedValues.isEmpty()) {
            return supplier;
        }
        return () -> callWithContext(supplier);
    }

    /**
     * Decorates a callable to execute with this context's scoped values bound.
     *
     * @param callable the callable to decorate
     * @param <T>      the return type
     * @return a decorated callable
     */
    public <T> Callable<T> decorateCallable(Callable<T> callable) {
        Objects.requireNonNull(callable, "Callable cannot be null");
        if (capturedValues.isEmpty()) {
            return callable;
        }
        return () -> callWithContext(callable);
    }

    /**
     * Builds a ScopedValue.Carrier with all captured values and runs the runnable.
     */
    private void buildCarrierAndRun(Runnable runnable) {
        List<Map.Entry<ScopedValue<Object>, Object>> entries = new ArrayList<>(capturedValues.entrySet());
        runWithNestedScopes(entries, 0, runnable);
    }

    /**
     * Builds a ScopedValue.Carrier with all captured values and calls the supplier.
     */
    private <T> T buildCarrierAndCall(Supplier<T> supplier) {
        List<Map.Entry<ScopedValue<Object>, Object>> entries = new ArrayList<>(capturedValues.entrySet());
        return callWithNestedScopes(entries, 0, supplier);
    }

    /**
     * Builds a ScopedValue.Carrier with all captured values and calls the callable.
     */
    private <T> T buildCarrierAndCallChecked(Callable<T> callable) throws Exception {
        List<Map.Entry<ScopedValue<Object>, Object>> entries = new ArrayList<>(capturedValues.entrySet());
        return callWithNestedScopesChecked(entries, 0, callable);
    }

    /**
     * Recursively nests scoped value bindings to run the runnable.
     */
    private void runWithNestedScopes(
        List<Map.Entry<ScopedValue<Object>, Object>> entries,
        int index,
        Runnable runnable
    ) {
        if (index >= entries.size()) {
            runnable.run();
            return;
        }
        Map.Entry<ScopedValue<Object>, Object> entry = entries.get(index);
        ScopedValue.where(entry.getKey(), entry.getValue())
            .run(() -> runWithNestedScopes(entries, index + 1, runnable));
    }

    /**
     * Recursively nests scoped value bindings to call the supplier.
     */
    private <T> T callWithNestedScopes(
        List<Map.Entry<ScopedValue<Object>, Object>> entries,
        int index,
        Supplier<T> supplier
    ) {
        if (index >= entries.size()) {
            return supplier.get();
        }
        Map.Entry<ScopedValue<Object>, Object> entry = entries.get(index);
        try {
            return ScopedValue.where(entry.getKey(), entry.getValue())
                .call(() -> callWithNestedScopes(entries, index + 1, supplier));
        } catch (Exception e) {
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Recursively nests scoped value bindings to call the callable.
     */
    private <T> T callWithNestedScopesChecked(
        List<Map.Entry<ScopedValue<Object>, Object>> entries,
        int index,
        Callable<T> callable
    ) throws Exception {
        if (index >= entries.size()) {
            return callable.call();
        }
        Map.Entry<ScopedValue<Object>, Object> entry = entries.get(index);
        return ScopedValue.where(entry.getKey(), entry.getValue())
            .call(() -> callWithNestedScopesChecked(entries, index + 1, callable));
    }

    /**
     * Utility method to run multiple propagators' captured values with proper scope binding.
     *
     * <p>This is a convenience method that combines capturing and running in one step.
     *
     * @param propagators the propagators to use
     * @param runnable    the runnable to execute
     */
    public static void runWithScopedValues(
        List<? extends ScopedValuePropagator<?>> propagators,
        Runnable runnable
    ) {
        capture(propagators).runWithContext(runnable);
    }

    /**
     * Utility method to call a supplier with multiple propagators' captured values bound.
     *
     * @param propagators the propagators to use
     * @param supplier    the supplier to execute
     * @param <T>         the return type
     * @return the result of the supplier
     */
    public static <T> T callWithScopedValues(
        List<? extends ScopedValuePropagator<?>> propagators,
        Supplier<T> supplier
    ) {
        return capture(propagators).callWithContext(supplier);
    }

    @Override
    public String toString() {
        return "ScopedValueContext{" +
            "capturedValues=" + capturedValues.size() + " entries" +
            '}';
    }
}
