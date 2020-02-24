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
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreakers;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.fallback.FallbackDecorators;
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
 * This Spring AOP aspect intercepts all methods which are annotated with a {@link CircuitBreaker}
 * annotation. The aspect will decorate methods that return a RxJava2 reactive type, Spring Reactor
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
public class CircuitBreakerAspect implements EmbeddedValueResolverAware, Ordered {

    private final CircuitBreakerConfigurationProperties circuitBreakerProperties;
    private final CircuitBreakerDecorator circuitBreakerDecorator;

    public CircuitBreakerAspect(CircuitBreakerConfigurationProperties circuitBreakerProperties,
        CircuitBreakerRegistry circuitBreakerRegistry,
        @Autowired(required = false) List<CircuitBreakerDecoratorExt> circuitBreakerDecoratorExtList,
        FallbackDecorators fallbackDecorators) {
        this.circuitBreakerProperties = circuitBreakerProperties;
        this.circuitBreakerDecorator = new
            CircuitBreakerDecorator(circuitBreakerRegistry, circuitBreakerDecoratorExtList, fallbackDecorators);
    }

    /**
     * Method used as pointcut
     *
     * @param circuitBreakers - matched annotation
     */
    @Pointcut(value = "@within(circuitBreakers) || @annotation(circuitBreakers)", argNames = "circuitBreakers")
    public void matchRepeatedAnnotatedClassOrMethod(CircuitBreakers circuitBreakers) {
        // Method used as pointcut
    }


    @Pointcut(value = "@within(circuitBreaker) || @annotation(circuitBreaker)", argNames = "circuitBreaker")
    public void matchAnnotatedClassOrMethod(CircuitBreaker circuitBreaker) {
    }

    @Around(
        value = "matchRepeatedAnnotatedClassOrMethod(circuitBreakers)",
        argNames = "proceedingJoinPoint, circuitBreakers")
    public Object repeatedCircuitBreakerAroundAdvice(
        ProceedingJoinPoint proceedingJoinPoint,
        @Nullable CircuitBreakers circuitBreakers) throws Throwable {
        return proceed(proceedingJoinPoint);
    }


    @Around(value = "matchAnnotatedClassOrMethod(circuitBreakerAnnotation)", argNames = "proceedingJoinPoint, circuitBreakerAnnotation")
    public Object circuitBreakerAroundAdvice(ProceedingJoinPoint proceedingJoinPoint,
        @Nullable CircuitBreaker circuitBreakerAnnotation) throws Throwable {

        return proceed(proceedingJoinPoint);
    }

    public Object proceed(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        ProceedingJoinPointWrapper joinPointWrapper = new ProceedingJoinPointWrapper(proceedingJoinPoint);

        // Find method or class annotations.
        // Method annotations override class annotations.
        Set<CircuitBreaker> annotations = joinPointWrapper.findRepeatableAnnotations(CircuitBreaker.class);
        if (!annotations.isEmpty()) {
            for (CircuitBreaker annotation : annotations) {
                joinPointWrapper = circuitBreakerDecorator.decorate(joinPointWrapper, annotation);
            }
        }

        return joinPointWrapper.proceed();
    }

    @Override
    public int getOrder() {
        return circuitBreakerProperties.getCircuitBreakerAspectOrder();
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        circuitBreakerDecorator.setEmbeddedValueResolver(resolver);
    }
}
