/*
 * Copyright 2017 Jan Sykora
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
package io.github.resilience4j.ratpack.bulkhead;

import io.github.resilience4j.ratpack.recovery.DefaultRecoveryFunction;
import io.github.resilience4j.ratpack.recovery.RecoveryFunction;
import ratpack.exec.Promise;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation for marking a method of an annotated object as bulkhead enabled.
 * Given a method like this:
 * <pre><code>
 *     {@literal @}Bulkhead(name = "myService")
 *     public String fancyName(String name) {
 *         return "Sir Captain " + name;
 *     }
 * </code></pre>
 * each time the {@code #fancyName(String)} method is invoked, the method's execution will pass through a
 * a bulkhead (concurrent limiting) according to the given bulkhead policy.
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface Bulkhead {
    /**
     * @return The name of the bulkhead. It will be looked up the bulkhead registry.
     */
    String name() default "";

    /**
     * The Function class that returns a fallback value. The default is a noop.
     *
     * @return the function
     */
    Class<? extends RecoveryFunction> recovery() default DefaultRecoveryFunction.class;

    /**
     * The method name that returns a recovery value. The default is a noop.
     *
     * The method parameter signature must match either:
     *
     * 1) The method parameter signature on the annotated method or
     * 2) The method parameter signature with a matching exception type as the last parameter on the annotated method
     *
     * The return value can be a {@link Promise}, {@link java.util.concurrent.CompletionStage},
     * {@link reactor.core.publisher.Flux}, {@link reactor.core.publisher.Mono}, or an object value.
     * Other reactive types are not supported.
     *
     * If the return value is one of the reactive types listed above, it must match the return value type of the
     * annotated method.
     *
     * This argument takes precedence over the {@link Bulkhead#recovery()} argument.
     *
     * @return the method name
     */
    String fallbackMethod() default "";
}
