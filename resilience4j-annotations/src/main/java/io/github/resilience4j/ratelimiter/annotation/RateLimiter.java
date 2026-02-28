/*
 * Copyright 2017 Bohdan Storozhuk
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
package io.github.resilience4j.ratelimiter.annotation;

import java.lang.annotation.*;

/**
 * This annotation can be applied to a class or a specific method. Applying it on a class is
 * equivalent to applying it on all its public methods. The annotation enables throttling for all
 * methods where it is applied. Throttling monitoring is performed via a rate limiter. See {@link
 * io.github.resilience4j.ratelimiter.RateLimiter} for details. If using Spring,
 * {@code name} and {@code fallbackMethod} can be resolved using Spring Expression Language (SpEL).
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD, ElementType.TYPE})
@Documented
public @interface RateLimiter {

    /**
     * Name of the rate limiter
     * It can be SpEL expression. If you want to use the first parameter of the method as name, you can
     * express it as {@code #root.args[0]}, {@code #p0} or {@code #a0}. The method name can be accessed via
     * {@code #root.methodName}.  To invoke a method on a Spring bean, pass {@code @yourBean.yourMethod(#a0)}.
     *
     * @return the name of the limiter
     */
    String name();

    /**
     * Configuration key to use if name is given as a SpEL expression share the same configuration
     * @return the configuration key
     */
    String configuration() default "";

    /**
     * fallbackMethod method name.
     * It can be a SpEL expression. To reference a fallback method on an external
     * Spring bean, use {@code "beanName::methodName"} syntax.
     *
     * @return fallbackMethod method name.
     */
    String fallbackMethod() default "";

    /**
     * Number of permits that this call requires.
     *
     * @return the number of permits that this call requires.
     */
    int permits() default 1;
}
