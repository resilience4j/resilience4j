package io.github.resilience4j.bulkhead.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD, ElementType.TYPE})
@Documented
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
	 * SEMAPHORE will invoke semaphore based bulkhead implementation
	 * THREADPOOL will invoke Thread pool based bulkhead implementation
	 */
	enum Type {
		SEMAPHORE, THREADPOOL
	}
}
