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

import static feign.Util.checkNotNull;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import feign.InvocationHandlerFactory.MethodHandler;
import feign.Target;
import io.vavr.CheckedFunction1;

/**
 * An instance of {@link InvocationHandler} that uses {@link FeignDecorator}s to enhance the
 * invocations of methods.
 */
public class DecoratorInvocationHandler implements InvocationHandler {

    private final Target<?> target;
    private final Map<Method, CheckedFunction1<Object[], Object>> decoratedDispatch;

    public DecoratorInvocationHandler(Target<?> target, Map<Method, MethodHandler> dispatch, FeignDecorator invocationDecorator) {
        checkNotNull(dispatch, "dispatch");
        decoratedDispatch = decorateMethodHandlers(dispatch, invocationDecorator);
        this.target = checkNotNull(target, "target");
    }

    private Map<Method, CheckedFunction1<Object[], Object>> decorateMethodHandlers(Map<Method, MethodHandler> dispatch,
            FeignDecorator invocationDecorator) {
        final Map<Method, CheckedFunction1<Object[], Object>> map = new HashMap<>();
        for (final Map.Entry<Method, MethodHandler> entry : dispatch.entrySet()) {
            final MethodHandler methodHandler = entry.getValue();
            map.put(entry.getKey(), invocationDecorator.decorate(methodHandler::invoke));
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
        if (obj == null) {
            return false;
        }
        if (Proxy.isProxyClass(obj.getClass())) {
            obj = Proxy.getInvocationHandler(obj);
        }
        if (obj instanceof DecoratorInvocationHandler) {
            final DecoratorInvocationHandler other = (DecoratorInvocationHandler) obj;
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
