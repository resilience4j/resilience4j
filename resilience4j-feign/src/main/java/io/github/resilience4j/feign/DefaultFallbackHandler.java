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
 * A {@link FallbackHandler} simply wrapping a fallback instance of type {@param T}.
 *
 * @param <T> the type of the fallback
 */
class DefaultFallbackHandler<T> implements FallbackHandler<T> {

    private final T fallback;

    DefaultFallbackHandler(T fallback) {
        this.fallback = fallback;
    }

    @Override
    public CheckedFunction<Object[], Object> decorate(
        CheckedFunction<Object[], Object> invocationCall,
        Method method,
        Predicate<Exception> filter) {
        validateFallback(fallback, method);
        Method fallbackMethod = getFallbackMethod(fallback, method);
        fallbackMethod.setAccessible(true);
        return args -> {
            try {
                return invocationCall.apply(args);
            } catch (Exception exception) {
                if (filter.test(exception)) {
                    return fallbackMethod.invoke(fallback, args);
                }
                throw exception;
            }
        };
    }
}
