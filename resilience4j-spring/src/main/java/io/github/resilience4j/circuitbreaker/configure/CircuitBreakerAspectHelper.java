/*
 * Copyright 2017 Robert Winkler
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
package io.github.resilience4j.circuitbreaker.configure;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.fallback.FallbackDecorators;
import io.github.resilience4j.fallback.FallbackMethod;
import io.github.resilience4j.utils.ProceedingJoinPointHelper;
import io.vavr.CheckedFunction0;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

@Aspect
public class CircuitBreakerAspectHelper {

    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerAspectHelper.class);

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final @Nullable
    List<CircuitBreakerAspectExt> circuitBreakerAspectExtList;
    private final FallbackDecorators fallbackDecorators;

    public CircuitBreakerAspectHelper(CircuitBreakerRegistry circuitBreakerRegistry, @Autowired(required = false) List<CircuitBreakerAspectExt> circuitBreakerAspectExtList, FallbackDecorators fallbackDecorators) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.circuitBreakerAspectExtList = circuitBreakerAspectExtList;
        this.fallbackDecorators = fallbackDecorators;
    }
    
    public void decorate(ProceedingJoinPointHelper joinPointHelper, CircuitBreaker circuitBreakerAnnotation) throws Throwable {
        String backend = circuitBreakerAnnotation.name();
        io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(joinPointHelper.getMethodName(), backend);
        joinPointHelper.decorateProceedCall(underliningCall -> decorateWithoutFallback(circuitBreaker, joinPointHelper.getReturnType(), underliningCall));
        if (StringUtils.isEmpty(circuitBreakerAnnotation.fallbackMethod())) {
            return;
        }
        FallbackMethod fallbackMethod = FallbackMethod.create(circuitBreakerAnnotation.fallbackMethod(), joinPointHelper.getMethod(), joinPointHelper.getJoinPoint().getArgs(), joinPointHelper.getJoinPoint().getTarget());
        joinPointHelper.decorateProceedCall(underliningCall -> fallbackDecorators.decorate(fallbackMethod, underliningCall));
    }

    private CheckedFunction0<Object> decorateWithoutFallback(io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker, Class<?> returnType, CheckedFunction0<Object> supplier) {
        if (circuitBreakerAspectExtList != null && !circuitBreakerAspectExtList.isEmpty()) {
            for (CircuitBreakerAspectExt circuitBreakerAspectExt : circuitBreakerAspectExtList) {
                if (circuitBreakerAspectExt.canHandleReturnType(returnType)) {
                    return circuitBreakerAspectExt.decorate(circuitBreaker, supplier);
                }
            }
        }
        if (CompletionStage.class.isAssignableFrom(returnType)) {
            return decorateCompletableFuture(circuitBreaker, supplier);
        }
        return io.github.resilience4j.circuitbreaker.CircuitBreaker.decorateCheckedSupplier(circuitBreaker, supplier);
    }

    private io.github.resilience4j.circuitbreaker.CircuitBreaker getOrCreateCircuitBreaker(String methodName, String backend) {
        io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(backend);

        if (logger.isDebugEnabled()) {
            logger.debug("Created or retrieved circuit breaker '{}' with failure rate '{}' for method: '{}'",
                    backend, circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold(), methodName);
        }

        return circuitBreaker;
    }

    /**
     * handle the CompletionStage return types AOP based into configured
     * circuit-breaker
     */
    private CheckedFunction0<Object> decorateCompletableFuture(io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker, CheckedFunction0<Object> supplier) {
        return () -> circuitBreaker.executeCompletionStage(() -> {
            try {
                return (CompletionStage<?>) supplier.apply();
            } catch (Throwable throwable) {
                throw new CompletionException(throwable);
            }
        });
    }
}
