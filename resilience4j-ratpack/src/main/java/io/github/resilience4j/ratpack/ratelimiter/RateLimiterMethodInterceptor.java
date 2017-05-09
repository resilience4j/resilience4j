/*
 * Copyright 2017 Dan Maas
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

package io.github.resilience4j.ratpack.ratelimiter;

import com.google.inject.Inject;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.operator.RateLimiterOperator;
import io.github.resilience4j.ratpack.recovery.RecoveryFunction;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import ratpack.exec.Promise;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * A {@link MethodInterceptor} to handle all methods annotated with {@link RateLimiter}. It will
 * handle methods that return a Promise only. It will add a transform to the promise with the circuit breaker and
 * fallback found in the annotation.
 */
public class RateLimiterMethodInterceptor implements MethodInterceptor {

    @Inject(optional = true)
    private RateLimiterRegistry registry;

    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        RateLimiter annotation = invocation.getMethod().getAnnotation(RateLimiter.class);
        RecoveryFunction<?> recoveryFunction = annotation.recovery().newInstance();
        if (registry == null) {
            registry = RateLimiterRegistry.ofDefaults();
        }
        io.github.resilience4j.ratelimiter.RateLimiter rateLimiter = registry.rateLimiter(annotation.name());
        if (rateLimiter == null) {
            return invocation.proceed();
        }
        Class<?> returnType = invocation.getMethod().getReturnType();
        if (Promise.class.isAssignableFrom(returnType)) {
            Promise<?> result = (Promise<?>) proceed(invocation, rateLimiter, recoveryFunction);
            if (result != null) {
                RateLimiterTransformer transformer = RateLimiterTransformer.of(rateLimiter).recover(recoveryFunction);
                result = result.transform(transformer);
            }
            return result;
        } else if (Observable.class.isAssignableFrom(returnType)) {
            Observable<?> result = (Observable<?>) proceed(invocation, rateLimiter, recoveryFunction);
            if (result != null) {
                RateLimiterOperator operator = RateLimiterOperator.of(rateLimiter);
                result = result.lift(operator).onErrorReturn(t -> recoveryFunction.apply((Throwable) t));
            }
            return result;
        } else if (Flowable.class.isAssignableFrom(returnType)) {
            Flowable<?> result = (Flowable<?>) proceed(invocation, rateLimiter, recoveryFunction);
            if (result != null) {
                RateLimiterOperator operator = RateLimiterOperator.of(rateLimiter);
                result = result.lift(operator).onErrorReturn(t -> recoveryFunction.apply((Throwable) t));
            }
            return result;
        } else if (Single.class.isAssignableFrom(returnType)) {
            Single<?> result = (Single<?>) proceed(invocation, rateLimiter, recoveryFunction);
            if (result != null) {
                RateLimiterOperator operator = RateLimiterOperator.of(rateLimiter);
                result = result.lift(operator).onErrorReturn(t -> recoveryFunction.apply((Throwable) t));
            }
            return result;
        } else if (CompletionStage.class.isAssignableFrom(returnType)) {
            RateLimiterConfig rateLimiterConfig = rateLimiter.getRateLimiterConfig();
            Duration timeoutDuration = rateLimiterConfig.getTimeoutDuration();
            if (rateLimiter.getPermission(timeoutDuration)) {
                return proceed(invocation, rateLimiter, recoveryFunction);
            } else {
                final CompletableFuture promise = new CompletableFuture<>();
                Throwable t = new RequestNotPermitted("Request not permitted for limiter: " + rateLimiter.getName());
                try {
                    promise.complete(recoveryFunction.apply(t));
                } catch (Throwable t2) {
                    promise.completeExceptionally(t2);
                }
                return promise;
            }
        }
        return handleProceedWithException(invocation, rateLimiter, recoveryFunction);
    }

    private Object proceed(MethodInvocation invocation, io.github.resilience4j.ratelimiter.RateLimiter rateLimiter, RecoveryFunction<?> recoveryFunction) throws Throwable {
        Object result;
        try {
            result = invocation.proceed();
        } catch (Exception e) {
            result = handleProceedWithException(invocation, rateLimiter, recoveryFunction);
        }
        return result;
    }

    private Object handleProceedWithException(MethodInvocation invocation, io.github.resilience4j.ratelimiter.RateLimiter rateLimiter, RecoveryFunction<?> recoveryFunction) throws Throwable {
        RateLimiterConfig rateLimiterConfig = rateLimiter.getRateLimiterConfig();
        Duration timeoutDuration = rateLimiterConfig.getTimeoutDuration();
        boolean permission = rateLimiter.getPermission(timeoutDuration);
        if (Thread.interrupted()) {
            throw new IllegalStateException("Thread was interrupted during permission wait");
        }
        if (!permission) {
            Throwable t = new RequestNotPermitted("Request not permitted for limiter: " + rateLimiter.getName());
            return recoveryFunction.apply(t);
        }
        return invocation.proceed();
    }

}
