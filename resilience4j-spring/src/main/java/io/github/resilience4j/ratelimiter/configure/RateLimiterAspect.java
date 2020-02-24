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
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.ratelimiter.annotation.RateLimiters;
import io.github.resilience4j.utils.ProceedingJoinPointWrapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.Ordered;
import org.springframework.util.StringValueResolver;

import java.util.List;
import java.util.Set;

/**
 * This Spring AOP aspect intercepts all methods which are annotated with a {@link RateLimiter}
 * annotation. The aspect will decorate methods that return a RxJava2 reactive type, Spring Reactor
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
public class RateLimiterAspect implements EmbeddedValueResolverAware, Ordered {

    private final RateLimiterConfigurationProperties properties;
    private final RateLimiterDecorator rateLimiterDecorator;

    public RateLimiterAspect(RateLimiterRegistry rateLimiterRegistry,
        RateLimiterConfigurationProperties properties,
        @Autowired(required = false) List<RateLimiterDecoratorExt> rateLimiterAspectExtList,
        FallbackDecorators fallbackDecorators) {
        this.properties = properties;
        this.rateLimiterDecorator = new
            RateLimiterDecorator(rateLimiterRegistry, rateLimiterAspectExtList, fallbackDecorators);
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

    /**
     * Method used as pointcut
     *
     * @param rateLimiters - matched annotation
     */
    @Pointcut(value = "@within(rateLimiters) || @annotation(rateLimiters)", argNames = "rateLimiters")
    public void matchRepeatedAnnotatedClassOrMethod(RateLimiters rateLimiters) {
        // Method used as pointcut
    }

    @Around(
        value = "matchRepeatedAnnotatedClassOrMethod(rateLimiters)",
        argNames = "proceedingJoinPoint, rateLimiters")
    public Object repeatedRateLimiterAroundAdvice(
        ProceedingJoinPoint proceedingJoinPoint,
        @Nullable RateLimiters rateLimiters) throws Throwable {
        return proceed(proceedingJoinPoint);
    }

    public Object proceed(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        ProceedingJoinPointWrapper joinPointWrapper = new ProceedingJoinPointWrapper(proceedingJoinPoint);

        // Find method or class annotations.
        // Method annotations override class annotations.
        Set<RateLimiter> rateLimiterAnnotations = joinPointWrapper.findRepeatableAnnotations(RateLimiter.class);
        if (!rateLimiterAnnotations.isEmpty()) {
            for (RateLimiter rateLimiterAnnotation : rateLimiterAnnotations) {
                joinPointWrapper = rateLimiterDecorator.decorate(joinPointWrapper, rateLimiterAnnotation);
            }
        }

        return joinPointWrapper.proceed();
    }

    @Around(value = "matchAnnotatedClassOrMethod(rateLimiterAnnotation)", argNames = "proceedingJoinPoint, rateLimiterAnnotation")
    public Object rateLimiterAroundAdvice(ProceedingJoinPoint proceedingJoinPoint,
        @Nullable RateLimiter annotation) throws Throwable {
        return proceed(proceedingJoinPoint);
    }

    @Override
    public int getOrder() {
        return properties.getRateLimiterAspectOrder();
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        rateLimiterDecorator.setEmbeddedValueResolver(resolver);
    }
}
