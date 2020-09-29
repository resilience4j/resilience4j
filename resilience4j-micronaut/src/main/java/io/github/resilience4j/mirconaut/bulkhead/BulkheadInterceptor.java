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

package io.github.resilience4j.mirconaut.bulkhead;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.operator.BulkheadOperator;
import io.github.resilience4j.mirconaut.BaseInterceptor;
import io.github.resilience4j.mirconaut.ResilienceInterceptPhase;
import io.github.resilience4j.mirconaut.fallback.UnhandledFallbackException;
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
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

/**
 * A {@link MethodInterceptor} that intercepts all method calls which are annotated with a {@link io.github.resilience4j.mirconaut.annotation.Bulkhead}
 * annotation.
 **/
@Singleton
@Requires(beans = BulkheadRegistry.class)
public class BulkheadInterceptor extends BaseInterceptor implements MethodInterceptor<Object,Object> {

    private final BulkheadRegistry bulkheadRegistry;
    private final BeanContext beanContext;

    /**
     * @param beanContext      The bean context to allow for DI of class annotated with {@link javax.inject.Inject}.
     * @param bulkheadRegistry bulkhead registry used to retrieve {@link Bulkhead} by name
     */
    public BulkheadInterceptor(BeanContext beanContext,
                               BulkheadRegistry bulkheadRegistry) {
        this.bulkheadRegistry = bulkheadRegistry;
        this.beanContext = beanContext;
    }

    @Override
    public int getOrder() {
        return ResilienceInterceptPhase.BULKHEAD.getPosition();
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
        final String fallbackMethod = executableMethod.stringValue(io.github.resilience4j.mirconaut.annotation.Bulkhead.class, "fallbackMethod").orElse("");
        Class<?> declaringType = context.getDeclaringType();
        return beanContext.findExecutionHandle(declaringType, fallbackMethod, context.getArgumentTypes());
    }

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {

        Optional<AnnotationValue<io.github.resilience4j.mirconaut.annotation.Bulkhead>> opt = context.findAnnotation(io.github.resilience4j.mirconaut.annotation.Bulkhead.class);
        if (!opt.isPresent()) {
            return context.proceed();
        }
        final io.github.resilience4j.mirconaut.annotation.Bulkhead.Type type = opt.get().enumValue("type", io.github.resilience4j.mirconaut.annotation.Bulkhead.Type.class).orElse(io.github.resilience4j.mirconaut.annotation.Bulkhead.Type.SEMAPHORE);
        if (type != io.github.resilience4j.mirconaut.annotation.Bulkhead.Type.SEMAPHORE) {
            return context.proceed();
        }

        final String name = opt.get().stringValue("name").orElse("default");
        Bulkhead bulkhead = this.bulkheadRegistry.bulkhead(name);
        ReturnType<Object> rt = context.getReturnType();
        Class<Object> returnType = rt.getType();
        if (CompletionStage.class.isAssignableFrom(returnType)) {
            return this.fallbackCompletable(bulkhead.executeCompletionStage(() -> {
                try {
                    return ((CompletableFuture<?>) context.proceed());
                } catch (Throwable e) {
                    throw new CompletionException(e);
                }
            }), context);
        } else if (Publishers.isConvertibleToPublisher(returnType)) {
            Object result = context.proceed();
            if (result == null) {
                return result;
            }
            Flowable<Object> flowable = ConversionService.SHARED
                .convert(result, Flowable.class)
                .orElseThrow(() -> new UnhandledFallbackException("Unsupported Reactive type: " + result));

            flowable = this.fallbackFlowable(flowable.compose(BulkheadOperator.of(bulkhead)), context);
            return ConversionService.SHARED
                .convert(flowable, context.getReturnType().asArgument())
                .orElseThrow(() -> new UnhandledFallbackException("Unsupported Reactive type: " + result));
        }
        try {
            return bulkhead.executeSupplier(context::proceed);
        } catch (RuntimeException exception) {
            return this.fallback(context, exception);
        } catch (Throwable throwable) {
            throw new UnhandledFallbackException("Error invoking fallback for type [" + context.getTarget().getClass().getName() + "]: " + throwable.getMessage(), throwable);
        }
    }
}
