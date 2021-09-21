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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A {@link FallbackHandler} wrapping a fallback of type {@param T} whose instance will consume the
 * exception thrown on error. A new instance of the fallback will be created for every thrown
 * exceptions.
 *
 * @param <T> the type of the fallback
 */
class FallbackFactory<T> implements FallbackHandler<T> {

    private final Function<Exception, T> fallbackSupplier;

    FallbackFactory(Function<Exception, T> fallbackSupplier) {
        this.fallbackSupplier = fallbackSupplier;
    }

    @Override
    public CheckedFunction<Object[], Object> decorate(
        CheckedFunction<Object[], Object> invocationCall,
        Method method,
        Predicate<Exception> filter) {
        return args -> {
            try {
                return invocationCall.apply(args);
            } catch (Exception exception) {
                if (filter.test(exception)) {
                    T fallbackInstance = fallbackSupplier.apply(exception);
                    validateFallback(fallbackInstance, method);
                    Method fallbackMethod = getFallbackMethod(fallbackInstance, method);
                    try {
                        return fallbackMethod.invoke(fallbackInstance, args);
                    } catch (InvocationTargetException e) {
                        // Rethrow the exception thrown in the fallback wrapped by InvocationTargetException
                        throw e.getCause();
                    }
                }
                throw exception;
            }
        };
    }
}
