package io.github.resilience4j.feign.test;

/**
 * A fallback throwing an exception.
 */
public class TestServiceFallbackThrowingException implements TestService {

    @Override
    public String greeting() {
        throw new RuntimeException("Exception in greeting fallback");
    }
}
