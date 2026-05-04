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
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A {@link ContextPropagator} implementation for Java 25+ Scoped Values (JEP 506).
 *
 * <p>Scoped Values provide a more efficient alternative to ThreadLocal for passing
 * immutable data to child tasks, especially in virtual thread environments. Unlike
 * ThreadLocal, Scoped Values:
 * <ul>
 *   <li>Are immutable once bound to a scope</li>
 *   <li>Are automatically cleaned up when the scope ends</li>
 *   <li>Don't cause virtual thread pinning</li>
 *   <li>Have lower memory overhead with virtual threads</li>
 * </ul>
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * // Define a ScopedValue
 * private static final ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();
 *
 * // Create a propagator for it
 * ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);
 *
 * // Use with ThreadPoolBulkhead
 * ThreadPoolBulkheadConfig config = ThreadPoolBulkheadConfig.custom()
 *     .contextPropagator(propagator)
 *     .build();
 *
 * // Run code with the scoped value bound
 * ScopedValue.where(REQUEST_ID, "req-123").run(() -> {
 *     bulkhead.executeSupplier(() -> {
 *         // REQUEST_ID is available here via propagator.getCapturedValue()
 *         return doWork();
 *     });
 * });
 * }</pre>
 *
 * <p><b>Integration Notes:</b>
 * <p>Since Scoped Values use a different binding model than ThreadLocal (scope-based
 * vs. set/get), this propagator captures the value and makes it available through
 * {@link #getCapturedValue()}. For automatic rebinding in the target thread, use
 * {@link ScopedValueContext} utility class which provides proper scope wrapping.
 *
 * @param <T> the type of value being propagated
 * @see ScopedValueContext
 * @see ContextPropagator
 * @since 3.0.0
 */
public final class ScopedValuePropagator<T> implements ContextPropagator<T> {

    private final ScopedValue<T> scopedValue;

    /**
     * Thread-local holder for captured scoped values during cross-thread propagation.
     * This is used as an intermediate storage mechanism because Scoped Values cannot
     * be directly "set" - they must be bound to a scope.
     */
    private static final ThreadLocal<Map<ScopedValue<?>, Object>> capturedValues =
        ThreadLocal.withInitial(HashMap::new);

    private ScopedValuePropagator(ScopedValue<T> scopedValue) {
        this.scopedValue = Objects.requireNonNull(scopedValue, "ScopedValue cannot be null");
    }

    /**
     * Creates a new ScopedValuePropagator for the given ScopedValue.
     *
     * @param scopedValue the ScopedValue to propagate
     * @param <T>         the type of the scoped value
     * @return a new ScopedValuePropagator instance
     * @throws NullPointerException if scopedValue is null
     */
    public static <T> ScopedValuePropagator<T> of(ScopedValue<T> scopedValue) {
        return new ScopedValuePropagator<>(scopedValue);
    }

    /**
     * Creates propagators for multiple ScopedValues.
     *
     * @param scopedValues the ScopedValues to create propagators for
     * @return a list of ScopedValuePropagator instances
     */
    @SafeVarargs
    public static List<ContextPropagator<?>> ofAll(ScopedValue<?>... scopedValues) {
        List<ContextPropagator<?>> propagators = new ArrayList<>(scopedValues.length);
        for (ScopedValue<?> sv : scopedValues) {
            propagators.add(new ScopedValuePropagator<>(sv));
        }
        return propagators;
    }

    /**
     * Retrieves the current value from the ScopedValue if it's bound, or from
     * the per-thread capture map as a fallback.
     *
     * <p>The fallback is what keeps multi-hop propagation working: when the
     * propagator is used via {@link ContextPropagator#decorateSupplier}, the
     * worker thread only receives {@link #copy()} — the real {@link ScopedValue}
     * is not re-bound on that thread. Without the fallback, a second decoration
     * performed on the worker would read {@link Optional#empty()} from
     * {@code scopedValue.isBound()} and silently drop the propagated value.
     *
     * @return a Supplier that returns an Optional containing the current value,
     *         or an empty Optional if neither the ScopedValue nor the capture
     *         map holds a value for this propagator
     */
    @Override
    public Supplier<Optional<T>> retrieve() {
        return () -> {
            if (scopedValue.isBound()) {
                return Optional.of(scopedValue.get());
            }
            @SuppressWarnings("unchecked")
            T carried = (T) capturedValues.get().get(scopedValue);
            return Optional.ofNullable(carried);
        };
    }

    /**
     * Copies the retrieved value to the target thread's capture storage.
     *
     * <p>Note: This stores the value in a ThreadLocal map for later access via
     * {@link #getCapturedValue()}. For proper Scoped Value rebinding, use
     * {@link ScopedValueContext#runWithScopedValues(List, Runnable)}.
     *
     * @return a Consumer that stores the value in the capture storage
     */
    @Override
    public Consumer<Optional<T>> copy() {
        return value -> value.ifPresent(v -> capturedValues.get().put(scopedValue, v));
    }

    /**
     * Clears the captured value from the target thread's capture storage.
     *
     * @return a Consumer that removes the value from the capture storage
     */
    @Override
    public Consumer<Optional<T>> clear() {
        return value -> capturedValues.get().remove(scopedValue);
    }

    /**
     * Gets the ScopedValue that this propagator wraps.
     *
     * @return the wrapped ScopedValue
     */
    public ScopedValue<T> getScopedValue() {
        return scopedValue;
    }

    /**
     * Gets the captured value from the current thread's capture storage.
     *
     * <p>This method should be called in the target thread after {@link #copy()}
     * has been invoked to retrieve the propagated value.
     *
     * @return an Optional containing the captured value, or empty if not captured
     */
    @SuppressWarnings("unchecked")
    public Optional<T> getCapturedValue() {
        return Optional.ofNullable((T) capturedValues.get().get(scopedValue));
    }

    /**
     * Checks if the underlying ScopedValue is currently bound in the current scope.
     *
     * @return true if the ScopedValue is bound, false otherwise
     */
    public boolean isBound() {
        return scopedValue.isBound();
    }

    /**
     * Gets the current value of the ScopedValue if bound.
     *
     * @return an Optional containing the current value, or empty if not bound
     */
    public Optional<T> getCurrentValue() {
        return scopedValue.isBound()
            ? Optional.of(scopedValue.get())
            : Optional.empty();
    }

    /**
     * Decorates a Supplier to execute within a scope where the captured values are bound.
     *
     * <p>This method provides proper Scoped Value rebinding by using
     * {@code ScopedValue.where().call()} to execute the supplier within a scope
     * where the captured value is bound to the ScopedValue.
     *
     * @param supplier the supplier to decorate
     * @param <R>      the return type of the supplier
     * @return a decorated supplier that executes with the scoped value bound
     */
    public <R> Supplier<R> decorateSupplierWithScope(Supplier<R> supplier) {
        final Optional<T> capturedValue = retrieve().get();
        return () -> {
            if (capturedValue.isPresent()) {
                try {
                    return ScopedValue.where(scopedValue, capturedValue.get())
                        .call(supplier::get);
                } catch (Exception e) {
                    if (e instanceof RuntimeException re) {
                        throw re;
                    }
                    throw new RuntimeException(e);
                }
            } else {
                return supplier.get();
            }
        };
    }

    /**
     * Decorates a Runnable to execute within a scope where the captured values are bound.
     *
     * @param runnable the runnable to decorate
     * @return a decorated runnable that executes with the scoped value bound
     */
    public Runnable decorateRunnableWithScope(Runnable runnable) {
        final Optional<T> capturedValue = retrieve().get();
        return () -> {
            if (capturedValue.isPresent()) {
                ScopedValue.where(scopedValue, capturedValue.get()).run(runnable);
            } else {
                runnable.run();
            }
        };
    }

    /**
     * Decorates a Callable to execute within a scope where the captured values are bound.
     *
     * @param callable the callable to decorate
     * @param <R>      the return type of the callable
     * @return a decorated callable that executes with the scoped value bound
     */
    public <R> Callable<R> decorateCallableWithScope(Callable<R> callable) {
        final Optional<T> capturedValue = retrieve().get();
        return () -> {
            if (capturedValue.isPresent()) {
                return ScopedValue.where(scopedValue, capturedValue.get()).call(() -> callable.call());
            } else {
                return callable.call();
            }
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScopedValuePropagator<?> that = (ScopedValuePropagator<?>) o;
        return Objects.equals(scopedValue, that.scopedValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scopedValue);
    }

    @Override
    public String toString() {
        return "ScopedValuePropagator{" +
            "scopedValue=" + scopedValue +
            ", isBound=" + scopedValue.isBound() +
            '}';
    }
}
