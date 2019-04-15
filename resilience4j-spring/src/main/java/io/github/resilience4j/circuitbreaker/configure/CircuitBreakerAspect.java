/*
 * Copyright 2017 Robert Winkler
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
package io.github.resilience4j.circuitbreaker.configure;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.utils.AnnotationExtractor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

/**
 * This Spring AOP aspect intercepts all methods which are annotated with a {@link CircuitBreaker} annotation.
 * The aspect protects an annotated method with a CircuitBreaker. The CircuitBreakerRegistry is used to retrieve an instance of a CircuitBreaker for
 * a specific name.
 */
@Aspect
public class CircuitBreakerAspect implements Ordered {

	private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerAspect.class);

	private final CircuitBreakerConfigurationProperties circuitBreakerProperties;
	private final CircuitBreakerRegistry circuitBreakerRegistry;
	private final @Nullable List<CircuitBreakerAspectExt> circuitBreakerAspectExtList;

	public CircuitBreakerAspect(CircuitBreakerConfigurationProperties backendMonitorPropertiesRegistry, CircuitBreakerRegistry circuitBreakerRegistry, @Autowired(required = false) List<CircuitBreakerAspectExt> circuitBreakerAspectExtList) {
		this.circuitBreakerProperties = backendMonitorPropertiesRegistry;
		this.circuitBreakerRegistry = circuitBreakerRegistry;
		this.circuitBreakerAspectExtList = circuitBreakerAspectExtList;
	}

	@Pointcut(value = "@within(circuitBreaker) || @annotation(circuitBreaker)", argNames = "circuitBreaker")
	public void matchAnnotatedClassOrMethod(CircuitBreaker circuitBreaker) {
	}

	@Around(value = "matchAnnotatedClassOrMethod(backendMonitored)", argNames = "proceedingJoinPoint, backendMonitored")
    public Object circuitBreakerAroundAdvice(ProceedingJoinPoint proceedingJoinPoint, @Nullable CircuitBreaker backendMonitored) throws Throwable {
		Method method = ((MethodSignature) proceedingJoinPoint.getSignature()).getMethod();
		String methodName = method.getDeclaringClass().getName() + "#" + method.getName();
		if (backendMonitored == null) {
			backendMonitored = getBackendMonitoredAnnotation(proceedingJoinPoint);
		}
        if(backendMonitored == null) { //because annotations wasn't found
            return proceedingJoinPoint.proceed();
        }
        String backend = backendMonitored.name();
		io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(methodName, backend);
		Class<?> returnType = method.getReturnType();
		if (circuitBreakerAspectExtList != null && !circuitBreakerAspectExtList.isEmpty()) {
			for (CircuitBreakerAspectExt circuitBreakerAspectExt : circuitBreakerAspectExtList) {
				if (circuitBreakerAspectExt.canHandleReturnType(returnType)) {
					return circuitBreakerAspectExt.handle(proceedingJoinPoint, circuitBreaker, methodName);
				}
			}
		}
		if (CompletionStage.class.isAssignableFrom(returnType)) {
			return handleJoinPointCompletableFuture(proceedingJoinPoint, circuitBreaker);
		}
		return defaultHandling(proceedingJoinPoint, circuitBreaker);
	}

	private io.github.resilience4j.circuitbreaker.CircuitBreaker getOrCreateCircuitBreaker(String methodName, String backend) {
		io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(backend,
				() -> circuitBreakerProperties.createCircuitBreakerConfig(backend));

		if (logger.isDebugEnabled()) {
			logger.debug("Created or retrieved circuit breaker '{}' with failure rate '{}' and wait interval'{}' for method: '{}'",
					backend, circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold(),
					circuitBreaker.getCircuitBreakerConfig().getWaitDurationInOpenState(), methodName);
		}

		return circuitBreaker;
	}

    @Nullable
	private CircuitBreaker getBackendMonitoredAnnotation(ProceedingJoinPoint proceedingJoinPoint) {
		if (logger.isDebugEnabled()) {
			logger.debug("circuitBreaker parameter is null");
		}
		return AnnotationExtractor.extract(proceedingJoinPoint.getTarget().getClass(), CircuitBreaker.class);
	}

	/**
	 * handle the CompletionStage return types AOP based into configured circuit-breaker
	 */
	private Object handleJoinPointCompletableFuture(ProceedingJoinPoint proceedingJoinPoint, io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker) {
		return circuitBreaker.executeCompletionStage(() -> {
			try {
				return (CompletionStage<?>) proceedingJoinPoint.proceed();
			} catch (Throwable throwable) {
				throw new CompletionException(throwable);
			}
		});
	}

	/**
	 * the default Java types handling for the circuit breaker AOP
	 */
	private Object defaultHandling(ProceedingJoinPoint proceedingJoinPoint, io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker) throws Throwable {
		return circuitBreaker.executeCheckedSupplier(proceedingJoinPoint::proceed);
	}

	@Override
	public int getOrder() {
		return circuitBreakerProperties.getCircuitBreakerAspectOrder();
	}
}
