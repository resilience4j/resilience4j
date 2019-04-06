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

import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Arrays;

public interface RecoveryApplier {
    boolean supports(Class target);

    CheckedFunction1<CheckedFunction0<Object>, Object> get(String recoveryMethodName, Object[] args, Object target);

    default Object invoke(String recoveryMethodName, Object[] args, Throwable throwable, Object target) throws Throwable {
        if (noRecoveryMethod(recoveryMethodName)) {
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

    default boolean noRecoveryMethod(String recoveryMethodName) {
        return recoveryMethodName == null || recoveryMethodName.isEmpty();
    }
}
