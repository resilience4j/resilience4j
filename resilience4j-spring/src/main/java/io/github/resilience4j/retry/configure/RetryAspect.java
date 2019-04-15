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
package io.github.resilience4j.retry.configure;

import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.annotation.Retry;
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
import java.util.concurrent.*;

/**
 * This Spring AOP aspect intercepts all methods which are annotated with a {@link Retry} annotation.
 * The aspect protects an annotated method with a Retry. The RetryRegistry is used to retrieve an instance of a Retry for
 * a specific name.
 */
@Aspect
public class RetryAspect implements Ordered {

	private static final Logger logger = LoggerFactory.getLogger(RetryAspect.class);
	private final static ScheduledExecutorService retryExecutorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
	private final RetryConfigurationProperties retryConfigurationProperties;
	private final RetryRegistry retryRegistry;
	private final @Nullable List<RetryAspectExt> retryAspectExtList;

	/**
	 * @param retryConfigurationProperties spring retry config properties
	 * @param retryRegistry                retry definition registry
	 * @param retryAspectExtList
	 */
	public RetryAspect(RetryConfigurationProperties retryConfigurationProperties, RetryRegistry retryRegistry, @Autowired(required = false) List<RetryAspectExt> retryAspectExtList) {
		this.retryConfigurationProperties = retryConfigurationProperties;
		this.retryRegistry = retryRegistry;
		this.retryAspectExtList = retryAspectExtList;
		cleanup();

	}

	@Pointcut(value = "@within(retry) || @annotation(retry)", argNames = "retry")
	public void matchAnnotatedClassOrMethod(Retry retry) {
	}

	@Around(value = "matchAnnotatedClassOrMethod(backendMonitored)", argNames = "proceedingJoinPoint, backendMonitored")
	public Object retryAroundAdvice(ProceedingJoinPoint proceedingJoinPoint, @Nullable Retry backendMonitored) throws Throwable {
		Method method = ((MethodSignature) proceedingJoinPoint.getSignature()).getMethod();
		String methodName = method.getDeclaringClass().getName() + "#" + method.getName();
		if (backendMonitored == null) {
			backendMonitored = getBackendMonitoredAnnotation(proceedingJoinPoint);
		}
		if(backendMonitored == null) { //because annotations wasn't found
			return proceedingJoinPoint.proceed();
		}
		String backend = backendMonitored.name();
		io.github.resilience4j.retry.Retry retry = getOrCreateRetry(methodName, backend);
		Class<?> returnType = method.getReturnType();
		if (CompletionStage.class.isAssignableFrom(returnType)) {
			return handleJoinPointCompletableFuture(proceedingJoinPoint, retry);
		}
		if (retryAspectExtList != null && !retryAspectExtList.isEmpty()) {
			for (RetryAspectExt retryAspectExt : retryAspectExtList) {
				if (retryAspectExt.canHandleReturnType(returnType)) {
					return retryAspectExt.handle(proceedingJoinPoint, retry, methodName);
				}
			}
		}
		return handleDefaultJoinPoint(proceedingJoinPoint, retry);
	}

	/**
	 * @param methodName the retry method name
	 * @param backend    the retry backend name
	 * @return the configured retry
	 */
	private io.github.resilience4j.retry.Retry getOrCreateRetry(String methodName, String backend) {
		io.github.resilience4j.retry.Retry retry = retryRegistry.retry(backend,
				() -> retryConfigurationProperties.createRetryConfig(backend));

		if (logger.isDebugEnabled()) {
			logger.debug("Created or retrieved retry '{}' with max attempts rate '{}'  for method: '{}'",
					backend, retry.getRetryConfig().getResultPredicate(), methodName);
		}
		return retry;
	}

	/**
	 * @param proceedingJoinPoint the aspect joint point
	 * @return the retry annotation
	 */
	@Nullable
	private Retry getBackendMonitoredAnnotation(ProceedingJoinPoint proceedingJoinPoint) {
		return AnnotationExtractor.extract(proceedingJoinPoint.getTarget().getClass(), Retry.class);
	}

	/**
	 * @param proceedingJoinPoint the AOP logic joint point
	 * @param retry               the configured sync retry
	 * @return the result object if any
	 * @throws Throwable
	 */
	private Object handleDefaultJoinPoint(ProceedingJoinPoint proceedingJoinPoint, io.github.resilience4j.retry.Retry retry) throws Throwable {
		return retry.executeCheckedSupplier(proceedingJoinPoint::proceed);
	}

	/**
	 * @param proceedingJoinPoint the AOP logic joint point
	 * @param retry               the configured async retry
	 * @return the result object if any
	 */
	@SuppressWarnings("unchecked")
	private Object handleJoinPointCompletableFuture(ProceedingJoinPoint proceedingJoinPoint, io.github.resilience4j.retry.Retry retry) {
		return retry.executeCompletionStage(retryExecutorService, () -> {
			try {
				return (CompletionStage<Object>) proceedingJoinPoint.proceed();
			} catch (Throwable throwable) {
				throw new CompletionException(throwable);
			}
		});
	}


	@Override
	public int getOrder() {
		return retryConfigurationProperties.getRetryAspectOrder();
	}

	private void cleanup() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			retryExecutorService.shutdown();
			try {
				if (!retryExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
					retryExecutorService.shutdownNow();
				}
			} catch (InterruptedException e) {
				if (!retryExecutorService.isTerminated()) {
					retryExecutorService.shutdownNow();
				}
				Thread.currentThread().interrupt();
			}
		}));
	}

}
