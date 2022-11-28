/* Copyright 2019 Mahmoud Romeh
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
package io.github.resilience4j.utils;

import org.springframework.context.annotation.ConditionContext;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * common Aspect util methods
 */
public class AspectUtil {

    private AspectUtil() {
    }

    /**
     * @param context           the spring condition context
     * @param classToCheck      the class to check in spring class loader
     * @param exceptionConsumer the custom exception consumer
     * @return true or false if the class is found or not
     */
    static boolean checkClassIfFound(ConditionContext context, String classToCheck,
        Consumer<Exception> exceptionConsumer) {
        try {
            final Class<?> aClass = requireNonNull(context.getClassLoader(),
                "context must not be null").loadClass(classToCheck);
            return aClass != null;
        } catch (ClassNotFoundException e) {
            exceptionConsumer.accept(e);
            return false;
        }
    }

    @SafeVarargs
    public static <T> Set<T> newHashSet(T... objs) {
        Set<T> set = new HashSet<>();
        Collections.addAll(set, objs);
        return Collections.unmodifiableSet(set);
    }
}
