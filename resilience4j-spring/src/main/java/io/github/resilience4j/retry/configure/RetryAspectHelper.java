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
package io.github.resilience4j.retry.configure;

import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.fallback.FallbackDecorators;
import io.github.resilience4j.fallback.FallbackMethod;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.utils.ProceedingJoinPointHelper;

public class RetryAspectHelper {

    private static final Logger logger = LoggerFactory.getLogger(RetryAspectHelper.class);
    private final ScheduledExecutorService retryExecutorService;
    private final RetryRegistry retryRegistry;
    private final @Nullable List<RetryAspectExt> retryAspectExtList;
    private final FallbackDecorators fallbackDecorators;

    /**
     * @param retryRegistry retry definition registry
     * @param retryAspectExtList a list of retry aspect extensions
     * @param fallbackDecorators the fallback decorators
     */
    public RetryAspectHelper(ScheduledExecutorService retryExecutorService, RetryRegistry retryRegistry, @Autowired(required = false) List<RetryAspectExt> retryAspectExtList, FallbackDecorators fallbackDecorators) {
        this.retryExecutorService = retryExecutorService;
        this.retryRegistry = retryRegistry;
        this.retryAspectExtList = retryAspectExtList;
        this.fallbackDecorators = fallbackDecorators;

    }

    public void decorate(ProceedingJoinPointHelper joinPointHelper, Retry retryAnnotation) throws Throwable {
        String backend = retryAnnotation.name();
        io.github.resilience4j.retry.Retry retry = getOrCreateRetry(joinPointHelper.getMethodName(), backend);
        decorateWithoutFallback(joinPointHelper, retry);
        if (StringUtils.isEmpty(retryAnnotation.fallbackMethod())) {
            return;
        }
        FallbackMethod fallbackMethod = FallbackMethod.create(retryAnnotation.fallbackMethod(), joinPointHelper.getMethod(), joinPointHelper.getJoinPoint().getArgs(), joinPointHelper.getJoinPoint().getTarget());
        joinPointHelper.decorateProceedCall(underliningCall -> fallbackDecorators.decorate(fallbackMethod, underliningCall));
    }

    private void decorateWithoutFallback(ProceedingJoinPointHelper joinPointHelper, io.github.resilience4j.retry.Retry retry) throws Throwable {
        if (CompletionStage.class.isAssignableFrom(joinPointHelper.getReturnType())) {
            decorateCompletableFuture(joinPointHelper, retry);
            return;
        }
        if (retryAspectExtList != null && !retryAspectExtList.isEmpty()) {
            for (RetryAspectExt retryAspectExt : retryAspectExtList) {
                if (retryAspectExt.canHandleReturnType(joinPointHelper.getReturnType())) {
                    retryAspectExt.decorate(joinPointHelper, retry);
                    return;
                }
            }
        }
        joinPointHelper.decorateProceedCall(
            underliningCall -> io.github.resilience4j.retry.Retry.decorateCheckedSupplier(retry, underliningCall));
    }

    /**
     * @param methodName the retry method name
     * @param backend the retry backend name
     * @return the configured retry
     */
    private io.github.resilience4j.retry.Retry getOrCreateRetry(String methodName, String backend) {
        io.github.resilience4j.retry.Retry retry = retryRegistry.retry(backend);

        if (logger.isDebugEnabled()) {
            logger.debug("Created or retrieved retry '{}' with max attempts rate '{}'  for method: '{}'",
                    backend, retry.getRetryConfig().getResultPredicate(), methodName);
        }
        return retry;
    }

    /**
     * @param proceedingJoinPoint the AOP logic joint point
     * @param retry the configured async retry
     * @return the result object if any
     */
    @SuppressWarnings("unchecked")
    private void decorateCompletableFuture(ProceedingJoinPointHelper joinPointHelper, io.github.resilience4j.retry.Retry retry) {
        joinPointHelper.decorateProceedCall(
                underliningCall -> () -> retry.executeCompletionStage(retryExecutorService, () -> {
                        try {
                            return (CompletionStage<Object>) underliningCall.apply();
                        } catch (Throwable throwable) {
                            throw new CompletionException(throwable);
                        }
                    })
        );
    }
}
