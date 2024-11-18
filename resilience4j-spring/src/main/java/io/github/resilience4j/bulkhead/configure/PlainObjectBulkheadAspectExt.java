package io.github.resilience4j.bulkhead.configure;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.internal.ThreadPoolBulkheadAdapter;
import io.github.resilience4j.core.ContextAwareScheduledThreadPoolExecutor;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * The {@code PlainObjectBulkheadAspectExt} class provides an aspect for managing method executions
 * that return plain objects while enforcing bulkhead constraints.
 */
public class PlainObjectBulkheadAspectExt implements BulkheadAspectExt {

    private static final Logger logger = LoggerFactory.getLogger(PlainObjectBulkheadAspectExt.class);
    private final TimeLimiterRegistry timeLimiterRegistry;
    private final ScheduledExecutorService executorService;

    public PlainObjectBulkheadAspectExt(
            TimeLimiterRegistry timeLimiterRegistry,
            @Nullable ContextAwareScheduledThreadPoolExecutor contextAwareScheduledThreadPoolExecutor
    ) {
        this.timeLimiterRegistry = timeLimiterRegistry;
        this.executorService = contextAwareScheduledThreadPoolExecutor != null
                ? contextAwareScheduledThreadPoolExecutor
                : Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
    }

    /**
     * Determines if the aspect can handle the specified return type of the method.
     *
     * @param returnType the AOP method return type class
     * @return {@code true} if the method has a plain object return type; {@code false} otherwise.
     */
    @Override
    @SuppressWarnings("rawtypes")
    public boolean canHandleReturnType(Class returnType) {
        return !returnType.equals(CompletableFuture.class)
                && !returnType.equals(Mono.class)
                && !returnType.equals(Flux.class);
    }

    /**
     * Handle the execution of a method wrapped in a bulkhead context.
     *
     * @param proceedingJoinPoint Spring AOP proceedingJoinPoint
     * @param bulkhead the configured bulkhead
     * @param methodName the method name
     * @return the result object from the method execution
     * @throws Throwable if an exception occurs during the method execution
     */
    @Override
    public Object handle(ProceedingJoinPoint proceedingJoinPoint, Bulkhead bulkhead, String methodName) throws Throwable {
        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter(bulkhead.getName());
        if (bulkhead instanceof ThreadPoolBulkheadAdapter) {
            ThreadPoolBulkhead threadPoolBulkhead = ((ThreadPoolBulkheadAdapter) bulkhead).threadPoolBulkhead();
            return handleThreadPoolBulkhead(proceedingJoinPoint, threadPoolBulkhead, timeLimiter, methodName);
        }

        return handleSemaphoreBulkhead(proceedingJoinPoint, bulkhead, timeLimiter, methodName);
    }

    private Object handleSemaphoreBulkhead(ProceedingJoinPoint proceedingJoinPoint, Bulkhead bulkhead, TimeLimiter timeLimiter, String methodName) throws ExecutionException, InterruptedException, TimeoutException {
        try {
            Supplier<CompletableFuture<Object>> futureSupplier = createBulkheadFuture(proceedingJoinPoint, bulkhead);
            long timeout = timeLimiter.getTimeLimiterConfig().getTimeoutDuration().toMillis();
            return timeLimiter.executeCompletionStage(executorService, futureSupplier)
                    .toCompletableFuture()
                    .get(timeout, TimeUnit.MILLISECONDS);
        } catch (BulkheadFullException ex) {
            logBulkheadFullException(methodName, ex);
            throw ex;
        } catch (CompletionException ex) {
            logCompletionException(methodName, ex);
            throw ex;
        } catch (Throwable ex) {
            logGenericException(methodName, ex);
            throw ex;
        }
    }

    private Object handleThreadPoolBulkhead(ProceedingJoinPoint proceedingJoinPoint, ThreadPoolBulkhead threadPoolBulkhead, TimeLimiter timeLimiter, String methodName) {
        try {
            Supplier<CompletionStage<Object>> futureSupplier = threadPoolBulkhead.decorateCallable (() -> {
                try {
                    return proceedingJoinPoint.proceed();
                } catch (Throwable throwable) {
                    return completeFutureExceptionally(throwable);
                }
            });

            return timeLimiter.executeCompletionStage(executorService, futureSupplier)
                    .toCompletableFuture()
                    .join();
        } catch (BulkheadFullException ex) {
            logBulkheadFullException(methodName, ex);
            throw ex;
        } catch (CompletionException ex) {
            logCompletionException(methodName, ex);
            throw ex;
        } catch (Throwable ex) {
            logGenericException(methodName, ex);
            throw ex;
        }
    }

    private Supplier<CompletableFuture<Object>> createBulkheadFuture(ProceedingJoinPoint proceedingJoinPoint, Bulkhead bulkhead) {
        return Bulkhead.decorateSupplier(bulkhead, () -> {
            try {
                return CompletableFuture.completedFuture(proceedingJoinPoint.proceed());
            } catch (Throwable throwable) {
                return completeFutureExceptionally(throwable);
            }
        });
    }

    private CompletableFuture<Object> completeFutureExceptionally(Throwable throwable) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);
        return future;
    }

    private void logBulkheadFullException(String methodName, BulkheadFullException exception) {
        logger.error("Bulkhead '{}' is full: {}", methodName, exception.getMessage(), exception);
    }

    private void logCompletionException(String methodName, CompletionException exception) {
        Throwable cause = exception.getCause();
        logger.error("Completion exception occurred while executing '{}': {}", methodName, cause.getMessage(), cause);
    }

    private void logGenericException(String methodName, Throwable exception) {
        logger.error("Unexpected error occurred executing '{}': {}", methodName, exception.getMessage(), exception);
    }
}
