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
    private final String recoveryMethodName;
    private final Object[] args;
    private final Object target;
    private final Class<?> returnType;

    public RecoveryMethod(String recoveryMethodName, Object[] args, Class<?> returnType,  Object target) {
        this.recoveryMethodName = recoveryMethodName;
        this.args = args;
        this.returnType = returnType;
        this.target = target;
    }

    public Object recover(Throwable throwable) throws Throwable {
        if (undefined()) {
            throw throwable;
        }

        Class[] params = new Class[args.length + 1];

        for (int i = 0; i < args.length; i++) {
            params[i] = args[i].getClass();
        }

        params[args.length] = Throwable.class;

        Method recovery = ReflectionUtils.findMethod(target.getClass(), recoveryMethodName, params);

        if (args.length != 0) {
            Object[] newArgs = Arrays.copyOf(args, args.length + 1);
            newArgs[args.length] = throwable;

            return recovery.invoke(target, newArgs);
        } else {
            return recovery.invoke(target, throwable);
        }
    }

    public boolean undefined() {
        return recoveryMethodName == null || recoveryMethodName.isEmpty();
    }

    public Class<?> getReturnType() {
        return returnType;
    }
}
