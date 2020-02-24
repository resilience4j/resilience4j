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
package io.github.resilience4j.retry.configure;

import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.fallback.FallbackDecorators;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.annotation.Retries;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.utils.ProceedingJoinPointWrapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.Ordered;
import org.springframework.util.StringValueResolver;

import java.util.List;
import java.util.Set;

/**
 * This Spring AOP aspect intercepts all methods which are annotated with a {@link Retry}
 * annotation. The aspect will decorate methods that return a RxJava2 reactive type, Spring Reactor
 * reactive type, CompletionStage type, or value type.
 * <p>
 * The RetryRegistry is used to retrieve an instance of a Retry for a specific name.
 * <p>
 * Given a method like this:
 * <pre><code>
 *     {@literal @}Retry(name = "myService")
 *     public String fancyName(String name) {
 *         return "Sir Captain " + name;
 *     }
 * </code></pre>
 * each time the {@code #fancyName(String)} method is invoked, the method's execution will pass
 * through a a {@link io.github.resilience4j.retry.Retry} according to the given config.
 * <p>
 * The fallbackMethod parameter signature must match either:
 * <p>
 * 1) The method parameter signature on the annotated method or 2) The method parameter signature
 * with a matching exception type as the last parameter on the annotated method
 */
@Aspect
public class RetryAspect implements EmbeddedValueResolverAware, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(RetryAspect.class);
    private final RetryConfigurationProperties retryConfigurationProperties;
    private final RetryDecorator retryDecorator;

    /**
     * @param retryConfigurationProperties spring retry config properties
     * @param retryRegistry                retry definition registry
     * @param retryAspectExtList           a list of retry aspect extensions
     * @param fallbackDecorators           the fallback decorators
     */
    public RetryAspect(RetryConfigurationProperties retryConfigurationProperties,
        RetryRegistry retryRegistry,
        @Autowired(required = false) List<RetryDecoratorExt> retryAspectExtList,
        FallbackDecorators fallbackDecorators) {
        this.retryConfigurationProperties = retryConfigurationProperties;
        this.retryDecorator = new
            RetryDecorator(retryRegistry, retryAspectExtList, fallbackDecorators);

    }

    /**
     * Method used as pointcut
     *
     * @param retries - matched annotation
     */
    @Pointcut(value = "@within(retries) || @annotation(retries)", argNames = "retries")
    public void matchRepeatedAnnotatedClassOrMethod(Retries retries) {
        // Method used as pointcut
    }

    @Pointcut(value = "@within(retry) || @annotation(retry)", argNames = "retry")
    public void matchAnnotatedClassOrMethod(Retry retry) {
    }

    @Around(
        value = "matchRepeatedAnnotatedClassOrMethod(retries)",
        argNames = "proceedingJoinPoint, retries")
    public Object repeatedRetryAroundAdvice(
        ProceedingJoinPoint proceedingJoinPoint,
        @Nullable Retries retries) throws Throwable {

        return proceed(proceedingJoinPoint);
    }

    @Around(value = "matchAnnotatedClassOrMethod(retryAnnotation)", argNames = "proceedingJoinPoint, retryAnnotation")
    public Object retryAroundAdvice(ProceedingJoinPoint proceedingJoinPoint,
        @Nullable Retry retryAnnotation) throws Throwable {

        return proceed(proceedingJoinPoint);
    }

    public Object proceed(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        ProceedingJoinPointWrapper joinPointWrapper = new ProceedingJoinPointWrapper(proceedingJoinPoint);

        // Find method or class annotations.
        // Method annotations override class annotations.
        Set<Retry> retryAnnotations = joinPointWrapper.findRepeatableAnnotations(Retry.class);
        if (!retryAnnotations.isEmpty()) {
            for (Retry retryAnnotation : retryAnnotations) {
                joinPointWrapper = retryDecorator.decorate(joinPointWrapper, retryAnnotation);
            }
        }

        return joinPointWrapper.proceed();
    }


    @Override
    public int getOrder() {
        return retryConfigurationProperties.getRetryAspectOrder();
    }


    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        retryDecorator.setEmbeddedValueResolver(resolver);
    }
}
