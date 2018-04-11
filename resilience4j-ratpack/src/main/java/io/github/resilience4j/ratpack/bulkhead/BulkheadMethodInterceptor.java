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
import io.github.resilience4j.ratpack.recovery.RecoveryFunction;
import io.github.resilience4j.reactor.bulkhead.operator.BulkheadOperator;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import ratpack.exec.Promise;
import ratpack.util.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * A {@link MethodInterceptor} to handle all methods annotated with {@link Bulkhead}. It will
 * handle methods that return a Promise only. It will add a transform to the promise with the bulkhead and
 * fallback found in the annotation.
 */
public class BulkheadMethodInterceptor implements MethodInterceptor {

    @Inject(optional = true)
    private BulkheadRegistry registry;

    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Bulkhead annotation = invocation.getMethod().getAnnotation(Bulkhead.class);
        RecoveryFunction<?> recoveryFunction = annotation.recovery().newInstance();
        if (registry == null) {
            registry = BulkheadRegistry.ofDefaults();
        }
        io.github.resilience4j.bulkhead.Bulkhead bulkhead = registry.bulkhead(annotation.name());
        if (bulkhead == null) {
            return invocation.proceed();
        }
        Class<?> returnType = invocation.getMethod().getReturnType();
        if (Promise.class.isAssignableFrom(returnType)) {
            Promise<?> result = (Promise<?>) invocation.proceed();
            if (result != null) {
                BulkheadTransformer transformer = BulkheadTransformer.of(bulkhead).recover(recoveryFunction);
                result = result.transform(transformer);
            }
            return result;
        } else if (Flux.class.isAssignableFrom(returnType)) {
            Flux<?> result = (Flux<?>) invocation.proceed();
            if (result != null) {
                BulkheadOperator operator = BulkheadOperator.of(bulkhead, Schedulers.immediate());
                result = recoveryFunction.onErrorResume(result.transform(operator));
            }
            return result;
        } else if (Mono.class.isAssignableFrom(returnType)) {
            Mono<?> result = (Mono<?>) invocation.proceed();
            if (result != null) {
                BulkheadOperator operator = BulkheadOperator.of(bulkhead, Schedulers.immediate());
                result = recoveryFunction.onErrorResume(result.transform(operator));
            }
            return result;
        } else if (CompletionStage.class.isAssignableFrom(returnType)) {
            if (bulkhead.isCallPermitted()) {
                return ((CompletionStage<?>) invocation.proceed()).handle((o, throwable) -> {
                    bulkhead.onComplete();
                    if (throwable != null) {
                        try {
                            return recoveryFunction.apply(throwable);
                        } catch (Exception e) {
                            throw Exceptions.uncheck(throwable);
                        }
                    } else {
                        return o;
                    }
                });
            } else {
                final CompletableFuture promise = new CompletableFuture<>();
                Throwable t = new BulkheadFullException(String.format("Bulkhead '%s' is full", bulkhead.getName()));
                try {
                    promise.complete(recoveryFunction.apply(t));
                } catch (Throwable t2) {
                    promise.completeExceptionally(t2);
                }
                return promise;
            }
        }
        return handleOther(invocation, bulkhead, recoveryFunction);
    }

    private Object handleOther(MethodInvocation invocation, io.github.resilience4j.bulkhead.Bulkhead bulkhead, RecoveryFunction<?> recoveryFunction) throws Throwable {
        boolean permission = bulkhead.isCallPermitted();

        if (!permission) {
            Throwable t = new BulkheadFullException(String.format("Bulkhead '%s' is full", bulkhead.getName()));
            return recoveryFunction.apply(t);
        }

        try {
            if (Thread.interrupted()) {
                throw new IllegalStateException("Thread was interrupted during permission wait");
            }

            return invocation.proceed();
        } catch (Exception e) {
            return recoveryFunction.apply(e);
        } finally {
            bulkhead.onComplete();
        }
    }
}
