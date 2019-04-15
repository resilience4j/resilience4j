/*
 * Copyright 2017 Bohdan Storozhuk
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
package io.github.resilience4j.ratelimiter.configure;

import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
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
 * This Spring AOP aspect intercepts all methods which are annotated with a {@link RateLimiter} annotation.
 * The aspect protects an annotated method with a RateLimiter. The RateLimiterRegistry is used to retrieve an instance of a RateLimiter for
 * a specific backend.
 */

@Aspect
public class RateLimiterAspect implements Ordered {
	public static final String RATE_LIMITER_RECEIVED = "Created or retrieved rate limiter '{}' with period: '{}'; limit for period: '{}'; timeout: '{}'; method: '{}'";
	private static final Logger logger = LoggerFactory.getLogger(RateLimiterAspect.class);
	private final RateLimiterRegistry rateLimiterRegistry;
	private final RateLimiterConfigurationProperties properties;
	private final @Nullable List<RateLimiterAspectExt> rateLimiterAspectExtList;

	public RateLimiterAspect(RateLimiterRegistry rateLimiterRegistry, RateLimiterConfigurationProperties properties, @Autowired(required = false) List<RateLimiterAspectExt> rateLimiterAspectExtList) {
		this.rateLimiterRegistry = rateLimiterRegistry;
		this.properties = properties;
		this.rateLimiterAspectExtList = rateLimiterAspectExtList;
	}

	/**
	 * Method used as pointcut
	 *
	 * @param rateLimiter - matched annotation
	 */
	@Pointcut(value = "@within(rateLimiter) || @annotation(rateLimiter)", argNames = "rateLimiter")
	public void matchAnnotatedClassOrMethod(RateLimiter rateLimiter) {
		// Method used as pointcut
	}

	@Around(value = "matchAnnotatedClassOrMethod(limitedService)", argNames = "proceedingJoinPoint, limitedService")
	public Object rateLimiterAroundAdvice(ProceedingJoinPoint proceedingJoinPoint, @Nullable RateLimiter limitedService) throws Throwable {
		RateLimiter targetService = limitedService;
		Method method = ((MethodSignature) proceedingJoinPoint.getSignature()).getMethod();
		String methodName = method.getDeclaringClass().getName() + "#" + method.getName();
		if (targetService == null) {
			targetService = getRateLimiterAnnotation(proceedingJoinPoint);
		}
		String name = targetService.name();
		Class<?> returnType = method.getReturnType();
		io.github.resilience4j.ratelimiter.RateLimiter rateLimiter = getOrCreateRateLimiter(methodName, name);
		if (rateLimiterAspectExtList != null && !rateLimiterAspectExtList.isEmpty()) {
			for (RateLimiterAspectExt rateLimiterAspectExt : rateLimiterAspectExtList) {
				if (rateLimiterAspectExt.canHandleReturnType(returnType)) {
					return rateLimiterAspectExt.handle(proceedingJoinPoint, rateLimiter, methodName);
				}
			}
		}
		if (CompletionStage.class.isAssignableFrom(returnType)) {
			return handleJoinPointCompletableFuture(proceedingJoinPoint, rateLimiter);
		}
		return handleJoinPoint(proceedingJoinPoint, rateLimiter);
	}

	private io.github.resilience4j.ratelimiter.RateLimiter getOrCreateRateLimiter(String methodName, String name) {
		io.github.resilience4j.ratelimiter.RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(name);

		if (logger.isDebugEnabled()) {
			RateLimiterConfig rateLimiterConfig = rateLimiter.getRateLimiterConfig();
			logger.debug(
					RATE_LIMITER_RECEIVED,
					name, rateLimiterConfig.getLimitRefreshPeriod(), rateLimiterConfig.getLimitForPeriod(),
					rateLimiterConfig.getTimeoutDuration(), methodName
			);
		}

		return rateLimiter;
	}

	@Nullable
	private RateLimiter getRateLimiterAnnotation(ProceedingJoinPoint proceedingJoinPoint) {
		return AnnotationExtractor.extract(proceedingJoinPoint.getTarget().getClass(), RateLimiter.class);
	}

	private Object handleJoinPoint(ProceedingJoinPoint proceedingJoinPoint,
								   io.github.resilience4j.ratelimiter.RateLimiter rateLimiter)
			throws Throwable {
		return rateLimiter.executeCheckedSupplier(proceedingJoinPoint::proceed);
	}

	/**
	 * handle the asynchronous completable future flow
	 *
	 * @param proceedingJoinPoint AOPJoinPoint
	 * @param rateLimiter         configured rate limiter
	 * @return CompletionStage
	 */
	private Object handleJoinPointCompletableFuture(ProceedingJoinPoint proceedingJoinPoint, io.github.resilience4j.ratelimiter.RateLimiter rateLimiter) {
		return rateLimiter.executeCompletionStage(() -> {
			try {
				return (CompletionStage<?>) proceedingJoinPoint.proceed();
			} catch (Throwable throwable) {
				throw new CompletionException(throwable);
			}
		});
	}


	@Override
	public int getOrder() {
		return properties.getRateLimiterAspectOrder();
	}
}
