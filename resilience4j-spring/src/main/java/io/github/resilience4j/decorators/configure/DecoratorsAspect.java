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

import io.github.resilience4j.retry.configure.*;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.decorators.annotation.Decorator;
import io.github.resilience4j.decorators.annotation.Decorators;
import io.github.resilience4j.fallback.FallbackDecorators;
import io.github.resilience4j.fallback.FallbackMethod;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.utils.ProceedingJoinPointHelper;
import io.github.resilience4j.utils.AnnotationExtractor;
import java.util.Arrays;
import java.util.Collections;

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
 *     {@literal @}Retry(name = "myService")
 *     public String fancyName(String name) {
 *         return "Sir Captain " + name;
 *     }
 * </code></pre> each time the {@code #fancyName(String)} method is invoked, the
 * method's execution will pass through a a
 * {@link io.github.resilience4j.retry.Retry} according to the given config.
 *
 * The fallbackMethod parameter signature must match either:
 *
 * 1) The method parameter signature on the annotated method or 2) The method
 * parameter signature with a matching exception type as the last parameter on
 * the annotated method
 */
//@Aspect
public class DecoratorsAspect /*implements Ordered*/ {
/*
    private static final Logger logger = LoggerFactory.getLogger(DecoratorsAspect.class);
    private final RetryAspectHelper retryAspectHelper;

    @Pointcut(value = "@within(decorators) || @annotation(decorators)", argNames = "decorators")
    public void matchAnnotatedClassOrMethod(Decorators decorators) {
    }

    @Around(value = "matchAnnotatedClassOrMethod(decoratorsAnnotation)", argNames = "proceedingJoinPoint, decoratorsAnnotation")
    public Object decorateAroundAdvice(ProceedingJoinPoint proceedingJoinPoint, @Nullable Decorators decoratorsAnnotation) throws Throwable {
        ProceedingJoinPointHelper joinPointHelper = new ProceedingJoinPointHelper(proceedingJoinPoint);
        if (decoratorsAnnotation == null) {
            decoratorsAnnotation = joinPointHelper.getAnnotation(Decorators.class);
        }
        if (decoratorsAnnotation == null || decoratorsAnnotation.value().length == 0) { //because annotations wasn't found
            return proceedingJoinPoint.proceed();
        }
        for (Decorator decoratorAnnotation : Collections.reverse(Arrays.asList(decoratorsAnnotation.value()))) {
            for (Retry retryAnnotation : Collections.reverse(Arrays.asList(decoratorAnnotation.retry()))) {
                retryAspectHelper.decorate(joinPointHelper, retryAnnotation)
            }
        }
    }*/
}
