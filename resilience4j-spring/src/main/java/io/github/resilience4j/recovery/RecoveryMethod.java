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

public class RecoveryMethod {
    private final Map<Class<?>, Method> recoveryMethods;
    private final Object[] args;
    private final Object target;
    private final Class<?> returnType;

    public RecoveryMethod(String recoveryMethodName, Method originalMethod, Object[] args, Object target) throws NoSuchMethodException {
        Class<?>[] params = originalMethod.getParameterTypes();
        Class<?> originalReturnType = originalMethod.getReturnType();

        Map<Class<?>, Method> methods = new HashMap<>();

        ReflectionUtils.doWithMethods(target.getClass(), method -> {
            if (!method.isAccessible()) {
                ReflectionUtils.makeAccessible(method);
            }
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

        if (methods.isEmpty()) {
            throw new NoSuchMethodException(String.format("%s %s.%s(%s,%s)", originalReturnType, target.getClass(), recoveryMethodName, StringUtils.arrayToDelimitedString(params, ","), Throwable.class));
        }

        this.recoveryMethods = methods;
        this.args = args;
        this.target = target;
        this.returnType = originalReturnType;
    }

    public Object recover(Throwable thrown) throws Throwable {
        Method recovery = null;

        for (Class<?> thrownClass = thrown.getClass(); recovery == null && thrownClass != Object.class; thrownClass = thrownClass.getSuperclass()) {
            recovery = recoveryMethods.get(thrownClass);
        }

        if (recovery == null) {
            throw thrown;
        }

        return invoke(recovery, thrown);
    }

    private Object invoke(Method recovery, Throwable throwable) throws IllegalAccessException, InvocationTargetException {
        if (args.length != 0) {
            Object[] newArgs = Arrays.copyOf(args, args.length + 1);
            newArgs[args.length] = throwable;

            return recovery.invoke(target, newArgs);
        } else {
            return recovery.invoke(target, throwable);
        }
    }

    public Class<?> getReturnType() {
        return returnType;
    }
}
