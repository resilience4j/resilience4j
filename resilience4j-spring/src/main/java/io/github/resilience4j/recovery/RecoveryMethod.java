/*
 * Copyright 2019 Kyuhyen Hwang
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
package io.github.resilience4j.recovery;

import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Reflection utility for invoking recovery method. Recovery method should have same return type and parameter types of original method but the last additional parameter.
 * The last additional parameter should be a subclass of {@link Throwable}. When {@link RecoveryMethod#recover(Throwable)} is invoked, {@link Throwable} will be passed to that last parameter.
 * If there are multiple recovery method, one of the methods that has most closest superclass parameter of thrown object will be invoked.
 * <pre>
 * For example, there are two recovery methods
 * {@code
 * String recovery(String parameter, RuntimeException exception)
 * String recovery(String parameter, IllegalArgumentException exception)
 * }
 * and if try to recover from {@link NumberFormatException}, {@code String recovery(String parameter, IllegalArgumentException exception)} will be invoked.
 * </pre>
 */
public class RecoveryMethod {
    private final Map<Class<?>, Method> recoveryMethods;
    private final Object[] args;
    private final Object target;
    private final Class<?> returnType;

    /**
     * create a recovery method.
     *
     * @param recoveryMethodName recovery method name
     * @param originalMethod     will be used for checking return type and parameter types of the recovery method
     * @param args               arguments those were passed to the original method. They will be passed to the recovery method.
     * @param target             target object the recovery method will be invoked
     * @throws NoSuchMethodException will be thrown, if recovery method is not found
     */
    public RecoveryMethod(String recoveryMethodName, Method originalMethod, Object[] args, Object target) throws NoSuchMethodException {
        Class<?>[] params = originalMethod.getParameterTypes();
        Class<?> originalReturnType = originalMethod.getReturnType();

        Map<Class<?>, Method> methods = extractMethods(recoveryMethodName, params, originalReturnType, target.getClass());

        if (methods.isEmpty()) {
            throw new NoSuchMethodException(String.format("%s %s.%s(%s,%s)", originalReturnType, target.getClass(), recoveryMethodName, StringUtils.arrayToDelimitedString(params, ","), Throwable.class));
        }

        this.recoveryMethods = methods;
        this.args = args;
        this.target = target;
        this.returnType = originalReturnType;
    }

    /**
     * try to recover from {@link Throwable}
     *
     * @param thrown {@link Throwable} that should be recover
     * @return recovered value
     * @throws Throwable if throwable is unrecoverable, throwable will be thrown
     */
    public Object recover(Throwable thrown) throws Throwable {
        if (recoveryMethods.size() == 1) {
            Map.Entry<Class<?>, Method> entry = recoveryMethods.entrySet().iterator().next();
            if (entry.getKey().isAssignableFrom(thrown.getClass())) {
                return invoke(entry.getValue(), thrown);
            } else {
                throw thrown;
            }
        }

        Method recovery = null;

        for (Class<?> thrownClass = thrown.getClass(); recovery == null && thrownClass != Object.class; thrownClass = thrownClass.getSuperclass()) {
            recovery = recoveryMethods.get(thrownClass);
        }

        if (recovery == null) {
            throw thrown;
        }

        return invoke(recovery, thrown);
    }

    /**
     * get return type of recovery method
     *
     * @return return type of recovery method
     */
    public Class<?> getReturnType() {
        return returnType;
    }

    private Object invoke(Method recovery, Throwable throwable) throws IllegalAccessException, InvocationTargetException {
        boolean accessible = recovery.isAccessible();
        try {
            if (!accessible) {
                ReflectionUtils.makeAccessible(recovery);
            }

            if (args.length != 0) {
                Object[] newArgs = Arrays.copyOf(args, args.length + 1);
                newArgs[args.length] = throwable;

                return recovery.invoke(target, newArgs);

            } else {
                return recovery.invoke(target, throwable);
            }
        } finally {
            if (!accessible) {
                recovery.setAccessible(false);
            }
        }
    }

    private static Map<Class<?>, Method> extractMethods(String recoveryMethodName, Class<?>[] params, Class<?> originalReturnType, Class<?> targetClass) {
        Map<Class<?>, Method> methods = new HashMap<>();

        ReflectionUtils.doWithMethods(targetClass, method -> {
            Class<?>[] recoveryParams = method.getParameterTypes();
            methods.put(recoveryParams[recoveryParams.length - 1], method);
        }, method -> {
            if (!method.getName().equals(recoveryMethodName) || method.getParameterCount() != params.length + 1) {
                return false;
            }
            if (!originalReturnType.isAssignableFrom(method.getReturnType())) {
                return false;
            }

            Class[] targetParams = method.getParameterTypes();
            for (int i = 0; i < params.length; i++) {
                if (params[i] != targetParams[i]) {
                    return false;
                }
            }

            return Throwable.class.isAssignableFrom(targetParams[params.length]);
        });

        return methods;
    }
}
