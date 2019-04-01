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
package io.github.resilience4j.circuitbreaker.configure;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.utils.CircuitBreakerUtils;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * the Reactor breaker logic support for the spring AOP
 * Conditional on Reactor class existence on spring class loader
 */

@Component
@Conditional(value = {InjectReactorAspect.class})
class ReactorCircuitBreakerAspect implements CircuitBreakerAspectExt {

	private static final Logger logger = LoggerFactory.getLogger(ReactorCircuitBreakerAspect.class);

	public ReactorCircuitBreakerAspect() {
	}

	@SafeVarargs
	private static <T> Set<T> newHashSet(T... objs) {
		Set<T> set = new HashSet<>();
		Collections.addAll(set, objs);
		return Collections.unmodifiableSet(set);
	}

	/**
	 * @param returnType the AOP method return type class
	 * @return boolean if the method has Reactor return type
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean matchReturnType(Class returnType) {
		return (Flux.class.isAssignableFrom(returnType)) || (Mono.class.isAssignableFrom(returnType));
	}

	/**
	 * handle the Spring web flux (Flux /Mono) return types AOP based into reactor circuit-breaker
	 * See {@link io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator} for details.
	 *
	 * @param proceedingJoinPoint Spring AOP proceedingJoinPoint
	 * @param circuitBreaker      the configured circuitBreaker
	 * @param methodName          the method name
	 * @return the result object
	 * @throws Throwable exception in case of faulty flow
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object handle(ProceedingJoinPoint proceedingJoinPoint, CircuitBreaker circuitBreaker, String methodName) throws Throwable {
		CircuitBreakerUtils.isCallPermitted(circuitBreaker);
		long start = System.nanoTime();
		try {
			Object returnValue = proceedingJoinPoint.proceed();
			if (Flux.class.isAssignableFrom(returnValue.getClass())) {
				Flux fluxReturnValue = (Flux) returnValue;
				return fluxReturnValue.transform(io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator.of(circuitBreaker));
			} else if (Mono.class.isAssignableFrom(returnValue.getClass())) {
				Mono monoReturnValue = (Mono) returnValue;
				return monoReturnValue.transform(CircuitBreakerOperator.of(circuitBreaker));
			} else {
				throw new IllegalArgumentException("Not Supported type for the circuit breaker in web flux :" + returnValue.getClass().getName());

			}
		} catch (Throwable throwable) {
			long durationInNanos = System.nanoTime() - start;
			circuitBreaker.onError(durationInNanos, throwable);
			if (logger.isDebugEnabled()) {
				logger.debug("Invocation of method '" + methodName + "' failed!", throwable);
			}
			throw throwable;
		}
	}
}
