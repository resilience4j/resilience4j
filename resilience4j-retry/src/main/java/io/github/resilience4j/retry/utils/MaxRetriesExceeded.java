package io.github.resilience4j.retry.utils;

/**
 * Max Retries reached out exception , to be thrown on result predicate check exceed the max configured retries
 */
public class MaxRetriesExceeded extends RuntimeException {
    public MaxRetriesExceeded(String errorMsg) {
        super(errorMsg);
    }
}
