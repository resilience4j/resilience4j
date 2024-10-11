package io.github.resilience4j.timelimiter.configure;

import org.aspectj.lang.ProceedingJoinPoint;
import io.github.resilience4j.timelimiter.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * The {@code PlainObjectTimeLimiterAspectExt} is an extension of the {@link TimeLimiterAspectExt}
 * that handles plain object return types for time-limited methods.
 *
 * <p>Example usage:</p>
 * <pre>
 *     {@code
 *     @TimeLimiter(name = "myTimeLimiter")
 *     public Object myTimeLimitedMethod() {
 *         return new MyObject();
 *     }}
 * </pre>
 */
public class PlainObjectTimeLimiterAspectExt implements TimeLimiterAspectExt {

    private final Logger logger = LoggerFactory.getLogger(PlainObjectTimeLimiterAspectExt.class);

    /**
     * Determines if the aspect can handle the specified return type of a method.
     *
     * @param returnType the class of the return type to check.
     * @return true if the return type is a plain object (not a CompletionStage), false otherwise.
     */
    @Override
    public boolean canHandleReturnType(Class<?> returnType) {
        return !returnType.equals(CompletableFuture.class)
                && !returnType.equals(Mono.class)
                && !returnType.equals(Flux.class);
    }

    /**
     * Handles the execution of the method represented by the join point,
     * applying time limiting as defined by the TimeLimiter.
     *
     * @param joinPoint the join point representing the method execution
     * @param timeLimiter the time limiter to enforce the execution time limit
     * @param methodName the name of the method being executed
     * @return the result of the method execution
     * @throws Throwable if the method execution fails or is interrupted
     */
    @Override
    public Object handle(ProceedingJoinPoint joinPoint, TimeLimiter timeLimiter, String methodName) throws Throwable {
        try {
            Supplier<CompletableFuture<Object>> futureSupplier = () -> CompletableFuture.supplyAsync(() -> {
                try {
                    return joinPoint.proceed();
                } catch (Throwable throwable) {
                    throw new CompletionException(throwable);
                }
            });

            CompletableFuture<Object> future = (CompletableFuture<Object>) timeLimiter.executeFutureSupplier(futureSupplier);
            return future.join();
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause();
            logger.error("Method {} execution failed.", methodName, cause);
            throw cause;
        } catch (Exception ex) {
            logger.error("Method {} execution timed out.", methodName, ex);
            throw new TimeoutException("Method " + methodName + " execution timed out.");
        }
    }
}
