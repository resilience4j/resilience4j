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

import java.util.Optional;

/**
 * Encapsulates results from hedging. Hedged calls can be overall successful or failed, and can be provided by either
 * one of the hedged calls or the primary call.
 *
 * @param <T> the parameterized type of the underlying object to which this result applies
 */
public class HedgeResult<T> {
    public final Optional<Throwable> throwable;
    public final boolean fromPrimary;
    public final T value;

    private HedgeResult(T value, boolean fromPrimary, Optional<Throwable> throwable) {
        this.fromPrimary = fromPrimary;
        this.value = value;
        this.throwable = throwable;
    }

    /**
     * Create a Hedge result
     *
     * @param value       the return value of the call. Undefined for errors.
     * @param fromPrimary whether the return value came from the primary call
     * @param throwable   an Optional containing any failure that occurred.
     * @param <T>         the type of the underlying return value
     * @return a HedgeResult representing the outcome of the hedging
     */
    public static <T> HedgeResult<T> of(T value, boolean fromPrimary, Optional<Throwable> throwable) {
        return new HedgeResult<>(value, fromPrimary, throwable);
    }
}
