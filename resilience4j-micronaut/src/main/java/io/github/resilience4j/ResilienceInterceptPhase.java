package io.github.resilience4j;

import io.micronaut.aop.Interceptor;

/**
 * <p>{@link Interceptor} classes implement the {@link io.micronaut.core.order.Ordered} interface
 * in order to control the order of execution when multiple interceptors are present.</p>
 *
 * <p> This class provides a set of phases used for resilience4j</p>
 *
 * The default order of phases are: <code>Retry ( CircuitBreaker ( RateLimiter ( TimeLimiter ( Bulkhead ( Function ) ) ) ) )</code>
 * The order places this at {@link io.github.resilience4j.retry.RetryInterceptor} and before {@link io.micronaut.retry.intercept.RecoveryInterceptor}
 *
 */
public enum ResilienceInterceptPhase {

    /**
     * Retry phase of execution.
     */
    RETRY(-60),

    /**
     * Retry phase of execution.
     */
    CIRCUIT_BREAKER(-55),

    /**
     * Retry phase of execution.
     */
    RATE_LIMITER(-50),

    /**
     * Retry phase of execution.
     */
    TIME_LIMITER(-45),

    /**
     * Retry phase of execution.
     */
    BULKHEAD(-40);

    private final int position;

    /**
     * Constructor.
     *
     * @param position The order of position
     */
    ResilienceInterceptPhase(int position) {
        this.position = position;
    }

    /**
     * @return The position
     */
    public int getPosition() {
        return position;
    }
}
