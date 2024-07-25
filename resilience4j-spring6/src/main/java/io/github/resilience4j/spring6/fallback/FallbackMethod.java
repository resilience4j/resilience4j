/*
 * Copyright 2019 Kyuhyen Hwang, Mahmoud Romeh
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
package io.github.resilience4j.spring6.fallback;

import io.github.resilience4j.core.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Reflection utility for invoking a fallback method. Fallback method should have same return type
 * and parameter types of original method but the last additional parameter. The last additional
 * parameter should be a subclass of {@link Throwable}. When {@link FallbackMethod#fallback(Throwable)}
 * is invoked, {@link Throwable} will be passed to that last parameter. If there are multiple
 * fallback method, one of the methods that has most closest superclass parameter of thrown object
 * will be invoked.
 * <pre>
 * For example, there are two fallback methods
 * {@code
 * String fallbackMethod(String parameter, RuntimeException exception)
 * String fallbackMethod(String parameter, IllegalArgumentException exception)
 * }
 * and if try to fallback from {@link NumberFormatException}, {@code String fallbackMethod(String parameter, IllegalArgumentException exception)} will be invoked.
 * </pre>
 */
public class FallbackMethod {

    private static final Map<MethodMeta, Map<Class<?>, Method>> FALLBACK_METHODS_CACHE = new ConcurrentReferenceHashMap<>();
    private final Map<Class<?>, Method> fallbackMethods;
    private final Object[] args;
    private final Object original;
    private final Object proxy;
    private final Class<?> returnType;

    /**
     * create a fallbackMethod method.
     *
     * @param fallbackMethods          configured and found fallback methods for this invocation
     * @param originalMethodReturnType the return type of the original source method
     * @param args                     arguments those were passed to the original method. They will
     *                                 be passed to the fallbackMethod method.
     * @param original                 target object the fallbackMethod method will be invoked
     * @param proxy                    proxy object the fallbackMethod method will be invoked
     */
    private FallbackMethod(Map<Class<?>, Method> fallbackMethods, Class<?> originalMethodReturnType,
                           Object[] args, Object original, Object proxy) {

        this.fallbackMethods = fallbackMethods;
        this.args = args;
        this.original = original;
        this.proxy = proxy;
        this.returnType = originalMethodReturnType;
    }

    /**
     * @param fallbackMethodName the configured fallback method name
     * @param originalMethod     the original method which has fallback method configured
     * @param args               the original method arguments
     * @param original           the original that own the original method and fallback method
     * @param proxy              the proxy that own the original method and fallback method
     * @return FallbackMethod instance
     */
    public static FallbackMethod create(String fallbackMethodName, Method originalMethod,
                                        Object[] args, Object original, Object proxy) throws NoSuchMethodException {
        MethodMeta methodMeta = new MethodMeta(
            fallbackMethodName,
            originalMethod.getParameterTypes(),
            originalMethod.getReturnType(),
            original.getClass());

        Map<Class<?>, Method> methods = FALLBACK_METHODS_CACHE
            .computeIfAbsent(methodMeta, FallbackMethod::extractMethods);

        if (!methods.isEmpty()) {
            return new FallbackMethod(methods, originalMethod.getReturnType(), args, original, proxy);
        } else {
            throw new NoSuchMethodException(String.format("%s %s.%s(%s,%s)",
                methodMeta.returnType, methodMeta.targetClass, methodMeta.fallbackMethodName,
                StringUtils.arrayToDelimitedString(methodMeta.params, ","), Throwable.class));
        }
    }

    /**
     * @param methodMeta the method meta data
     * @return Map<Class < ?>, Method>  map of all configure fallback methods for the original
     * method that match the fallback method name
     */
    private static Map<Class<?>, Method> extractMethods(MethodMeta methodMeta) {
        Map<Class<?>, Method> methods = new HashMap<>();
        ReflectionUtils.doWithMethods(methodMeta.targetClass,
            method -> merge(method, methods),
            method -> filter(method, methodMeta)
        );
        return methods;
    }

    private static void merge(Method method, Map<Class<?>, Method> methods) {
        Class<?>[] fallbackParams = method.getParameterTypes();
        Class<?> exception = fallbackParams[fallbackParams.length - 1];
        Method similar = methods.get(exception);
        if (similar == null || Arrays
            .equals(similar.getParameterTypes(), method.getParameterTypes())) {
            methods.put(exception, method);
        } else {
            throw new IllegalStateException(
                "You have more that one fallback method that cover the same exception type "
                    + exception.getName());
        }
    }

