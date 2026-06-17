/*
 * Copyright 2026 Bobae Kim
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
package io.github.resilience4j.spring6.httpservice;

import io.github.resilience4j.core.functions.CheckedFunction;

import java.lang.reflect.Method;

/**
 * Used to decorate methods defined by HTTP Service clients. Decorators can be stacked, allowing
 * multiple Decorators to be combined. See {@link HttpServiceDecorators}.
 */
@FunctionalInterface
public interface HttpServiceDecorator {

    /**
     * Decorates the invocation of a method defined by an HTTP Service client.
     *
     * @param invocationCall represents the call to the method. This should be decorated by the
     *                       implementing class.
     * @param method         the method of the HTTP Service that is invoked.
     * @param target         metadata about the target HTTP Service.
     * @return the decorated invocationCall
     */
    CheckedFunction<Object[], Object> decorate(CheckedFunction<Object[], Object> invocationCall,
                                               Method method,
                                               HttpServiceTarget<?> target);

}
