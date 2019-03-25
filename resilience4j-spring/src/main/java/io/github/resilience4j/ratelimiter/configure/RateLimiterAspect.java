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

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.recovery.RecoveryFunction;
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

    public RateLimiterAspect(RateLimiterRegistry rateLimiterRegistry, RateLimiterConfigurationProperties properties) {
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.properties = properties;
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
    public Object rateLimiterAroundAdvice(ProceedingJoinPoint proceedingJoinPoint, RateLimiter limitedService) throws Throwable {
        RateLimiter targetService = limitedService;
        Method method = ((MethodSignature) proceedingJoinPoint.getSignature()).getMethod();
        String methodName = method.getDeclaringClass().getName() + "#" + method.getName();
        if (targetService == null) {
            targetService = getRateLimiterAnnotation(proceedingJoinPoint);
        }
        String name = targetService.name();
        io.github.resilience4j.ratelimiter.RateLimiter rateLimiter = getOrCreateRateLimiter(methodName, name);
        RecoveryFunction recovery = targetService.recovery().newInstance();
        return handleJoinPoint(proceedingJoinPoint, rateLimiter, recovery, methodName);
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

    private RateLimiter getRateLimiterAnnotation(ProceedingJoinPoint proceedingJoinPoint) {
        RateLimiter rateLimiter = null;
        Class<?> targetClass = proceedingJoinPoint.getTarget().getClass();
        if (targetClass.isAnnotationPresent(RateLimiter.class)) {
            rateLimiter = targetClass.getAnnotation(RateLimiter.class);
            if (rateLimiter == null) {
                rateLimiter = targetClass.getDeclaredAnnotation(RateLimiter.class);
            }
            if (rateLimiter == null) {
                logger.debug("TargetClass has no declared annotation 'RateLimiter'");
            }
        }
        return rateLimiter;
    }

    @SuppressWarnings("unchecked")
    private Object handleJoinPoint(ProceedingJoinPoint proceedingJoinPoint,
                                   io.github.resilience4j.ratelimiter.RateLimiter rateLimiter,
                                   RecoveryFunction recovery,
                                   String methodName)
            throws Throwable {
        try {
            io.github.resilience4j.ratelimiter.RateLimiter.waitForPermission(rateLimiter);
            return proceedingJoinPoint.proceed();
        } catch (Exception exception) {
            if (logger.isDebugEnabled()) {
                logger.debug("Invocation of method '" + methodName + "' failed!", exception);
            }

            return recovery.apply(exception);
        }
    }

    @Override
    public int getOrder() {
        return properties.getRateLimiterAspectOrder();
    }
}
