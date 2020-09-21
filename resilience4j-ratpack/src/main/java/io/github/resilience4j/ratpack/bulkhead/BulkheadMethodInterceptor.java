/*
 * Copyright 2017 Jan Sykora
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
package io.github.resilience4j.ratpack.bulkhead;

import com.google.inject.Inject;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.ratpack.internal.AbstractMethodInterceptor;
import io.github.resilience4j.ratpack.recovery.DefaultRecoveryFunction;
import io.github.resilience4j.ratpack.recovery.RecoveryFunction;
import io.github.resilience4j.reactor.bulkhead.operator.BulkheadOperator;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import ratpack.exec.Promise;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * A {@link MethodInterceptor} to handle all methods annotated with {@link Bulkhead}. It will handle
 * methods that return a {@link Promise}, {@link reactor.core.publisher.Flux}, {@link
 * reactor.core.publisher.Mono}, {@link java.util.concurrent.CompletionStage}, or value.
 * <p>
 * Given a method like this:
 * <pre><code>
 *     {@literal @}Bulkhead(name = "myService")
 *     public String fancyName(String name) {
 *         return "Sir Captain " + name;
 *     }
 * </code></pre>
 * each time the {@code #fancyName(String)} method is invoked, the method's execution will pass
 * through a a {@link io.github.resilience4j.bulkhead.Bulkhead} (concurrent limiting) according to
 * the given policy.
 * <p>
 * The fallbackMethod parameter signature must match either:
 * <p>
 * 1) The method parameter signature on the annotated method or 2) The method parameter signature
 * with a matching exception type as the last parameter on the annotated method
 * <p>
 * The return value can be a {@link Promise}, {@link java.util.concurrent.CompletionStage}, {@link
 * reactor.core.publisher.Flux}, {@link reactor.core.publisher.Mono}, or an object value. Other
 * reactive types are not supported.
 * <p>
 * If the return value is one of the reactive types listed above, it must match the return value
 * type of the annotated method.
 */

public class BulkheadMethodInterceptor extends AbstractMethodInterceptor {

    @Inject(optional = true)
    @Nullable
    private BulkheadRegistry registry;

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Bulkhead annotation = invocation.getMethod().getAnnotation(Bulkhead.class);
        if (annotation == null) {
            annotation = invocation.getMethod().getDeclaringClass().getAnnotation(Bulkhead.class);
        }
        final RecoveryFunction<?> fallbackMethod = Optional
            .ofNullable(createRecoveryFunction(invocation, annotation.fallbackMethod()))
            .orElse(new DefaultRecoveryFunction<>());
        if (registry == null) {
            registry = BulkheadRegistry.ofDefaults();
        }
        io.github.resilience4j.bulkhead.Bulkhead bulkhead = registry.bulkhead(annotation.name());
        Class<?> returnType = invocation.getMethod().getReturnType();
        if (Promise.class.isAssignableFrom(returnType)) {
            Promise<?> result = (Promise<?>) proceed(invocation);
            if (result != null) {
                BulkheadTransformer transformer = BulkheadTransformer.of(bulkhead)
                    .recover(fallbackMethod);
                result = result.transform(transformer);
            }
            return result;
        } else if (Flux.class.isAssignableFrom(returnType)) {
            Flux<?> result = (Flux<?>) proceed(invocation);
            if (result != null) {
                BulkheadOperator operator = BulkheadOperator.of(bulkhead);
                result = fallbackMethod.onErrorResume(result.transform(operator));
            }
            return result;
        } else if (Mono.class.isAssignableFrom(returnType)) {
            Mono<?> result = (Mono<?>) proceed(invocation);
            if (result != null) {
                BulkheadOperator operator = BulkheadOperator.of(bulkhead);
                result = fallbackMethod.onErrorResume(result.transform(operator));
            }
            return result;
        } else if (CompletionStage.class.isAssignableFrom(returnType)) {
            final CompletableFuture promise = new CompletableFuture<>();
            if (bulkhead.tryAcquirePermission()) {
                CompletionStage<?> result = (CompletionStage<?>) proceed(invocation);
                if (result != null) {
                    result.whenComplete((value, throwable) -> {
                        bulkhead.onComplete();
                        if (throwable != null) {
                            completeFailedFuture(throwable, fallbackMethod, promise);
                        } else {
                            promise.complete(value);
                        }
                    });
                }
            } else {
                Throwable t = BulkheadFullException.createBulkheadFullException(bulkhead);
                completeFailedFuture(t, fallbackMethod, promise);
            }
            return promise;
        } else {
            return handleProceedWithException(invocation, bulkhead, fallbackMethod);
        }
    }


    @Nullable
    private Object handleProceedWithException(MethodInvocation invocation,
        io.github.resilience4j.bulkhead.Bulkhead bulkhead, RecoveryFunction<?> recoveryFunction)
        throws Throwable {
        try {
            return io.github.resilience4j.bulkhead.Bulkhead
                .decorateCheckedSupplier(bulkhead, invocation::proceed).get();
        } catch (Throwable throwable) {
            return recoveryFunction.apply(throwable);
        }
    }

}
