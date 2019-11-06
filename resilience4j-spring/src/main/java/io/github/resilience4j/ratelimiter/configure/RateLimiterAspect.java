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

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.Ordered;

import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.utils.ProceedingJoinPointHelper;

/**
 * This Spring AOP aspect intercepts all methods which are annotated with a
 * {@link RateLimiter} annotation. The aspect will handle methods that return a
 * RxJava2 reactive type, Spring Reactor reactive type, CompletionStage type, or
 * value type.
 *
 * The RateLimiterRegistry is used to retrieve an instance of a RateLimiter for
 * a specific backend.
 *
 * Given a method like this:
 * <pre><code>
 *     {@literal @}RateLimiter(name = "myService")
 *     public String fancyName(String name) {
 *         return "Sir Captain " + name;
 *     }
 * </code></pre> each time the {@code #fancyName(String)} method is invoked, the
 * method's execution will pass through a a
 * {@link io.github.resilience4j.ratelimiter.RateLimiter} according to the given
 * config.
 *
 * The fallbackMethod parameter signature must match either:
 *
 * 1) The method parameter signature on the annotated method or 2) The method
 * parameter signature with a matching exception type as the last parameter on
 * the annotated method
 */
@Aspect
public class RateLimiterAspect implements Ordered {

    private final RateLimiterAspectHelper rateLimiterAspectHelper;
    private final RateLimiterConfigurationProperties properties;

    public RateLimiterAspect(RateLimiterAspectHelper rateLimiterAspectHelper, RateLimiterConfigurationProperties properties) {
        this.rateLimiterAspectHelper = rateLimiterAspectHelper;
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

    @Around(value = "matchAnnotatedClassOrMethod(rateLimiterAnnotation)", argNames = "proceedingJoinPoint, rateLimiterAnnotation")
    public Object rateLimiterAroundAdvice(ProceedingJoinPoint proceedingJoinPoint, @Nullable RateLimiter rateLimiterAnnotation) throws Throwable {
        ProceedingJoinPointHelper joinPointHelper = new ProceedingJoinPointHelper(proceedingJoinPoint);
        if (rateLimiterAnnotation == null) {
            rateLimiterAnnotation = joinPointHelper.getAnnotation(RateLimiter.class);
        }
        if (rateLimiterAnnotation == null) { //because annotations wasn't found
            return proceedingJoinPoint.proceed();
        }
        rateLimiterAspectHelper.decorate(joinPointHelper, rateLimiterAnnotation);
        return joinPointHelper.getDecoratedProceedCall().apply();
    }

    @Override
    public int getOrder() {
        return properties.getRateLimiterAspectOrder();
    }
}
