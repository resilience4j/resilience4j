/*
 *
 *  Copyright 2026 authors
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
package io.github.resilience4j.metrics.internal;

import com.codahale.metrics.Counter;

import java.util.function.LongSupplier;

import static java.util.Objects.requireNonNull;

/** A read-only counter whose lifetime value is maintained by its source. */
public final class FunctionCounter extends Counter {

    private final LongSupplier count;

    /**
     * Creates a read-only view of a counter maintained by the supplied source.
     *
     * @param count the source of the current lifetime count
     * @throws NullPointerException if the source is null
     */
    public FunctionCounter(LongSupplier count) {
        this.count = requireNonNull(count);
    }

    @Override
    public long getCount() {
        return count.getAsLong();
    }

    @Override
    public void inc() {
        throw immutable();
    }

    @Override
    public void inc(long n) {
        throw immutable();
    }

    @Override
    public void dec() {
        throw immutable();
    }

    @Override
    public void dec(long n) {
        throw immutable();
    }

    private static UnsupportedOperationException immutable() {
        return new UnsupportedOperationException("This counter is maintained by its source");
    }
}
