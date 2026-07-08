package io.github.resilience4j.retry;

public class RetryBudgetExceededException extends RuntimeException {
    public RetryBudgetExceededException(String name, double ratio) {
        super(String.format("Retry budget exceeded for '%s' (allowed ratio: %.2f)", name, ratio));
    }
}
