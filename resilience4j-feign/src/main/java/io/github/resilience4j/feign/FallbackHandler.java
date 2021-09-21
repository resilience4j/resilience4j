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

import io.github.resilience4j.core.functions.CheckedFunction;

import java.lang.reflect.Method;
import java.util.function.Predicate;

/**
 * Handles a fallback of of type {@param T}.
 *
 * @param <T> the type of the fallback
 */
interface FallbackHandler<T> {

    CheckedFunction<Object[], Object> decorate(CheckedFunction<Object[], Object> invocationCall,
                                               Method method, Predicate<Exception> filter);

    default void validateFallback(T fallback, Method method) {
        if (fallback.getClass().isAssignableFrom(method.getDeclaringClass())) {
            throw new IllegalArgumentException("Cannot use the fallback ["
                + fallback.getClass() + "] for ["
                + method.getDeclaringClass() + "]!");
        }
    }

    default Method getFallbackMethod(T fallbackInstance, Method method) {
        Method fallbackMethod;
        try {
            fallbackMethod = fallbackInstance.getClass()
                .getMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException | SecurityException e) {
            throw new IllegalArgumentException("Cannot use the fallback ["
                + fallbackInstance.getClass() + "] for ["
                + method.getDeclaringClass() + "]", e);
        }
        return fallbackMethod;
    }
}
