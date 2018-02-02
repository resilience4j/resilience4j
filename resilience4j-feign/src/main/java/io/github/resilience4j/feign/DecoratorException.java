package io.github.resilience4j.feign;

public class DecoratorException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DecoratorException(String message, Exception cause) {
        super(message, cause);
    }

}
