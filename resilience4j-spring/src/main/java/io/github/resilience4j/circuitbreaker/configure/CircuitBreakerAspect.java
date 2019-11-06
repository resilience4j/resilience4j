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

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.utils.ProceedingJoinPointHelper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;

/**
 * This Spring AOP aspect intercepts all methods which are annotated with a
 * {@link CircuitBreaker} annotation. The aspect will handle methods that return
 * a RxJava2 reactive type, Spring Reactor reactive type, CompletionStage type,
 * or value type.
 *
 * The CircuitBreakerRegistry is used to retrieve an instance of a
 * CircuitBreaker for a specific name.
 *
 * Given a method like this:
 * <pre><code>
 *     {@literal @}CircuitBreaker(name = "myService")
 *     public String fancyName(String name) {
 *         return "Sir Captain " + name;
 *     }
 * </code></pre> each time the {@code #fancyName(String)} method is invoked, the
 * method's execution will pass through a a
 * {@link io.github.resilience4j.circuitbreaker.CircuitBreaker} according to the
 * given config.
 *
 * The fallbackMethod parameter signature must match either:
 *
 * 1) The method parameter signature on the annotated method or 2) The method
 * parameter signature with a matching exception type as the last parameter on
 * the annotated method
 */
@Aspect
public class CircuitBreakerAspect implements Ordered {

    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerAspect.class);

    private final CircuitBreakerAspectHelper circuitBreakerAspectHelper;
    private final CircuitBreakerConfigurationProperties circuitBreakerProperties;

    public CircuitBreakerAspect(CircuitBreakerAspectHelper circuitBreakerAspectHelper, CircuitBreakerConfigurationProperties circuitBreakerProperties) {
        this.circuitBreakerAspectHelper = circuitBreakerAspectHelper;
        this.circuitBreakerProperties = circuitBreakerProperties;
    }

    @Pointcut(value = "@within(circuitBreaker) || @annotation(circuitBreaker)", argNames = "circuitBreaker")
    public void matchAnnotatedClassOrMethod(CircuitBreaker circuitBreaker) {
    }

    @Around(value = "matchAnnotatedClassOrMethod(circuitBreakerAnnotation)", argNames = "proceedingJoinPoint, circuitBreakerAnnotation")
    public Object circuitBreakerAroundAdvice(ProceedingJoinPoint proceedingJoinPoint, @Nullable CircuitBreaker circuitBreakerAnnotation) throws Throwable {
        ProceedingJoinPointHelper joinPointHelper = new ProceedingJoinPointHelper(proceedingJoinPoint);
        if (circuitBreakerAnnotation == null) {
            circuitBreakerAnnotation = joinPointHelper.getAnnotation(CircuitBreaker.class);
        }
        if (circuitBreakerAnnotation == null) { //because annotations wasn't found
            return proceedingJoinPoint.proceed();
        }
        circuitBreakerAspectHelper.decorate(joinPointHelper, circuitBreakerAnnotation);
        return joinPointHelper.getDecoratedProceedCall().apply();
    }

    @Override
    public int getOrder() {
        return circuitBreakerProperties.getCircuitBreakerAspectOrder();
    }
}
