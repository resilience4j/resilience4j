/*
 * Copyright 2019 Mahmoud Romeh
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
package io.github.resilience4j.micronaut.annotation;

import io.micronaut.aop.Around;
import io.micronaut.context.annotation.Executable;

import java.lang.annotation.*;

/**
 * This annotation can be applied to a class or a specific method. Applying it on a class is
 * equivalent to applying it on all its public methods. The annotation enables backend retry for all
 * methods where it is applied. Backend retry is performed via a retry.
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD, ElementType.TYPE})
@Around
@Documented
@Executable
public @interface Retry {

    /**
     * Name of the sync retry.
     *
     * @return the name of the sync retry.
     */
    String name();

    /**
     * fallbackMethod method name.
     *
     * @return fallbackMethod method name.
     */
    String fallbackMethod() default "";
}
