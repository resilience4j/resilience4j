/*
 * Copyright 2023 Mariusz Kopylec
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

package io.github.resilience4j.micrometer.annotation;

import java.lang.annotation.*;

/**
 * This annotation can be applied to a class or a specific method. Applying it on a class is
 * equivalent to applying it on all its public methods. The annotation enables timer for all
 * methods where it is applied. If using Spring,
 * {@code name} and {@code fallbackMethod} can be resolved using Spring Expression Language (SpEL).
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD, ElementType.TYPE})
@Documented
public @interface Timer {

    /**
     * Name of the timer.
     * It can be SpEL expression. If you want to use the first parameter of the method as name, you can
     * express it as {@code #root.args[0]}, {@code #p0} or {@code #a0}. The method name can be accessed via
     * {@code #root.methodName}. To invoke a method on a Spring bean, pass {@code @yourBean.yourMethod(#a0)}.
     *
     * @return the name of the timer.
     */
    String name();

    /**
     * fallbackMethod method name.
     *
     * @return fallbackMethod method name.
     */
    String fallbackMethod() default "";
}
