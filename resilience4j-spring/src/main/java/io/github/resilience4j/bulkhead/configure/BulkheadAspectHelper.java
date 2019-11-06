/*
 * Copyright 2019 lespinsideg
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
package io.github.resilience4j.bulkhead.configure;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.fallback.FallbackDecorators;
import io.github.resilience4j.fallback.FallbackMethod;
import io.github.resilience4j.utils.ProceedingJoinPointHelper;
import io.vavr.CheckedFunction0;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import org.springframework.util.StringUtils;

public class BulkheadAspectHelper {

    private static final Logger logger = LoggerFactory.getLogger(BulkheadAspectHelper.class);

    private final BulkheadRegistry bulkheadRegistry;
    private final ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry;
    private final @Nullable
    List<BulkheadAspectExt> bulkheadAspectExts;
    private final FallbackDecorators fallbackDecorators;

    public BulkheadAspectHelper(ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry, BulkheadRegistry bulkheadRegistry, @Autowired(required = false) List<BulkheadAspectExt> bulkheadAspectExts, FallbackDecorators fallbackDecorators) {
        this.bulkheadRegistry = bulkheadRegistry;
        this.bulkheadAspectExts = bulkheadAspectExts;
        this.fallbackDecorators = fallbackDecorators;
        this.threadPoolBulkheadRegistry = threadPoolBulkheadRegistry;
    }

    public void decorate(ProceedingJoinPointHelper joinPointHelper, Bulkhead bulkheadAnnotation) throws Throwable {
        String name = bulkheadAnnotation.name();
        switch (bulkheadAnnotation.type()) {
            case THREADPOOL:
                joinPointHelper.decorateProceedCall(
                        underliningCall -> decorateWithThreadpoolWithoutFallback(name, joinPointHelper.getMethodName(), joinPointHelper.getReturnType(), underliningCall));
                break;
            case SEMAPHORE:
                joinPointHelper.decorateProceedCall(
                        underliningCall -> decorateWithSemaphoreWithoutFallback(name, joinPointHelper.getMethodName(), joinPointHelper.getReturnType(), underliningCall));
                break;
        }
        if (StringUtils.isEmpty(bulkheadAnnotation.fallbackMethod())) {
            return;
        }
        FallbackMethod fallbackMethod = FallbackMethod.create(bulkheadAnnotation.fallbackMethod(), joinPointHelper.getMethod(), joinPointHelper.getJoinPoint().getArgs(), joinPointHelper.getJoinPoint().getTarget());
        joinPointHelper.decorateProceedCall(underliningCall -> fallbackDecorators.decorate(fallbackMethod, underliningCall));
    }

    /**
     * execute the logic wrapped by ThreadPool bulkhead , please check
     * {@link io.github.resilience4j.bulkhead.ThreadPoolBulkhead} for more
     * information
     */
    private CheckedFunction0<Object> decorateWithThreadpoolWithoutFallback(String backend, String methodName, Class<?> returnType, CheckedFunction0<Object> supplier) {
        if (logger.isDebugEnabled()) {
            logger.debug("ThreadPool bulkhead invocation for method {} in backend {}", methodName, backend);
        }
        ThreadPoolBulkhead threadPoolBulkhead = threadPoolBulkheadRegistry.bulkhead(backend);
        if (CompletionStage.class.isAssignableFrom(returnType)) {
            return () -> threadPoolBulkhead.executeSupplier(() -> {
                try {
                    return ((CompletionStage<?>) supplier.apply()).toCompletableFuture().get();
                } catch (Throwable throwable) {
                    throw new CompletionException(throwable);
                }
            });
        } else {
            throw new IllegalStateException("ThreadPool bulkhead is only applicable for completable futures ");
        }
    }

    private CheckedFunction0<Object> decorateWithSemaphoreWithoutFallback(String backend, String methodName, Class<?> returnType, CheckedFunction0<Object> supplier) {
        io.github.resilience4j.bulkhead.Bulkhead bulkhead = getOrCreateBulkhead(methodName, backend);
        if (bulkheadAspectExts != null && !bulkheadAspectExts.isEmpty()) {
            for (BulkheadAspectExt bulkHeadAspectExt : bulkheadAspectExts) {
                if (bulkHeadAspectExt.canHandleReturnType(returnType)) {
                    return bulkHeadAspectExt.decorate(bulkhead, supplier);
                }
            }
        }
        if (CompletionStage.class.isAssignableFrom(returnType)) {
            return decorateCompletableFuture(bulkhead, supplier);
        }
        return io.github.resilience4j.bulkhead.Bulkhead.decorateCheckedSupplier(bulkhead, supplier);
    }

    private io.github.resilience4j.bulkhead.Bulkhead getOrCreateBulkhead(String methodName, String backend) {
        io.github.resilience4j.bulkhead.Bulkhead bulkhead = bulkheadRegistry.bulkhead(backend);

        if (logger.isDebugEnabled()) {
            logger.debug("Created or retrieved bulkhead '{}' with max concurrent call '{}' and max wait time '{}ms' for method: '{}'",
                    backend, bulkhead.getBulkheadConfig().getMaxConcurrentCalls(),
                    bulkhead.getBulkheadConfig().getMaxWaitDuration().toMillis(), methodName);
        }

        return bulkhead;
    }

    /**
     * handle the asynchronous completable future flow
     *
     * @param proceedingJoinPoint AOPJoinPoint
     * @param bulkhead configured bulkhead
     * @return CompletionStage
     */
    private CheckedFunction0<Object> decorateCompletableFuture(io.github.resilience4j.bulkhead.Bulkhead bulkhead, CheckedFunction0<Object> supplier) {
        return () -> bulkhead.executeCompletionStage(() -> {
            try {
                return (CompletionStage<?>) supplier.apply();
            } catch (Throwable throwable) {
                throw new CompletionException(throwable);
            }
        });
    }
}
