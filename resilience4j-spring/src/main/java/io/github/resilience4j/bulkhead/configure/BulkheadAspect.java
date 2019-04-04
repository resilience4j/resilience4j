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
import io.github.resilience4j.bulkhead.utils.BulkheadUtils;
import io.github.resilience4j.utils.AnnotationExtractor;
import io.github.resilience4j.utils.RecoveryUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;

import java.lang.reflect.Method;

/**
 * This Spring AOP aspect intercepts all methods which are annotated with a {@link Bulkhead} annotation.
 * The aspect protects an annotated method with a Bulkhead. The BulkheadRegistry is used to retrieve an instance of a Bulkhead for
 * a specific name.
 */
@Aspect
public class BulkheadAspect implements Ordered {

	private static final Logger logger = LoggerFactory.getLogger(BulkheadAspect.class);

	private final BulkheadConfigurationProperties bulkheadConfigurationProperties;
	private final BulkheadRegistry bulkheadRegistry;

	public BulkheadAspect(BulkheadConfigurationProperties backendMonitorPropertiesRegistry, BulkheadRegistry bulkheadRegistry) {
		this.bulkheadConfigurationProperties = backendMonitorPropertiesRegistry;
		this.bulkheadRegistry = bulkheadRegistry;
	}

	@Pointcut(value = "@within(Bulkhead) || @annotation(Bulkhead)", argNames = "Bulkhead")
	public void matchAnnotatedClassOrMethod(Bulkhead Bulkhead) {
	}

	@Around(value = "matchAnnotatedClassOrMethod(backendMonitored)", argNames = "proceedingJoinPoint, backendMonitored")
	public Object bulkheadAroundAdvice(ProceedingJoinPoint proceedingJoinPoint, Bulkhead backendMonitored) throws Throwable {
		Method method = ((MethodSignature) proceedingJoinPoint.getSignature()).getMethod();
		String methodName = method.getDeclaringClass().getName() + "#" + method.getName();
		if (backendMonitored == null) {
			backendMonitored = getBackendMonitoredAnnotation(proceedingJoinPoint);
		}
		String backend = backendMonitored.name();
		io.github.resilience4j.bulkhead.Bulkhead bulkhead = getOrCreateBulkhead(methodName, backend);

		return handleJoinPoint(proceedingJoinPoint, bulkhead, backendMonitored.recovery(), methodName);
	}

	private io.github.resilience4j.bulkhead.Bulkhead getOrCreateBulkhead(String methodName, String backend) {
		io.github.resilience4j.bulkhead.Bulkhead bulkhead = bulkheadRegistry.bulkhead(backend,
				() -> bulkheadConfigurationProperties.createBulkheadConfig(backend));

		if (logger.isDebugEnabled()) {
			logger.debug("Created or retrieved bulkhead '{}' with max concurrent call '{}' and max wait time '{}' for method: '{}'",
					backend, bulkhead.getBulkheadConfig().getMaxConcurrentCalls(),
					bulkhead.getBulkheadConfig().getMaxWaitTime(), methodName);
		}

		return bulkhead;
	}

	private Bulkhead getBackendMonitoredAnnotation(ProceedingJoinPoint proceedingJoinPoint) {
		if (logger.isDebugEnabled()) {
			logger.debug("bulkhead parameter is null");
		}

		return AnnotationExtractor.extract(proceedingJoinPoint.getTarget().getClass(), Bulkhead.class);
	}

	@SuppressWarnings("unchecked")
	private Object handleJoinPoint(ProceedingJoinPoint proceedingJoinPoint, io.github.resilience4j.bulkhead.Bulkhead  bulkhead, String recoveryMethodName, String methodName) throws Throwable {
		BulkheadUtils.isCallPermitted(bulkhead);
		try {
			return proceedingJoinPoint.proceed();
		} catch (Throwable throwable) {
			if (logger.isDebugEnabled()) {
				logger.debug("Invocation of method '" + methodName + "' failed!", throwable);
			}

			return RecoveryUtils.invoke(recoveryMethodName, proceedingJoinPoint.getArgs(), throwable, proceedingJoinPoint.getThis());
		} finally {
			bulkhead.onComplete();
		}
	}

	@Override
	public int getOrder() {
		return bulkheadConfigurationProperties.getBulkheadAspectOrder();
	}
}
