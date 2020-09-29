/*
 * Copyright 2019 Michael Pollind
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
package io.github.resilience4j.mirconaut.timelimiter;

import io.github.resilience4j.mirconaut.BaseInterceptor;
import io.github.resilience4j.mirconaut.ResilienceInterceptPhase;
import io.github.resilience4j.mirconaut.fallback.UnhandledFallbackException;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.github.resilience4j.timelimiter.transformer.TimeLimiterTransformer;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.ReturnType;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.inject.MethodExecutionHandle;
import io.reactivex.Flowable;

import javax.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Singleton
@Requires(beans = TimeLimiterRegistry.class)
public class TimeLimiterInterceptor extends BaseInterceptor implements MethodInterceptor<Object,Object> {

    private final TimeLimiterRegistry timeLimiterRegistry;
    private final BeanContext beanContext;
    private static final ScheduledExecutorService timeLimiterExecutorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    public TimeLimiterInterceptor(BeanContext beanContext, TimeLimiterRegistry timeLimiterRegistry) {
        this.beanContext = beanContext;
        this.timeLimiterRegistry = timeLimiterRegistry;
    }

    @Override
    public int getOrder() {
        return ResilienceInterceptPhase.TIME_LIMITER.getPosition();
    }

    /**
     * Finds a fallback method for the given context.
     *
     * @param context The context
     * @return The fallback method if it is present
     */
    @Override
    public Optional<? extends MethodExecutionHandle<?, Object>> findFallbackMethod(MethodInvocationContext<Object, Object> context) {
        ExecutableMethod executableMethod = context.getExecutableMethod();
        final String fallbackMethod = executableMethod.stringValue(io.github.resilience4j.mirconaut.annotation.TimeLimiter.class, "fallbackMethod").orElse("");
        Class<?> declaringType = context.getDeclaringType();
        return beanContext.findExecutionHandle(declaringType, fallbackMethod, context.getArgumentTypes());
    }

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        Optional<AnnotationValue<io.github.resilience4j.mirconaut.annotation.TimeLimiter>> opt = context.findAnnotation(io.github.resilience4j.mirconaut.annotation.TimeLimiter.class);
        if (!opt.isPresent()) {
            return context.proceed();
        }

        ExecutableMethod executableMethod = context.getExecutableMethod();
        final String name = executableMethod.stringValue(io.github.resilience4j.mirconaut.annotation.TimeLimiter.class).orElse("default");
        TimeLimiter timeLimiter = this.timeLimiterRegistry.timeLimiter(name);

        ReturnType<Object> rt = context.getReturnType();
        Class<Object> returnType = rt.getType();

        if (CompletionStage.class.isAssignableFrom(returnType)) {
            return this.fallbackCompletable(timeLimiter.executeCompletionStage(timeLimiterExecutorService, () -> ((CompletableFuture<?>) context.proceed())), context);
        } else if (Publishers.isConvertibleToPublisher(returnType)) {
            Object result = context.proceed();
            if (result == null) {
                return result;
            }
            Flowable<Object> flowable = ConversionService.SHARED
                .convert(result, Flowable.class)
                .orElseThrow(() -> new UnhandledFallbackException("Unsupported Reactive type: " + result));

            flowable = this.fallbackFlowable(flowable.compose(TimeLimiterTransformer.of(timeLimiter)), context);

            return ConversionService.SHARED
                .convert(flowable, context.getReturnType().asArgument())
                .orElseThrow(() -> new UnhandledFallbackException("Unsupported Reactive type: " + result));
        }
        try {
            return timeLimiter.executeFutureSupplier(
                () -> CompletableFuture.supplyAsync(context::proceed));
        } catch (RuntimeException exception) {
            return this.fallback(context, exception);
        } catch (Throwable throwable) {
            throw new UnhandledFallbackException("Error invoking fallback for type [" + context.getTarget().getClass().getName() + "]: " + throwable.getMessage(), throwable);
        }
    }
}
