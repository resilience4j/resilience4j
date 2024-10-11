package io.github.resilience4j.bulkhead.configure;

import io.github.resilience4j.bulkhead.Bulkhead;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

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

    /**
     * Determines if the aspect can handle the specified return type of a method.
     *
     * @param returnType the AOP method return type class
     * @return {@code true} if the method has a plain object return type; {@code false} otherwise.
     */
    @Override
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
        try {
            return proceedingJoinPoint.proceed();
        } catch (Throwable throwable) {
            logger.error("Error occurred while executing method: {}", methodName, throwable);
            throw throwable;
        }
    }
}
