/*
 *
 * Copyright 2019
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package io.github.resilience4j.feign;

import feign.InvocationHandlerFactory.MethodHandler;
import feign.Target;
import io.github.resilience4j.core.functions.CheckedFunction;

import java.lang.reflect.Method;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

/**
 * Decorator that calls a fallback in the case that an exception is thrown.
 */
class FallbackDecorator<T> implements FeignDecorator {

    private final FallbackHandler<T> fallback;
    private final Predicate<Exception> filter;

    /**
     * Creates a fallback that will be called for every {@link Exception}.
     */
    FallbackDecorator(FallbackHandler<T> fallback) {
        this(fallback, ex -> true);
    }

    /**
     * Creates a fallback that will only be called for the specified {@link Exception}.
     */
    FallbackDecorator(FallbackHandler<T> fallback, Class<? extends Exception> filter) {
        this(fallback, filter::isInstance);
        requireNonNull(filter, "Filter cannot be null!");
    }

    /**
     * Creates a fallback that will only be called if the specified {@link Predicate} returns
     * <code>true</code>.
     */
    FallbackDecorator(FallbackHandler<T> fallback, Predicate<Exception> filter) {
        this.fallback = requireNonNull(fallback, "Fallback cannot be null!");
        this.filter = requireNonNull(filter, "Filter cannot be null!");
    }

    /**
     * Calls the fallback if the invocationCall throws an {@link Exception}.
     *
     * @throws IllegalArgumentException if the fallback object does not have a corresponding
     *                                  fallback method.
     */
    @Override
    public CheckedFunction<Object[], Object> decorate(
        CheckedFunction<Object[], Object> invocationCall,
        Method method,
        MethodHandler methodHandler,
        Target<?> target) {
        return fallback.decorate(invocationCall, method, filter);
    }
}
