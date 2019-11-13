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
package io.github.resilience4j.decorators.configure;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.bulkhead.configure.BulkheadAspectHelper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerAspectHelper;
import io.github.resilience4j.retry.configure.*;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;

import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.decorators.annotation.Decorator;
import io.github.resilience4j.decorators.annotation.Decorators;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.ratelimiter.configure.RateLimiterAspectHelper;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.utils.ProceedingJoinPointHelper;
import java.util.Arrays;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Value;

/**
 * This Spring AOP aspect intercepts all methods which are annotated with a
 * {@link Decorator} annotation. The aspect will handle methods that return a
 * RxJava2 reactive type, Spring Reactor reactive type, CompletionStage type, or
 * value type.
 *
 * The RetryRegistry is used to retrieve an instance of a Retry for a specific
 * name.
 *
 * Given a method like this:
 * <pre><code>
 *     {@literal @}Decorator(retry = @Retry(name = "readTimeouts"))
 *     {@literal @}Decorator(rateLimiter = @RateLimiter(name = "myServiceLimiter"))
 *     {@literal @}Decorator(retry = @Retry(name = "connectionTimeouts"))
 *     public String getInformation() throws Exception {
 *         return restTemplate.getForEntity(informationUrl, String.class);
 *     }
 * </code></pre> each time the {@code #getInformation()} method is invoked, the
 * method's execution will pass through all the decorators specified in the above
 * annotation.
 * 
 * Calling a Spring bean method with the annotations given above is equivalent to:
 * 
 * <pre><code>
 *     Decorators.ofCheckedSupplier(() -{@literal >} service.getInformation())
 *         .withRetry(retryRegistry.retry("readtimeouts"))
 *         .withRateLimiter(rateLimiterRegistry.rateLimiter(""))
 *         .withRetry(retryRegistry.retry(""))
 *         .get();
 * </code></pre>
 */
@Aspect
public class DecoratorsAspect implements Ordered {

    private static final Logger logger = LoggerFactory.getLogger(DecoratorsAspect.class);
    private final RetryAspectHelper retryAspectHelper;
    private final RateLimiterAspectHelper rateLimiterAspectHelper;
    private final BulkheadAspectHelper bulkheadAspectHelper;
    private final CircuitBreakerAspectHelper circuitBreakerAspectHelper;
    private final int order;

    public DecoratorsAspect(
            RetryAspectHelper retryAspectHelper,
            RateLimiterAspectHelper rateLimiterAspectHelper,
            BulkheadAspectHelper bulkheadAspectHelper,
            CircuitBreakerAspectHelper circuitBreakerAspectHelper,
            int order) {
        this.retryAspectHelper = retryAspectHelper;
        this.rateLimiterAspectHelper = rateLimiterAspectHelper;
        this.bulkheadAspectHelper = bulkheadAspectHelper;
        this.circuitBreakerAspectHelper = circuitBreakerAspectHelper;
        this.order = order;
    }

    @Pointcut(value = "@within(decorator) || @annotation(decorator)", argNames = "decorator")
    public void matchAnnotatedClassOrMethod(Decorator decorator) {
    }

    @Around(value = "matchAnnotatedClassOrMethod(foundDecoratorAnnotation)", argNames = "proceedingJoinPoint, foundDecoratorAnnotation")
    public Object decorateAroundAdvice(ProceedingJoinPoint proceedingJoinPoint, @Nullable Decorator foundDecoratorAnnotation) throws Throwable {
        ProceedingJoinPointHelper joinPointHelper = ProceedingJoinPointHelper.prepareFor(proceedingJoinPoint);
        List<Decorator> decoratorAnnotations = joinPointHelper.getMethodAnnotations(Decorator.class);
        if (decoratorAnnotations == null) {
            decoratorAnnotations = joinPointHelper.getClassAnnotations(Decorator.class);
        }
        if (decoratorAnnotations == null) { //because annotations wasn't found
            return proceedingJoinPoint.proceed();
        }
        Collections.reverse(decoratorAnnotations);
        for (Decorator decoratorAnnotation : decoratorAnnotations) {
            List<Retry> retryAnnotations = Arrays.asList(decoratorAnnotation.retry());
            Collections.reverse(retryAnnotations);
            for (Retry retryAnnotation : retryAnnotations) {
                retryAspectHelper.decorate(joinPointHelper, retryAnnotation);
            }
            List<RateLimiter> rateLimiterAnnotations = Arrays.asList(decoratorAnnotation.rateLimiter());
            Collections.reverse(rateLimiterAnnotations);
            for (RateLimiter rateLimiterAnnotation : rateLimiterAnnotations) {
                rateLimiterAspectHelper.decorate(joinPointHelper, rateLimiterAnnotation);
            }
            List<Bulkhead> bulkheadAnnotations = Arrays.asList(decoratorAnnotation.bulkhead());
            Collections.reverse(bulkheadAnnotations);
            for (Bulkhead bulkheadAnnotation : bulkheadAnnotations) {
                bulkheadAspectHelper.decorate(joinPointHelper, bulkheadAnnotation);
            }
            List<CircuitBreaker> circuitBreakerAnnotations = Arrays.asList(decoratorAnnotation.circuitBreaker());
            Collections.reverse(circuitBreakerAnnotations);
            for (CircuitBreaker circuitBreakerAnnotation : circuitBreakerAnnotations) {
                circuitBreakerAspectHelper.decorate(joinPointHelper, circuitBreakerAnnotation);
            }
        }
        return joinPointHelper.getDecoratedProceedCall().apply();
    }

    @Override
    public int getOrder() {
        return order;
    }
}
