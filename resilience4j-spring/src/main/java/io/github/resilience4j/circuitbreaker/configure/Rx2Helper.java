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

import io.github.resilience4j.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.circuitbreaker.utils.CircuitBreakerUtils;
import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.SingleSource;

/**
 * Rx helper class for the RX circuit breaker logic support for the spring AOP
 */

final class Rx2Helper {

	private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerAspect.class);
	private static final Set<Class> rxSupportedTypes = newHashSet(ObservableSource.class, SingleSource.class, CompletableSource.class, MaybeSource.class, Flowable.class);

	private Rx2Helper() {
	}

	/**
	 * @param returnType the AOP method return type class
	 * @return boolean if the method has Rx java 2 rerun type
	 */
	@SuppressWarnings("unchecked")
	public static boolean isRxJava2ReturnType(Class returnType) {
		return rxSupportedTypes.stream().anyMatch(classType -> classType.isAssignableFrom(returnType));
	}


	/**
	 * @param proceedingJoinPoint Spring AOP proceedingJoinPoint
	 * @param circuitBreaker      the configured circuitBreaker
	 * @param methodName          the method name
	 * @return the result object
	 * @throws Throwable exception in case of faulty flow
	 */
	@SuppressWarnings("unchecked")
	public static Object defaultRx2Retry(ProceedingJoinPoint proceedingJoinPoint, io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker, String methodName) throws Throwable {
		CircuitBreakerUtils.isCallPermitted(circuitBreaker);
		CircuitBreakerOperator circuitBreakerOperator = CircuitBreakerOperator.of(circuitBreaker);
		long start = System.nanoTime();
		try {
			Object returnValue = proceedingJoinPoint.proceed();
			if (returnValue instanceof ObservableSource) {
				Observable observable = (Observable) returnValue;
				return observable.lift(circuitBreakerOperator);
			} else if (returnValue instanceof SingleSource) {
				Single single = (Single) returnValue;
				return single.lift(circuitBreakerOperator);
			} else if (returnValue instanceof CompletableSource) {
				Completable completable = (Completable) returnValue;
				return completable.lift(circuitBreakerOperator);
			} else if (returnValue instanceof MaybeSource) {
				Maybe maybe = (Maybe) returnValue;
				return maybe.lift(circuitBreakerOperator);
			} else if (returnValue instanceof Flowable) {
				Flowable flowable = (Flowable) returnValue;
				return flowable.lift(circuitBreakerOperator);
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

	@SafeVarargs
	private static <T> Set<T> newHashSet(T... objs) {
		Set<T> set = new HashSet<>();
		Collections.addAll(set, objs);
		return Collections.unmodifiableSet(set);
	}
}
