/*
 *
 *  Copyright 2020 krnSaurabh
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
package io.github.resilience4j.core.functions;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * A {@link Consumer}-like interface which allows throwing Error.
 */
@FunctionalInterface
public interface CheckedConsumer<T> {
    void accept(T t) throws Throwable;

    default CheckedConsumer<T> andThen(CheckedConsumer<? super T> after) {
        Objects.requireNonNull(after, "after is null");
        return (T t) -> { accept(t); after.accept(t); };
    }

    default Consumer<T> unchecked() {
        return t -> {
            try {
                accept(t);
            } catch(Throwable x) {
                sneakyThrow(x);
            }
        };
    }

    @SuppressWarnings("unchecked")
    static <T extends Throwable, R> R sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}
