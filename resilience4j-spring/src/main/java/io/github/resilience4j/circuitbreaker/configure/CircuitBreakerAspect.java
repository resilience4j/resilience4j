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
import io.github.resilience4j.circuitbreaker.utils.CircuitBreakerUtils;
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
 * This Spring AOP aspect intercepts all methods which are annotated with a {@link CircuitBreaker} annotation.
 * The aspect protects an annotated method with a CircuitBreaker. The CircuitBreakerRegistry is used to retrieve an instance of a CircuitBreaker for
 * a specific backend.
 */
@Aspect
public class CircuitBreakerAspect implements Ordered {

    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerAspect.class);

    private final CircuitBreakerConfigurationProperties circuitBreakerProperties;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public CircuitBreakerAspect(CircuitBreakerConfigurationProperties backendMonitorPropertiesRegistry, CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerProperties = backendMonitorPropertiesRegistry;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Pointcut(value = "@within(circuitBreaker) || @annotation(circuitBreaker)", argNames = "circuitBreaker")
    public void matchAnnotatedClassOrMethod(CircuitBreaker circuitBreaker) {
    }

    @Around(value = "matchAnnotatedClassOrMethod(backendMonitored)", argNames = "proceedingJoinPoint, backendMonitored")
    public Object circuitBreakerAroundAdvice(ProceedingJoinPoint proceedingJoinPoint, CircuitBreaker backendMonitored) throws Throwable {
        Method method = ((MethodSignature) proceedingJoinPoint.getSignature()).getMethod();
        String methodName = method.getDeclaringClass().getName() + "#" + method.getName();
        if (backendMonitored == null) {
            backendMonitored = getBackendMonitoredAnnotation(proceedingJoinPoint);
        }
        String backend = backendMonitored.backend();
        io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(methodName, backend);
        return handleJoinPoint(proceedingJoinPoint, circuitBreaker, methodName);
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

    private CircuitBreaker getBackendMonitoredAnnotation(ProceedingJoinPoint proceedingJoinPoint) {
        if (logger.isDebugEnabled()) {
            logger.debug("circuitBreaker parameter is null");
        }
        CircuitBreaker circuitBreaker = null;
        Class<?> targetClass = proceedingJoinPoint.getTarget().getClass();
        if (targetClass.isAnnotationPresent(CircuitBreaker.class)) {
            circuitBreaker = targetClass.getAnnotation(CircuitBreaker.class);
            if (circuitBreaker == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("TargetClass has no annotation 'CircuitBreaker'");
                }
                circuitBreaker = targetClass.getDeclaredAnnotation(CircuitBreaker.class);
                if (circuitBreaker == null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("TargetClass has no declared annotation 'CircuitBreaker'");
                    }
                }
            }
        }
        return circuitBreaker;
    }

    private Object handleJoinPoint(ProceedingJoinPoint proceedingJoinPoint, io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker, String methodName) throws Throwable {
        CircuitBreakerUtils.isCallPermitted(circuitBreaker);
        long start = System.nanoTime();
        try {
            Object returnValue = proceedingJoinPoint.proceed();

            long durationInNanos = System.nanoTime() - start;
            circuitBreaker.onSuccess(durationInNanos);
            return returnValue;
        } catch (Throwable throwable) {
            long durationInNanos = System.nanoTime() - start;
            circuitBreaker.onError(durationInNanos, throwable);
            if (logger.isDebugEnabled()) {
                logger.debug("Invocation of method '" + methodName + "' failed!", throwable);
            }
            throw throwable;
        }
    }

    @Override
    public int getOrder() {
        return circuitBreakerProperties.getCircuitBreakerAspectOrder();
    }
}
