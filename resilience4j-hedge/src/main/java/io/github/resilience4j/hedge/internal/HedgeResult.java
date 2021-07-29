/*
 *
 *  Copyright 2021: Matthew Sandoz
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
package io.github.resilience4j.hedge.internal;

/**
 * Encapsulates results from hedging. Hedged calls can be overall successful or failed, and can be provided by either
 * one of the hedged calls or the primary call.
 *
 * @param <T> the parameterized type of the underlying object to which this result applies
 */
public class HedgeResult<T> {
    public final Throwable throwable;
    public final boolean failed;
    public final boolean fromPrimary;
    public final T value;

    private HedgeResult(T value, boolean fromPrimary, boolean failed, Throwable throwable) {
        this.fromPrimary = fromPrimary;
        this.value = value;
        this.failed = failed;
        this.throwable = throwable;
    }

    /**
     * Create a Hedge result
     *
     * @param value       the return value of the call. Undefined for errors.
     * @param fromPrimary whether or not the return value came from the primary call
     * @param failed      whether or not the call failed
     * @param throwable   the failure that occurred. Undefined for successful calls
     * @param <T>         the type of the underlying return value
     * @return a HedgeResult representing the outcome of the hedging
     */
    public static <T> io.github.resilience4j.hedge.internal.HedgeResult<T> of(T value, boolean fromPrimary, boolean failed, Throwable throwable) {
        return new io.github.resilience4j.hedge.internal.HedgeResult<>(value, fromPrimary, failed, throwable);
    }
}
