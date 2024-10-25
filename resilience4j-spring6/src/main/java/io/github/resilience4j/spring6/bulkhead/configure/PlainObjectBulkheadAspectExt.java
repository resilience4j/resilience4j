package io.github.resilience4j.spring6.bulkhead.configure;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

/**
 * The {@code PlainObjectBulkheadAspectExt} class provides an aspect for managing method executions
 * that return plain objects while enforcing bulkhead constraints.
 *
 * <p>Example usage:</p>
 * <pre>
 *     {@code
 *     @Bulkhead(name = "myBulkHead")
 *     public String myMethod() {
 *         return "This is a plain result";
 *     }
 *     }
 * </pre>
 * <p>
 *     The aspect can be configured to handle specific return types if needed by overriding the
 *     {@link #canHandleReturnType(Class)} method.
 * </p>
 */
public class PlainObjectBulkheadAspectExt implements BulkheadAspectExt {

    private static final Logger logger = LoggerFactory.getLogger(PlainObjectBulkheadAspectExt.class);

    private final TimeLimiterRegistry timeLimiterRegistry;
    private final ScheduledExecutorService executorService;

    public PlainObjectBulkheadAspectExt(TimeLimiterRegistry timeLimiterRegistry) {
        this.timeLimiterRegistry = timeLimiterRegistry;
        this.executorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
    }

    /**
     * Determines if the aspect can handle the specified return type of the method.
     *
     * @param returnType the AOP method return type class
     * @return {@code true} if the method has a plain object return type; {@code false} otherwise.
     * */
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

        try {
            Supplier<CompletableFuture<Object>> futureSupplier = createBulkheadFuture(proceedingJoinPoint, bulkhead);
            return timeLimiter.executeCompletionStage(executorService, futureSupplier)
                    .toCompletableFuture()
                    .join();

        } catch (Exception ex) {
            logException(ex, methodName);
            throw ex;
        }
    }

    private Supplier<CompletableFuture<Object>> createBulkheadFuture(ProceedingJoinPoint proceedingJoinPoint, Bulkhead bulkhead) {
        return Bulkhead.decorateSupplier(bulkhead, () -> {
            try {
                return CompletableFuture.completedFuture(proceedingJoinPoint.proceed());
            } catch (Throwable throwable) {
                CompletableFuture<Object> future = new CompletableFuture<>();
                future.completeExceptionally(throwable);
                return future;
            }
        });
    }

    private void logException(Exception exception, String methodName) {
        logger.error("Error occurred executing '{}': {}", methodName, exception.getMessage(), exception);
        if (exception instanceof BulkheadFullException) {
            logger.debug("Bulkhead '{}' is full.", methodName);
        } else if (exception instanceof CompletionException) {
            Throwable cause = exception.getCause();
            logger.error("Error occurred executing '{}': {}", methodName, cause.getMessage(), cause);
        }
    }
}
