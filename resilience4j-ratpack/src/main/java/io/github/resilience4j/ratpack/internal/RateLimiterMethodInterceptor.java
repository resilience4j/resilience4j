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

package io.github.resilience4j.ratpack.internal;

import com.google.inject.Inject;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratpack.RateLimiterTransformer;
import io.github.resilience4j.ratpack.RecoveryFunction;
import io.github.resilience4j.ratpack.annotation.RateLimiter;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import ratpack.exec.Promise;

import java.time.Duration;

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
        Object result;
        try {
            result = invocation.proceed();
        } catch (Exception e) {
            RateLimiterConfig rateLimiterConfig = rateLimiter.getRateLimiterConfig();
            Duration timeoutDuration = rateLimiterConfig.getTimeoutDuration();
            boolean permission = rateLimiter.getPermission(timeoutDuration);
            if (Thread.interrupted()) {
                throw new IllegalStateException("Thread was interrupted during permission wait");
            }
            if (!permission) {
                Throwable t = new RequestNotPermitted("Request not permitted for limiter: " + rateLimiter.getName());
                if (!annotation.recovery().isAssignableFrom(DefaultRecoveryFunction.class)) {
                    return recoveryFunction.apply(t);
                } else {
                    throw t;
                }
            } else {
                throw e;
            }
        }
        if (result instanceof Promise<?>) {
            RateLimiterTransformer transformer = RateLimiterTransformer.of(rateLimiter);
            if (!annotation.recovery().isAssignableFrom(DefaultRecoveryFunction.class)) {
                transformer = transformer.recover(recoveryFunction);
            }
            result = ((Promise<?>) result).transform(transformer);
            // TODO drmaas - this will be fixed in a future PR. Commenting out for now.
        }
//        else if (result instanceof CompletionStage) {
//            CompletionStage stage = (CompletionStage) result;
//            RateLimiterConfig rateLimiterConfig = rateLimiter.getRateLimiterConfig();
//            Duration timeoutDuration = rateLimiterConfig.getTimeoutDuration();
//            boolean permission = rateLimiter.getPermission(timeoutDuration);
//            if (permission) {
//                return stage;
//            } else {
//                return CompletableFuture.supplyAsync(() -> {
//                    if (annotation.recovery().isAssignableFrom(DefaultRecoveryFunction.class)) {
//                        throw new RequestNotPermitted("Request not permitted for limiter: " + rateLimiter.getName());
//                    } else {
//                        try {
//                            return recoveryFunction.apply(new RequestNotPermitted("Request not permitted for limiter: " + rateLimiter.getName()));
//                        } catch (Exception e) {
//                            return null;
//                        }
//                    }
//                });
//            }
//        }
        else {
            io.github.resilience4j.ratelimiter.RateLimiter.waitForPermission(rateLimiter);
        }
        return result;
    }

}
