/*
 *
 * Copyright 2018
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

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Method;

import feign.InvocationHandlerFactory.MethodHandler;
import feign.Target;
import io.vavr.CheckedFunction1;

/**
 * Decorator that calls a fallback in the case that an exception is thrown.
 */
class FallbackDecorator<T> implements FeignDecorator {

    private final T fallback;

    public FallbackDecorator(T fallback) {
        this.fallback = requireNonNull(fallback, "Fallback cannot be null!");
    }

    /**
     * Calls the fallback if the invocationCall throws an {@link Exception}.
     *
     * @throws IllegalArgumentException if the fallback object does not have a corresponding
     *         fallback method.
     */
    @Override
    public CheckedFunction1<Object[], Object> decorate(CheckedFunction1<Object[], Object> invocationCall,
            Method method,
            MethodHandler methodHandler,
            Target<?> target) {
        final Method fallbackMethod;
        validateFallback(method);
        fallbackMethod = getFallbackMethod(method);
        return args -> {
            try {
                return invocationCall.apply(args);
            } catch (final Exception exception) {
                return fallbackMethod.invoke(fallback, args);
            }
        };
    }

    private void validateFallback(Method method) {
        if (fallback.getClass().isAssignableFrom(method.getDeclaringClass())) {
            throw new IllegalArgumentException("Cannot use the fallback ["
                    + fallback.getClass() + "] for ["
                    + method.getDeclaringClass() + "]!");
        }
    }

    private Method getFallbackMethod(Method method) {
        Method fallbackMethod;
        try {
            fallbackMethod = fallback.getClass().getMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException | SecurityException e) {
            throw new IllegalArgumentException("Cannot use the fallback ["
                    + fallback.getClass() + "] for ["
                    + method.getDeclaringClass() + "]", e);
        }
        return fallbackMethod;
    }

}
