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
package io.github.resilience4j.spring6.circuitbreaker.configure;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.spring6.fallback.FallbackExecutor;
import io.github.resilience4j.spring6.spelresolver.SpelResolver;
import io.github.resilience4j.spring6.utils.AnnotationExtractor;
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
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

/**
 * This Spring AOP aspect intercepts all methods which are annotated with a {@link CircuitBreaker}
 * annotation. The aspect will handle methods that return a RxJava2 reactive type, Spring Reactor
 * reactive type, CompletionStage type, or value type.
 * <p>
 * The CircuitBreakerRegistry is used to retrieve an instance of a CircuitBreaker for a specific
 * name.
 * <p>
 * Given a method like this:
 * <pre><code>
 *     {@literal @}CircuitBreaker(name = "myService")
 *     public String fancyName(String name) {
 *         return "Sir Captain " + name;
 *     }
 * </code></pre>
 * each time the {@code #fancyName(String)} method is invoked, the method's execution will pass
 * through a a {@link io.github.resilience4j.circuitbreaker.CircuitBreaker} according to the given
 * config.
 * <p>
 * The fallbackMethod parameter signature must match either:
 * <p>
 * 1) The method parameter signature on the annotated method or 2) The method parameter signature
 * with a matching exception type as the last parameter on the annotated method
 */
@Aspect
public class CircuitBreakerAspect implements Ordered {

    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerAspect.class);

    private final CircuitBreakerConfigurationProperties circuitBreakerProperties;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final @Nullable
    List<CircuitBreakerAspectExt> circuitBreakerAspectExtList;
    private final FallbackExecutor fallbackExecutor;
    private final SpelResolver spelResolver;

    public CircuitBreakerAspect(CircuitBreakerConfigurationProperties circuitBreakerProperties,
                                CircuitBreakerRegistry circuitBreakerRegistry,
                                @Autowired(required = false) List<CircuitBreakerAspectExt> circuitBreakerAspectExtList,
                                FallbackExecutor fallbackExecutor,
                                SpelResolver spelResolver) {
        this.circuitBreakerProperties = circuitBreakerProperties;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.circuitBreakerAspectExtList = circuitBreakerAspectExtList;
        this.fallbackExecutor = fallbackExecutor;
        this.spelResolver = spelResolver;
    }

    @Pointcut(value = "@within(circuitBreaker) || @annotation(circuitBreaker)", argNames = "circuitBreaker")
    public void matchAnnotatedClassOrMethod(CircuitBreaker circuitBreaker) {
    }

    @Around(value = "matchAnnotatedClassOrMethod(circuitBreakerAnnotation)", argNames = "proceedingJoinPoint, circuitBreakerAnnotation")
    public Object circuitBreakerAroundAdvice(ProceedingJoinPoint proceedingJoinPoint,
        @Nullable CircuitBreaker circuitBreakerAnnotation) throws Throwable {
        Method method = ((MethodSignature) proceedingJoinPoint.getSignature()).getMethod();
        String methodName = method.getDeclaringClass().getName() + "#" + method.getName();
        if (circuitBreakerAnnotation == null) {
            circuitBreakerAnnotation = getCircuitBreakerAnnotation(proceedingJoinPoint);
        }
        if (circuitBreakerAnnotation == null) { //because annotations wasn't found
            return proceedingJoinPoint.proceed();
        }
        String backend = spelResolver.resolve(method, proceedingJoinPoint.getArgs(), circuitBreakerAnnotation.name());
        io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(
            methodName, backend);
        Class<?> returnType = method.getReturnType();
        final CheckedSupplier<Object> circuitBreakerExecution = () -> proceed(proceedingJoinPoint, methodName, circuitBreaker, returnType);
        return fallbackExecutor.execute(proceedingJoinPoint, method, circuitBreakerAnnotation.fallbackMethod(), circuitBreakerExecution);
    }

    private Object proceed(ProceedingJoinPoint proceedingJoinPoint, String methodName,
        io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker, Class<?> returnType)
        throws Throwable {
        if (circuitBreakerAspectExtList != null && !circuitBreakerAspectExtList.isEmpty()) {
            for (CircuitBreakerAspectExt circuitBreakerAspectExt : circuitBreakerAspectExtList) {
                if (circuitBreakerAspectExt.canHandleReturnType(returnType)) {
                    return circuitBreakerAspectExt
                        .handle(proceedingJoinPoint, circuitBreaker, methodName);
                }
            }
        }
        if (CompletionStage.class.isAssignableFrom(returnType)) {
            return handleJoinPointCompletableFuture(proceedingJoinPoint, circuitBreaker);
        }
        return defaultHandling(proceedingJoinPoint, circuitBreaker);
    }

    private io.github.resilience4j.circuitbreaker.CircuitBreaker getOrCreateCircuitBreaker(
        String methodName, String backend) {
        CircuitBreakerConfig config = circuitBreakerRegistry.getConfiguration(backend)
            .orElse(circuitBreakerRegistry.getDefaultConfig());
        io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker = circuitBreakerRegistry
            .circuitBreaker(backend, config);

        if (logger.isDebugEnabled()) {
            logger.debug(
                "Created or retrieved circuit breaker '{}' with failure rate '{}' for method: '{}'",
                backend, circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold(),
                methodName);
        }

        return circuitBreaker;
    }

    @Nullable
    private CircuitBreaker getCircuitBreakerAnnotation(ProceedingJoinPoint proceedingJoinPoint) {
        if (logger.isDebugEnabled()) {
            logger.debug("circuitBreaker parameter is null");
        }
        if (proceedingJoinPoint.getTarget() instanceof Proxy) {
            logger.debug(
                "The circuit breaker annotation is kept on a interface which is acting as a proxy");
            return AnnotationExtractor
                .extractAnnotationFromProxy(proceedingJoinPoint.getTarget(), CircuitBreaker.class);
        } else {
            return AnnotationExtractor
                .extract(proceedingJoinPoint.getTarget().getClass(), CircuitBreaker.class);
        }
    }

    /**
     * handle the CompletionStage return types AOP based into configured circuit-breaker
     */
    private Object handleJoinPointCompletableFuture(ProceedingJoinPoint proceedingJoinPoint,
        io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker) {
        return circuitBreaker.executeCompletionStage(() -> {
            try {
                return (CompletionStage<?>) proceedingJoinPoint.proceed();
            } catch (Throwable throwable) {
                throw new CompletionException(throwable);
            }
        });
    }

    /**
     * the default Java types handling for the circuit breaker AOP
     */
    private Object defaultHandling(ProceedingJoinPoint proceedingJoinPoint,
        io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker) throws Throwable {
        return circuitBreaker.executeCheckedSupplier(proceedingJoinPoint::proceed);
    }

    @Override
    public int getOrder() {
        return circuitBreakerProperties.getCircuitBreakerAspectOrder();
    }
}
