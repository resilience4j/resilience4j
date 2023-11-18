/*
 * Copyright 2020 Kyuhyen Hwang
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
package io.github.resilience4j.spelresolver;

import java.lang.reflect.Method;

/**
 * SpEL #root class
 */
public class SpelRootObject {
    private final String className;
    private final String methodName;
    private final Object[] args;

    public SpelRootObject(Method method, Object[] args) {
        this.className = method.getDeclaringClass().getName();
        this.methodName = method.getName();
        this.args = args;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public Object[] getArgs() {
        return args;
    }
}
