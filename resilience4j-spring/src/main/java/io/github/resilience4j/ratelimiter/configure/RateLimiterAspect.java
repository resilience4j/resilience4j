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
import io.github.resilience4j.fallback.FallbackDecorators;
import io.github.resilience4j.fallback.FallbackMethod;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.spelresolver.SpelResolver;
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
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

/**
 * This Spring AOP aspect intercepts all methods which are annotated with a {@link RateLimiter}
 * annotation. The aspect will handle methods that return a RxJava2 reactive type, Spring Reactor
 * reactive type, CompletionStage type, or value type.
 * <p>
 * The RateLimiterRegistry is used to retrieve an instance of a RateLimiter for a specific backend.
 * <p>
 * Given a method like this:
 * <pre><code>
 *     {@literal @}RateLimiter(name = "myService")
 *     public String fancyName(String name) {
 *         return "Sir Captain " + name;
 *     }
 * </code></pre>
 * each time the {@code #fancyName(String)} method is invoked, the method's execution will pass
 * through a a {@link io.github.resilience4j.ratelimiter.RateLimiter} according to the given
 * config.
 * <p>
 * The fallbackMethod parameter signature must match either:
 * <p>
 * 1) The method parameter signature on the annotated method or 2) The method parameter signature
 * with a matching exception type as the last parameter on the annotated method
 */

@Aspect
public class RateLimiterAspect implements Ordered {

    private static final String RATE_LIMITER_RECEIVED = "Created or retrieved rate limiter '{}' with period: '{}'; limit for period: '{}'; timeout: '{}'; method: '{}'";
    private static final Logger logger = LoggerFactory.getLogger(RateLimiterAspect.class);
    private final RateLimiterRegistry rateLimiterRegistry;
    private final RateLimiterConfigurationProperties properties;
    private final @Nullable
    List<RateLimiterAspectExt> rateLimiterAspectExtList;
    private final FallbackDecorators fallbackDecorators;
    private final SpelResolver spelResolver;

    public RateLimiterAspect(RateLimiterRegistry rateLimiterRegistry,
                             RateLimiterConfigurationProperties properties,
                             @Autowired(required = false) List<RateLimiterAspectExt> rateLimiterAspectExtList,
                             FallbackDecorators fallbackDecorators,
                             SpelResolver spelResolver) {
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.properties = properties;
        this.rateLimiterAspectExtList = rateLimiterAspectExtList;
        this.fallbackDecorators = fallbackDecorators;
        this.spelResolver = spelResolver;
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

    @Around(value = "matchAnnotatedClassOrMethod(rateLimiterAnnotation)", argNames = "proceedingJoinPoint, rateLimiterAnnotation")
    public Object rateLimiterAroundAdvice(ProceedingJoinPoint proceedingJoinPoint,
        @Nullable RateLimiter rateLimiterAnnotation) throws Throwable {
        Method method = ((MethodSignature) proceedingJoinPoint.getSignature()).getMethod();
        String methodName = method.getDeclaringClass().getName() + "#" + method.getName();
        if (rateLimiterAnnotation == null) {
            rateLimiterAnnotation = getRateLimiterAnnotation(proceedingJoinPoint);
        }
        if (rateLimiterAnnotation == null) { //because annotations wasn't found
            return proceedingJoinPoint.proceed();
        }
        String name = spelResolver.resolve(method, proceedingJoinPoint.getArgs(), rateLimiterAnnotation.name());
        io.github.resilience4j.ratelimiter.RateLimiter rateLimiter = getOrCreateRateLimiter(
            methodName, name);
        Class<?> returnType = method.getReturnType();

        String fallbackMethodValue = spelResolver.resolve(method, proceedingJoinPoint.getArgs(), rateLimiterAnnotation.fallbackMethod());
        if (StringUtils.isEmpty(fallbackMethodValue)) {
            return proceed(proceedingJoinPoint, methodName, returnType, rateLimiter);
        }
        FallbackMethod fallbackMethod = FallbackMethod
            .create(fallbackMethodValue, method, proceedingJoinPoint.getArgs(),
                proceedingJoinPoint.getTarget());
        return fallbackDecorators.decorate(fallbackMethod,
            () -> proceed(proceedingJoinPoint, methodName, returnType, rateLimiter)).get();
    }

    private Object proceed(ProceedingJoinPoint proceedingJoinPoint, String methodName,
        Class<?> returnType, io.github.resilience4j.ratelimiter.RateLimiter rateLimiter)
        throws Throwable {
        if (rateLimiterAspectExtList != null && !rateLimiterAspectExtList.isEmpty()) {
            for (RateLimiterAspectExt rateLimiterAspectExt : rateLimiterAspectExtList) {
                if (rateLimiterAspectExt.canHandleReturnType(returnType)) {
                    return rateLimiterAspectExt
                        .handle(proceedingJoinPoint, rateLimiter, methodName);
                }
            }
        }
        if (CompletionStage.class.isAssignableFrom(returnType)) {
            return handleJoinPointCompletableFuture(proceedingJoinPoint, rateLimiter);
        }
        return handleJoinPoint(proceedingJoinPoint, rateLimiter);
    }

    private io.github.resilience4j.ratelimiter.RateLimiter getOrCreateRateLimiter(String methodName,
        String name) {
        io.github.resilience4j.ratelimiter.RateLimiter rateLimiter = rateLimiterRegistry
            .rateLimiter(name);

        if (logger.isDebugEnabled()) {
            RateLimiterConfig rateLimiterConfig = rateLimiter.getRateLimiterConfig();
            logger.debug(
                RATE_LIMITER_RECEIVED,
                name, rateLimiterConfig.getLimitRefreshPeriod(),
                rateLimiterConfig.getLimitForPeriod(),
                rateLimiterConfig.getTimeoutDuration(), methodName
            );
        }

        return rateLimiter;
    }

    @Nullable
    private RateLimiter getRateLimiterAnnotation(ProceedingJoinPoint proceedingJoinPoint) {
        if (proceedingJoinPoint.getTarget() instanceof Proxy) {
            logger.debug(
                "The rate limiter annotation is kept on a interface which is acting as a proxy");
            return AnnotationExtractor
                .extractAnnotationFromProxy(proceedingJoinPoint.getTarget(), RateLimiter.class);
        } else {
            return AnnotationExtractor
                .extract(proceedingJoinPoint.getTarget().getClass(), RateLimiter.class);
        }
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
    private Object handleJoinPointCompletableFuture(ProceedingJoinPoint proceedingJoinPoint,
        io.github.resilience4j.ratelimiter.RateLimiter rateLimiter) {
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
