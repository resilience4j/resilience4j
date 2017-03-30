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

import com.google.inject.Provider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.core.StopWatch;
import io.github.resilience4j.ratpack.CircuitBreakerTransformer;
import io.github.resilience4j.ratpack.RecoveryFunction;
import io.github.resilience4j.ratpack.annotation.CircuitBreaker;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import ratpack.exec.Promise;

import javax.inject.Inject;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * A {@link MethodInterceptor} to handle all methods annotated with {@link CircuitBreaker}. It will
 * handle methods that return a Promise only. It will add a transform to the promise with the circuit breaker and
 * fallback found in the annotation.
 */
public class CircuitBreakerMethodInterceptor implements MethodInterceptor {

    private final Provider<CircuitBreakerRegistry> provider;

    @Inject
    public CircuitBreakerMethodInterceptor(Provider<CircuitBreakerRegistry> provider) {
        this.provider = provider;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        CircuitBreaker annotation = invocation.getMethod().getAnnotation(CircuitBreaker.class);
        io.github.resilience4j.circuitbreaker.CircuitBreaker breaker = provider.get().circuitBreaker(annotation.name());
        if (breaker == null) {
            return invocation.proceed();
        }
        RecoveryFunction<?> recoveryFunction = annotation.recovery().newInstance();
        Object result;
        StopWatch stopWatchOuter = StopWatch.start(breaker.getName());
        try {
            result = invocation.proceed();
        } catch (Exception e) {
            breaker.onError(stopWatchOuter.getProcessingDuration(), e);
            return recoveryFunction.apply((Throwable) e);
        }
        if (result instanceof Promise<?>) {
            CircuitBreakerTransformer transformer = CircuitBreakerTransformer.of(breaker);
            if (!annotation.recovery().isAssignableFrom(DefaultRecoveryFunction.class)) {
                transformer = transformer.recover(recoveryFunction);
            }
            result = ((Promise<?>) result).transform(transformer);
        } else if (result instanceof Observable) {
            CircuitBreakerOperator operator = CircuitBreakerOperator.of(breaker);
            result = ((Observable<?>) result).lift(operator).onErrorReturn(t -> recoveryFunction.apply((Throwable) t));
        } else if (result instanceof Flowable) {
            CircuitBreakerOperator operator = CircuitBreakerOperator.of(breaker);
            result = ((Flowable<?>) result).lift(operator).onErrorReturn(t -> recoveryFunction.apply((Throwable) t));
        } else if (result instanceof CompletionStage) {
            CompletionStage stage = (CompletionStage) result;
            StopWatch stopWatch;
            if (breaker.isCallPermitted()) {
                stopWatch = StopWatch.start(breaker.getName());
                return stage.handle((v, t) -> {
                    Duration d = stopWatch.stop().getProcessingDuration();
                    if (t != null) {
                        breaker.onError(d, (Throwable) t);
                        try {
                            return recoveryFunction.apply((Throwable) t);
                        } catch (Exception e) {
                            return v;
                        }
                    } else if (v != null) {
                        breaker.onSuccess(d);
                    }
                    return v;
                });
            } else {
                return CompletableFuture.supplyAsync(() -> {
                    Throwable t = new CircuitBreakerOpenException("CircuitBreaker ${circuitBreaker.name} is open");
                    try {
                        return recoveryFunction.apply((Throwable) t);
                    } catch (Throwable t2) {
                        return null;
                    }
                });
            }
        }
        return result;
    }

}
