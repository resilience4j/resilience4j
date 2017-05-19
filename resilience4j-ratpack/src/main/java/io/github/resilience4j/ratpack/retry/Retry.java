/*
 * Copyright 2017 Dan Maas
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
package io.github.resilience4j.ratpack.retry;

import io.github.resilience4j.ratpack.recovery.RecoveryFunction;
import io.github.resilience4j.ratpack.recovery.DefaultRecoveryFunction;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation for marking a method of an annotated object as circuit breaker enabled.
 * Given a method like this:
 * <pre><code>
 *     {@literal @}Retry(name = "myRetry")
 *     public String fancyName(String name) {
 *         return "Sir Captain " + name;
 *     }
 * </code></pre>
 * each time the {@code #fancyName(String)} method is invoked, the method's execution will pass through a
 * a retry according to the given retry policy.
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface Retry {
    /**
     * @return The name of the circuit breaker. It will be looked up the circuit breaker registry.
     */
    String name() default "";

    /**
     * The Function class that returns a fallback value. The default is a noop.
     *
     * @return the function
     */
    Class<? extends RecoveryFunction> recovery() default DefaultRecoveryFunction.class;

}
