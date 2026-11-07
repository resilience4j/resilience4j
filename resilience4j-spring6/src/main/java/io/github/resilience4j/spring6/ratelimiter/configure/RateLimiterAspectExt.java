/*
 * Copyright 2019 Mahmoud Romeh
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
package io.github.resilience4j.spring6.ratelimiter.configure;

import io.github.resilience4j.ratelimiter.RateLimiter;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * RateLimiter aspect extension support interface type if you want to support new types
 */
public interface RateLimiterAspectExt {

    boolean canHandleReturnType(Class returnType);

    Object handle(ProceedingJoinPoint proceedingJoinPoint, RateLimiter rateLimiter,
        String methodName) throws Throwable;

    /**
     * Handles an invocation with the number of permits requested by the annotation.
     * The default implementation delegates to the existing handler for compatibility.
     * Extensions supporting weighted calls should override this method.
     *
     * @param proceedingJoinPoint the intercepted invocation
     * @param rateLimiter the configured rate limiter
     * @param methodName the intercepted method name
     * @param permits the number of permits required by the invocation
     * @return the decorated result
     * @throws Throwable if the invocation fails
     */
    default Object handle(ProceedingJoinPoint proceedingJoinPoint, RateLimiter rateLimiter,
        String methodName, int permits) throws Throwable {
        return handle(proceedingJoinPoint, rateLimiter, methodName);
    }
}
