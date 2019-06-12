package io.github.resilience4j.feign.test;

/**
 * A fallback consuming the thrown exception.
 */
public class TestServiceFallbackWithException implements TestService {

    private final Exception cause;

    public TestServiceFallbackWithException(Exception cause) {
        this.cause = cause;
    }

    @Override
    public String greeting() {
        return "Message from exception: " + cause.getMessage();
    }
}
