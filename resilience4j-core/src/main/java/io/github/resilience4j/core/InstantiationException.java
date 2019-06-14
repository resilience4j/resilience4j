package io.github.resilience4j.core;

public class InstantiationException extends RuntimeException {

    public InstantiationException(String message) {
        super(message);
    }

    public InstantiationException(String message, Throwable cause) {
        super(message, cause);
    }

}
