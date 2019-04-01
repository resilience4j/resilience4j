package io.github.resilience4j.circuitbreaker.configure;

import org.aspectj.lang.ProceedingJoinPoint;

/**
 * @author romeh
 */
public interface CircuitBreakerAspectExt {

	boolean matchReturnType(Class returnType);

	Object handle(ProceedingJoinPoint proceedingJoinPoint, io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker, String methodName) throws Throwable;
}
