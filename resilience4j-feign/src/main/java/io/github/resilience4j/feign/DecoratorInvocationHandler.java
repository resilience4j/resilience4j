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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static feign.Util.checkNotNull;

/**
 * An instance of {@link InvocationHandler} that uses {@link FeignDecorator}s to enhance the
 * invocations of methods.
 */
class DecoratorInvocationHandler implements InvocationHandler {

    private final Target<?> target;
    private final Map<Method, CheckedFunction<Object[], Object>> decoratedDispatch;

    public DecoratorInvocationHandler(Target<?> target,
        Map<Method, MethodHandler> dispatch,
        FeignDecorator invocationDecorator) {
        this.target = checkNotNull(target, "target");
        checkNotNull(dispatch, "dispatch");
        this.decoratedDispatch = decorateMethodHandlers(dispatch, invocationDecorator, target);
    }

    /**
     * Applies the specified {@link FeignDecorator} to all specified {@link MethodHandler}s and
     * returns the result as a map of {@link CheckedFunction}s. Invoking a {@link CheckedFunction}
     * will therefore invoke the decorator which, in turn, may invoke the corresponding {@link
     * MethodHandler}.
     *
     * @param dispatch            a map of the methods from the feign interface to the {@link
     *                            MethodHandler}s.
     * @param invocationDecorator the {@link FeignDecorator} with which to decorate the {@link
     *                            MethodHandler}s.
     * @param target              the target feign interface.
     * @return a new map where the {@link MethodHandler}s are decorated with the {@link
     * FeignDecorator}.
     */
    private Map<Method, CheckedFunction<Object[], Object>> decorateMethodHandlers(
        Map<Method, MethodHandler> dispatch,
        FeignDecorator invocationDecorator, Target<?> target) {
        final Map<Method, CheckedFunction<Object[], Object>> map = new HashMap<>();
        for (final Map.Entry<Method, MethodHandler> entry : dispatch.entrySet()) {
            final Method method = entry.getKey();
            final MethodHandler methodHandler = entry.getValue();
            if (methodHandler != null) {
                CheckedFunction<Object[], Object> decorated = invocationDecorator
                    .decorate(methodHandler::invoke, method, methodHandler, target);
                map.put(method, decorated);
            }
        }
        return map;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args)
        throws Throwable {
        switch (method.getName()) {
            case "equals":
                return equals(args.length > 0 ? args[0] : null);

            case "hashCode":
                return hashCode();

            case "toString":
                return toString();

            default:
                break;
        }

        return decoratedDispatch.get(method).apply(args);
    }

    @Override
    public boolean equals(Object obj) {
        Object compareTo = obj;
        if (compareTo == null) {
            return false;
        }
        if (Proxy.isProxyClass(compareTo.getClass())) {
            compareTo = Proxy.getInvocationHandler(compareTo);
        }
        if (compareTo instanceof DecoratorInvocationHandler) {
            final DecoratorInvocationHandler other = (DecoratorInvocationHandler) compareTo;
            return target.equals(other.target);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return target.hashCode();
    }

    @Override
    public String toString() {
        return target.toString();
    }
}
