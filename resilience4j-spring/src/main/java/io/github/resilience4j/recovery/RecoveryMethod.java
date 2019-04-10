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

import java.lang.reflect.Method;
import java.util.Arrays;

public class RecoveryMethod {
    private final Method recovery;
    private final Object[] args;
    private final Object target;

    public RecoveryMethod(String recoveryMethodName, Method originalMethod, Object[] args, Object target) throws NoSuchMethodException, ClassCastException {
        Class[] params = originalMethod.getParameterTypes();
        Class[] recoveryParams = Arrays.copyOf(params, params.length + 1);
        recoveryParams[params.length] = Throwable.class;
        Method recovery = ReflectionUtils.findMethod(target.getClass(), recoveryMethodName, recoveryParams);

        if (recovery == null) {
            throw new NoSuchMethodException(String.format("%s.%s", target.getClass(), recoveryMethodName));
        }

        Class<?> originalReturnType = originalMethod.getReturnType();
        if (!originalReturnType.isAssignableFrom(recovery.getReturnType())) {
            throw new ClassCastException(String.format("recovery return type not matched (expected: %s, actual :%s)", originalReturnType.getName(), recovery.getReturnType().getName()));
        }

        this.recovery = recovery;
        this.args = args;
        this.target = target;
    }

    public Object recover(Throwable throwable) throws Throwable {
        if (args.length != 0) {
            Object[] newArgs = Arrays.copyOf(args, args.length + 1);
            newArgs[args.length] = throwable;

            return recovery.invoke(target, newArgs);
        } else {
            return recovery.invoke(target, throwable);
        }
    }

    public Class<?> getReturnType() {
        return recovery.getReturnType();
    }
}
