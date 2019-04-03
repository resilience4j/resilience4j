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
package io.github.resilience4j.bulkhead.configure;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.operator.BulkheadOperator;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerAspect;
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
 * the Rx bulkhead logic support for the spring AOP
 * conditional on the presence of Rx classes on the spring class loader
 */
public class RxJava2BulkHeadAspectExt implements BulkHeadAspectExt {

	private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerAspect.class);
	private final Set<Class> rxSupportedTypes = newHashSet(ObservableSource.class, SingleSource.class, CompletableSource.class, MaybeSource.class, Flowable.class);

	@SafeVarargs
	private static <T> Set<T> newHashSet(T... objs) {
		Set<T> set = new HashSet<>();
		Collections.addAll(set, objs);
		return Collections.unmodifiableSet(set);
	}

	/**
	 * @param returnType the AOP method return type class
	 * @return boolean if the method has Rx java 2 rerun type
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean canHandleReturnType(Class returnType) {
		return rxSupportedTypes.stream().anyMatch(classType -> classType.isAssignableFrom(returnType));
	}

	/**
	 * @param proceedingJoinPoint Spring AOP proceedingJoinPoint
	 * @param bulkhead            the configured bulkhead
	 * @param methodName          the method name
	 * @return the result object
	 * @throws Throwable exception in case of faulty flow
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object handle(ProceedingJoinPoint proceedingJoinPoint, Bulkhead bulkhead, String methodName) throws Throwable {
		BulkheadOperator bulkheadOperator = BulkheadOperator.of(bulkhead);
		Object returnValue = proceedingJoinPoint.proceed();
		return executeRxJava2Aspect(bulkheadOperator, returnValue);
	}

	private Object executeRxJava2Aspect(BulkheadOperator bulkheadOperator, Object returnValue) {
		if (returnValue instanceof ObservableSource) {
			Observable<?> observable = (Observable<?>) returnValue;
			return observable.lift(bulkheadOperator);
		} else if (returnValue instanceof SingleSource) {
			Single<?> single = (Single) returnValue;
			return single.lift(bulkheadOperator);
		} else if (returnValue instanceof CompletableSource) {
			Completable completable = (Completable) returnValue;
			return completable.lift(bulkheadOperator);
		} else if (returnValue instanceof MaybeSource) {
			Maybe<?> maybe = (Maybe) returnValue;
			return maybe.lift(bulkheadOperator);
		} else if (returnValue instanceof Flowable) {
			Flowable<?> flowable = (Flowable) returnValue;
			return flowable.lift(bulkheadOperator);
		} else {
			logger.error("Unsupported type for BulkHead RxJava2 {}", returnValue.getClass().getTypeName());
			throw new IllegalArgumentException("Not Supported type for the BulkHead in RxJava2 :" + returnValue.getClass().getName());
		}
	}
}
