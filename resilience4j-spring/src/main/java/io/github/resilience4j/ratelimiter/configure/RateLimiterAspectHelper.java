/*
 * Copyright 2017 Bohdan Storozhuk
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
package io.github.resilience4j.ratelimiter.configure;

import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.fallback.FallbackDecorators;
import io.github.resilience4j.fallback.FallbackMethod;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.utils.ProceedingJoinPointHelper;
import io.vavr.CheckedFunction0;

public class RateLimiterAspectHelper {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiterAspectHelper.class);
    private final RateLimiterRegistry rateLimiterRegistry;
    private final @Nullable
    List<RateLimiterAspectExt> rateLimiterAspectExtList;
    private final FallbackDecorators fallbackDecorators;

    public RateLimiterAspectHelper(RateLimiterRegistry rateLimiterRegistry, @Autowired(required = false) List<RateLimiterAspectExt> rateLimiterAspectExtList, FallbackDecorators fallbackDecorators) {
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.rateLimiterAspectExtList = rateLimiterAspectExtList;
        this.fallbackDecorators = fallbackDecorators;
    }
    
    public void decorate(ProceedingJoinPointHelper joinPointHelper, RateLimiter rateLimiterAnnotation) throws Throwable {
        String name = rateLimiterAnnotation.name();
        io.github.resilience4j.ratelimiter.RateLimiter rateLimiter = getOrCreateRateLimiter(joinPointHelper.getDeclaringMethodName(), name);
        joinPointHelper.decorateProceedCall(underliningCall -> decorateWithoutFallback(rateLimiter, joinPointHelper.getReturnType(), underliningCall));
        if (StringUtils.isEmpty(rateLimiterAnnotation.fallbackMethod())) {
            return;
        }
        FallbackMethod fallbackMethod = FallbackMethod.create(rateLimiterAnnotation.fallbackMethod(), joinPointHelper.getDeclaringMethod(), joinPointHelper.getJoinPoint().getArgs(), joinPointHelper.getJoinPoint().getTarget());
        joinPointHelper.decorateProceedCall(underliningCall -> fallbackDecorators.decorate(fallbackMethod, underliningCall));
    }

    private CheckedFunction0<Object> decorateWithoutFallback(io.github.resilience4j.ratelimiter.RateLimiter rateLimiter, Class<?> returnType, CheckedFunction0<Object> supplier) {
        if (rateLimiterAspectExtList != null && !rateLimiterAspectExtList.isEmpty()) {
            for (RateLimiterAspectExt rateLimiterAspectExt : rateLimiterAspectExtList) {
                if (rateLimiterAspectExt.canHandleReturnType(returnType)) {
                    return rateLimiterAspectExt.decorate(rateLimiter, supplier);
                }
            }
        }
        if (CompletionStage.class.isAssignableFrom(returnType)) {
            return decorateCompletableFuture(rateLimiter, supplier);
        }
        return io.github.resilience4j.ratelimiter.RateLimiter.decorateCheckedSupplier(rateLimiter, supplier);
    }

    private io.github.resilience4j.ratelimiter.RateLimiter getOrCreateRateLimiter(String methodName, String name) {
        io.github.resilience4j.ratelimiter.RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(name);

        if (logger.isDebugEnabled()) {
            RateLimiterConfig rateLimiterConfig = rateLimiter.getRateLimiterConfig();
            logger.debug(
                    "Created or retrieved rate limiter '{}' with period: '{}'; "
                            + "limit for period: '{}'; timeout: '{}'; method: '{}'",
                    name, rateLimiterConfig.getLimitRefreshPeriod(), rateLimiterConfig.getLimitForPeriod(),
                    rateLimiterConfig.getTimeoutDuration(), methodName
            );
        }

        return rateLimiter;
    }

    /**
     * @param rateLimiter configured rate limiter
     * @param supplier target function that should be decorated
     * @return the result object if any
     */
    @SuppressWarnings("unchecked")
    private CheckedFunction0<Object> decorateCompletableFuture(io.github.resilience4j.ratelimiter.RateLimiter rateLimiter, CheckedFunction0<Object> supplier) {
        return () -> rateLimiter.executeCompletionStage(() -> {
            try {
                return (CompletionStage<?>) supplier.apply();
            } catch (Throwable throwable) {
                throw new CompletionException(throwable);
            }
        });
    }
}