    private static boolean filter(Method method, MethodMeta methodMeta) {
        if (!method.getName().equals(methodMeta.fallbackMethodName)) {
            return false;
        }
        if (!methodMeta.returnType.isAssignableFrom(method.getReturnType())) {
            return false;
        }
        if (method.getParameterCount() == 1) {
            return Throwable.class.isAssignableFrom(method.getParameterTypes()[0]);
        }
        if (method.getParameterCount() != methodMeta.params.length + 1) {
            return false;
        }
        Class[] targetParams = method.getParameterTypes();
        for (int i = 0; i < methodMeta.params.length; i++) {
            if (methodMeta.params[i] != targetParams[i]) {
                return false;
            }
        }
        return Throwable.class.isAssignableFrom(targetParams[methodMeta.params.length]);
    }

    /**
     * try to fallback from {@link Throwable}
     *
     * @param thrown {@link Throwable} that should be fallback
     * @return fallback value
     * @throws Throwable if throwable is unrecoverable, throwable will be thrown
     */
    @Nullable
    public Object fallback(Throwable thrown) throws Throwable {
        if (fallbackMethods.size() == 1) {
            Map.Entry<Class<?>, Method> entry = fallbackMethods.entrySet().iterator().next();
            if (entry.getKey().isAssignableFrom(thrown.getClass())) {
                return invoke(entry.getValue(), thrown);
            } else {
                throw thrown;
            }
        }

        Method fallback = null;
        Class<?> thrownClass = thrown.getClass();
        while (fallback == null && thrownClass != Object.class) {
            fallback = fallbackMethods.get(thrownClass);
            thrownClass = thrownClass.getSuperclass();
        }

        if (fallback != null) {
            return invoke(fallback, thrown);
        } else {
            throw thrown;
        }
    }

    /**
     * get return type of fallbackMethod method
     *
     * @return return type of fallbackMethod method
     */
    public Class<?> getReturnType() {
        return returnType;
    }

    /**
     * invoke the fallback method logic
     *
     * @param fallback  fallback method
     * @param throwable the thrown exception
     * @return the result object if any
     * @throws IllegalAccessException    exception
     * @throws InvocationTargetException exception
     */
    private Object invoke(Method fallback, Throwable throwable) throws Throwable {
        boolean accessible = fallback.isAccessible();
        try {
            if (!accessible) {
                ReflectionUtils.makeAccessible(fallback);
            }
            Object target = getTarget(fallback);
            if (args.length != 0) {
                if (fallback.getParameterTypes().length == 1 && Throwable.class
                    .isAssignableFrom(fallback.getParameterTypes()[0])) {
                    return fallback.invoke(target, throwable);
                }
                Object[] newArgs = Arrays.copyOf(args, args.length + 1);
                newArgs[args.length] = throwable;

                return fallback.invoke(target, newArgs);

            } else {
                return fallback.invoke(target, throwable);
            }
        } catch (InvocationTargetException e) {
            // We want the original fallback-method exception to propagate instead:
            throw e.getCause();
        } finally {
            if (!accessible) {
                fallback.setAccessible(false);
            }
        }
    }

    private Object getTarget(Method fallback) {
        boolean isPrivate = Modifier.isPrivate(fallback.getModifiers());
        if (isPrivate) {
            return original;
        }
        if (Proxy.isProxyClass(proxy.getClass())) {
            return original;
        }
        return proxy;
    }

    private static class MethodMeta {

        final String fallbackMethodName;
        final Class<?>[] params;
        final Class<?> returnType;
        final Class<?> targetClass;

        /**
         * @param fallbackMethodName the configured fallback method name
         * @param returnType         the original method return type
         * @param params             the original method arguments
         * @param targetClass        the target class that own the original method and fallback
         *                           method
         */
        MethodMeta(String fallbackMethodName, Class<?>[] params, Class<?> returnType,
                   Class<?> targetClass) {
            this.fallbackMethodName = fallbackMethodName;
            this.params = params;
            this.returnType = returnType;
            this.targetClass = targetClass;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MethodMeta that = (MethodMeta) o;
            return targetClass.equals(that.targetClass) &&
                fallbackMethodName.equals(that.fallbackMethodName) &&
                returnType.equals(that.returnType) &&
                Arrays.equals(params, that.params);
        }

        @Override
        public int hashCode() {
            return targetClass.getName().hashCode() ^ fallbackMethodName.hashCode();
        }
    }
}
