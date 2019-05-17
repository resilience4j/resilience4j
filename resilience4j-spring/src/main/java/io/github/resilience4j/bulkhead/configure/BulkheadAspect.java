/*
 * Copyright 2019 lespinsideg
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

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.fallback.FallbackDecorators;
import io.github.resilience4j.fallback.FallbackMethod;
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
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

/**
 * This Spring AOP aspect intercepts all methods which are annotated with a {@link Bulkhead} annotation.
 * The aspect will handle methods that return a RxJava2 reactive type, Spring Reactor reactive type, CompletionStage type, or value type.
 *
 * The BulkheadRegistry is used to retrieve an instance of a Bulkhead for a specific name.
 *
 * Given a method like this:
 * <pre><code>
 *     {@literal @}Bulkhead(name = "myService")
 *     public String fancyName(String name) {
 *         return "Sir Captain " + name;
 *     }
 * </code></pre>
 * each time the {@code #fancyName(String)} method is invoked, the method's execution will pass through a
 * a {@link io.github.resilience4j.bulkhead.Bulkhead} according to the given config.
 *
 * The fallbackMethod parameter signature must match either:
 *
 * 1) The method parameter signature on the annotated method or
 * 2) The method parameter signature with a matching exception type as the last parameter on the annotated method
 */
@Aspect
public class BulkheadAspect implements Ordered {

	private static final Logger logger = LoggerFactory.getLogger(BulkheadAspect.class);

	private final BulkheadConfigurationProperties bulkheadConfigurationProperties;
	private final BulkheadRegistry bulkheadRegistry;
	private final @Nullable
	List<BulkheadAspectExt> bulkheadAspectExts;
	private final FallbackDecorators fallbackDecorators;

	public BulkheadAspect(BulkheadConfigurationProperties backendMonitorPropertiesRegistry, BulkheadRegistry bulkheadRegistry, @Autowired(required = false) List<BulkheadAspectExt> bulkheadAspectExts, FallbackDecorators fallbackDecorators) {
		this.bulkheadConfigurationProperties = backendMonitorPropertiesRegistry;
		this.bulkheadRegistry = bulkheadRegistry;
		this.bulkheadAspectExts = bulkheadAspectExts;
		this.fallbackDecorators = fallbackDecorators;
	}

	@Pointcut(value = "@within(Bulkhead) || @annotation(Bulkhead)", argNames = "Bulkhead")
	public void matchAnnotatedClassOrMethod(Bulkhead Bulkhead) {
	}

	@Around(value = "matchAnnotatedClassOrMethod(bulkheadAnnotation)", argNames = "proceedingJoinPoint, bulkheadAnnotation")
	public Object bulkheadAroundAdvice(ProceedingJoinPoint proceedingJoinPoint, @Nullable Bulkhead bulkheadAnnotation) throws Throwable {
		Method method = ((MethodSignature) proceedingJoinPoint.getSignature()).getMethod();
		String methodName = method.getDeclaringClass().getName() + "#" + method.getName();
		if (bulkheadAnnotation == null) {
			bulkheadAnnotation = geBulkheadAnnotation(proceedingJoinPoint);
		}
		if (bulkheadAnnotation == null) { //because annotations wasn't found
			return proceedingJoinPoint.proceed();
		}
		String backend = bulkheadAnnotation.name();
		io.github.resilience4j.bulkhead.Bulkhead bulkhead = getOrCreateBulkhead(methodName, backend);
		Class<?> returnType = method.getReturnType();

		if (StringUtils.isEmpty(bulkheadAnnotation.fallbackMethod())) {
			return proceed(proceedingJoinPoint, methodName, bulkhead, returnType);
		}

		FallbackMethod fallbackMethod = new FallbackMethod(bulkheadAnnotation.fallbackMethod(), method, proceedingJoinPoint.getArgs(), proceedingJoinPoint.getTarget());
		return fallbackDecorators.decorate(fallbackMethod, () -> proceed(proceedingJoinPoint, methodName, bulkhead, returnType)).apply();
	}

	private Object proceed(ProceedingJoinPoint proceedingJoinPoint, String methodName, io.github.resilience4j.bulkhead.Bulkhead bulkhead, Class<?> returnType) throws Throwable {
		if (bulkheadAspectExts != null && !bulkheadAspectExts.isEmpty()) {
			for (BulkheadAspectExt bulkHeadAspectExt : bulkheadAspectExts) {
				if (bulkHeadAspectExt.canHandleReturnType(returnType)) {
					return bulkHeadAspectExt.handle(proceedingJoinPoint, bulkhead, methodName);
				}
			}
		}
		if (CompletionStage.class.isAssignableFrom(returnType)) {
			return handleJoinPointCompletableFuture(proceedingJoinPoint, bulkhead);
		}
		return handleJoinPoint(proceedingJoinPoint, bulkhead);
	}

	private io.github.resilience4j.bulkhead.Bulkhead getOrCreateBulkhead(String methodName, String backend) {
		io.github.resilience4j.bulkhead.Bulkhead bulkhead = bulkheadRegistry.bulkhead(backend);

		if (logger.isDebugEnabled()) {
			logger.debug("Created or retrieved bulkhead '{}' with max concurrent call '{}' and max wait time '{}' for method: '{}'",
					backend, bulkhead.getBulkheadConfig().getMaxConcurrentCalls(),
					bulkhead.getBulkheadConfig().getMaxWaitTime(), methodName);
		}

		return bulkhead;
	}

	@Nullable
	private Bulkhead geBulkheadAnnotation(ProceedingJoinPoint proceedingJoinPoint) {
		if (logger.isDebugEnabled()) {
			logger.debug("bulkhead parameter is null");
		}

		return AnnotationExtractor.extract(proceedingJoinPoint.getTarget().getClass(), Bulkhead.class);
	}

	private Object handleJoinPoint(ProceedingJoinPoint proceedingJoinPoint, io.github.resilience4j.bulkhead.Bulkhead bulkhead) throws Throwable {
		return bulkhead.executeCheckedSupplier(proceedingJoinPoint::proceed);
	}

	/**
	 * handle the asynchronous completable future flow
	 *
	 * @param proceedingJoinPoint AOPJoinPoint
	 * @param bulkhead            configured bulkhead
	 * @return CompletionStage
	 */
	private Object handleJoinPointCompletableFuture(ProceedingJoinPoint proceedingJoinPoint, io.github.resilience4j.bulkhead.Bulkhead bulkhead) {
		return bulkhead.executeCompletionStage(() -> {
			try {
				return (CompletionStage<?>) proceedingJoinPoint.proceed();
			} catch (Throwable throwable) {
				throw new CompletionException(throwable);
			}
		});
	}

	@Override
	public int getOrder() {
		return bulkheadConfigurationProperties.getBulkheadAspectOrder();
	}
}
