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

import feign.InvocationHandlerFactory.MethodHandler;
import feign.Target;
import io.github.resilience4j.core.functions.CheckedFunction;

import java.lang.reflect.Method;

/**
 * Used to decorate methods defined by feign interfaces. Decorators can be stacked, allowing
 * multiple Decorators to be combined. See {@link FeignDecorators}.
 */
@FunctionalInterface
public interface FeignDecorator {

    /**
     * Decorates the invocation of a method defined by a feign interface.
     *
     * @param invocationCall represents the call to the method. This should be decorated by the
     *                       implementing class.
     * @param method         the method of the feign interface that is invoked.
     * @param methodHandler  the feign methodHandler that executes the http request.
     * @return the decorated invocationCall
     */
    CheckedFunction<Object[], Object> decorate(CheckedFunction<Object[], Object> invocationCall,
                                               Method method, MethodHandler methodHandler,
                                               Target<?> target);

}
