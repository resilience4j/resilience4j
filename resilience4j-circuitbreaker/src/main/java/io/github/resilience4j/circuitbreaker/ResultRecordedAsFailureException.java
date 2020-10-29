package io.github.resilience4j.circuitbreaker;

/**
 * A {@link ResultRecordedAsFailureException} signals that a result has been recorded as a circuit breaker failure.
 */
public class ResultRecordedAsFailureException extends RuntimeException {

    private final String circuitBreakerName;

    private final transient Object result;

    public ResultRecordedAsFailureException(String circuitBreakerName, Object result) {
        super(String.format("CircuitBreaker '%s' has recorded '%s' as a failure", circuitBreakerName, result.toString()));
        this.result = result;
        this.circuitBreakerName = circuitBreakerName;
    }

    public Object getResult() {
        return result;
    }

    public String getCircuitBreakerName() {
        return circuitBreakerName;
    }
}
