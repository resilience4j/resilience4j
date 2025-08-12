package io.github.resilience4j.bulkhead.annotation;

import java.lang.annotation.*;

/**
 * This annotation can be applied to a class or a specific method. Applying it on a class is
 * equivalent to applying it on all its public methods. If using Spring,
 * {@code name} and {@code fallbackMethod} can be resolved using Spring Expression Language (SpEL).
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD, ElementType.TYPE})
@Documented
public @interface Bulkhead {

    /**
     * Name of the bulkhead.
     * It can be SpEL expression. If you want to use the first parameter of the method as name, you can
     * express it as {@code #root.args[0]}, {@code #p0} or {@code #a0}. The method name can be accessed via
     * {@code #root.methodName}.  To invoke a method on a Spring bean, pass {@code @yourBean.yourMethod(#a0)}.
     *
     * @return the name of the bulkhead
     */
    String name();

    /**
     * Configuration key to use if name is given as a SpEL expression share the same configuration
     * @return the configuration key
     */
    String configuration() default "";

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
