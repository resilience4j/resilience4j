/*
 * Copyright 2023 Mariusz Kopylec
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

package io.github.resilience4j.spring6.micrometer.configure;

import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.micrometer.TimerConfig;
import io.github.resilience4j.micrometer.TimerRegistry;
import io.github.resilience4j.micrometer.annotation.Timer;
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
import org.springframework.core.Ordered;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

@Aspect
public class TimerAspect implements Ordered {

    private static final Logger logger = LoggerFactory.getLogger(TimerAspect.class);

    private final TimerRegistry timerRegistry;
    private final TimerConfigurationProperties properties;
    @Nullable
    private final List<TimerAspectExt> timerAspectExtList;
    private final FallbackExecutor fallbackExecutor;
    private final SpelResolver spelResolver;

    public TimerAspect(
            TimerRegistry timerRegistry,
            TimerConfigurationProperties properties,
            @Nullable List<TimerAspectExt> timerAspectExtList,
            FallbackExecutor fallbackExecutor,
            SpelResolver spelResolver
    ) {
        this.timerRegistry = timerRegistry;
        this.properties = properties;
        this.timerAspectExtList = timerAspectExtList;
        this.fallbackExecutor = fallbackExecutor;
        this.spelResolver = spelResolver;
    }

    @Pointcut(value = "@within(timer) || @annotation(timer)", argNames = "timer")
    public void matchAnnotatedClassOrMethod(Timer timer) {
        // a marker method
    }

    @Around(value = "matchAnnotatedClassOrMethod(timerAnnotation)", argNames = "proceedingJoinPoint, timerAnnotation")
    public Object timerAroundAdvice(ProceedingJoinPoint proceedingJoinPoint, @Nullable Timer timerAnnotation) throws Throwable {
        Method method = ((MethodSignature) proceedingJoinPoint.getSignature()).getMethod();
        String methodName = method.getDeclaringClass().getName() + "#" + method.getName();
        if (timerAnnotation == null) {
            timerAnnotation = getTimerAnnotation(proceedingJoinPoint);
        }
        if (timerAnnotation == null) {
            return proceedingJoinPoint.proceed();
        }
        String name = spelResolver.resolve(method, proceedingJoinPoint.getArgs(), timerAnnotation.name());
        io.github.resilience4j.micrometer.Timer timer = getOrCreateTimer(methodName, name);
        Class<?> returnType = method.getReturnType();
        CheckedSupplier<Object> timerExecution = () -> proceed(proceedingJoinPoint, methodName, timer, returnType);
        return fallbackExecutor.execute(proceedingJoinPoint, method, timerAnnotation.fallbackMethod(), timerExecution);
    }

    private Object proceed(ProceedingJoinPoint proceedingJoinPoint, String methodName, io.github.resilience4j.micrometer.Timer timer, Class<?> returnType) throws Throwable {
        if (timerAspectExtList != null && !timerAspectExtList.isEmpty()) {
            for (TimerAspectExt timerAspectExt : timerAspectExtList) {
                if (timerAspectExt.canHandleReturnType(returnType)) {
                    return timerAspectExt.handle(proceedingJoinPoint, timer, methodName);
                }
            }
        }
        if (CompletionStage.class.isAssignableFrom(returnType)) {
            return handleJoinPointCompletableStage(proceedingJoinPoint, timer);
        }
        return handleDefaultJoinPoint(proceedingJoinPoint, timer);
    }

    private io.github.resilience4j.micrometer.Timer getOrCreateTimer(String methodName, String name) {
        TimerConfig config = timerRegistry.getConfiguration(name).orElse(timerRegistry.getDefaultConfig());
        io.github.resilience4j.micrometer.Timer timer = timerRegistry.timer(name, config);
        if (logger.isDebugEnabled()) {
            logger.debug("Created or retrieved timer '{}' for method: '{}'", name, methodName);
        }
        return timer;
    }

    @Nullable
    private Timer getTimerAnnotation(ProceedingJoinPoint proceedingJoinPoint) {
        if (proceedingJoinPoint.getTarget() instanceof Proxy) {
            logger.debug("The Timer annotation is kept on a interface which is acting as a proxy");
            return AnnotationExtractor.extractAnnotationFromProxy(proceedingJoinPoint.getTarget(), Timer.class);
        } else {
            return AnnotationExtractor.extract(proceedingJoinPoint.getTarget().getClass(), Timer.class);
        }
    }

    private Object handleJoinPointCompletableStage(ProceedingJoinPoint proceedingJoinPoint, io.github.resilience4j.micrometer.Timer timer) {
        return timer.executeCompletionStage(() -> {
            try {
                return (CompletionStage<?>) proceedingJoinPoint.proceed();
            } catch (Throwable throwable) {
                throw new CompletionException(throwable);
            }
        });
    }

    private Object handleDefaultJoinPoint(ProceedingJoinPoint proceedingJoinPoint, io.github.resilience4j.micrometer.Timer timer) throws Throwable {
        return timer.executeCheckedSupplier(proceedingJoinPoint::proceed);
    }

    @Override
    public int getOrder() {
        return properties.getTimerAspectOrder();
    }
}
