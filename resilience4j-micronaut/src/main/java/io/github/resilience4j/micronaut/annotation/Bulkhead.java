package io.github.resilience4j.micronaut.annotation;

import io.micronaut.aop.Around;
import io.micronaut.context.annotation.Executable;

import java.lang.annotation.*;

/**
 * This annotation can be applied to a class or a specific method. Applying it on a class is
 * equivalent to applying it on all its public methods.
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD, ElementType.TYPE})
@Around
@Documented
@Executable
public @interface Bulkhead {

    /**
     * Name of the bulkhead.
     *
     * @return the name of the bulkhead
     */
    String name();

    /**
     * fallbackMethod method name.
     *
     * @return fallbackMethod method name.
     */
    String fallbackMethod() default "";

    /**
     * @return the bulkhead implementation type (SEMAPHORE or THREADPOOL)
     */
    Type type() default Type.SEMAPHORE;

    /**
     * bulkhead implementation types
     * <p>
     * SEMAPHORE will invoke semaphore based bulkhead implementation THREADPOOL will invoke Thread
     * pool based bulkhead implementation
     */
    enum Type {
        SEMAPHORE, THREADPOOL
    }
}
