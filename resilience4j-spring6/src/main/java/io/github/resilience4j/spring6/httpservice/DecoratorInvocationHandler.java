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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An instance of {@link InvocationHandler} that uses {@link HttpServiceDecorator}s to enhance the
 * invocations of methods.
 */
class DecoratorInvocationHandler implements InvocationHandler {

    private final HttpServiceTarget<?> target;
    private final Object delegate;
    private final Map<Method, CheckedFunction<Object[], Object>> decoratedDispatch;

    public DecoratorInvocationHandler(HttpServiceTarget<?> target,
                                      Object delegate,
                                      HttpServiceDecorator invocationDecorator) {
        this.target = Objects.requireNonNull(target, "target must not be null");
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        Objects.requireNonNull(invocationDecorator, "invocationDecorator must not be null");
        this.decoratedDispatch = decorateMethodHandlers(target, delegate, invocationDecorator);
    }

    /**
     * Applies the specified {@link HttpServiceDecorator} to all methods and returns the result as
     * a map of {@link CheckedFunction}s. Invoking a {@link CheckedFunction} will therefore invoke
     * the decorator which, in turn, may invoke the corresponding method on the delegate.
     *
     * @param target              the target HTTP Service metadata.
     * @param delegate            the Spring-created proxy to delegate calls to.
     * @param invocationDecorator the {@link HttpServiceDecorator} with which to decorate methods.
     * @return a new map where the methods are decorated with the {@link HttpServiceDecorator}.
     */
    private Map<Method, CheckedFunction<Object[], Object>> decorateMethodHandlers(
            HttpServiceTarget<?> target,
            Object delegate,
            HttpServiceDecorator invocationDecorator) {

        final Map<Method, CheckedFunction<Object[], Object>> map = new HashMap<>();

        for (Method method : target.type().getMethods()) {
            if (isObjectMethod(method) || method.isSynthetic()) {
                continue;
            }

            CheckedFunction<Object[], Object> invocation = args -> {
                try {
                    return method.invoke(delegate, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            };

            CheckedFunction<Object[], Object> decorated =
                    invocationDecorator.decorate(invocation, method, target);
            map.put(method, decorated);
        }
        return map;
    }

    private boolean isObjectMethod(Method method) {
        return method.getDeclaringClass() == Object.class;
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

        if (method.isDefault()) {
            return InvocationHandler.invokeDefault(proxy, method, args);
        }

        CheckedFunction<Object[], Object> decorated = decoratedDispatch.get(method);
        if (decorated != null) {
            return decorated.apply(args);
        }

        try {
            return method.invoke(delegate, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
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
        if (compareTo instanceof DecoratorInvocationHandler other) {
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
