package io.github.resilience4j.commons.configuration.dummy;

public class DummyIgnoredException extends RuntimeException{
    public DummyIgnoredException(String message) {
        super(message);
    }

    public DummyIgnoredException(String message, Throwable cause) {
        super(message, cause);
    }
}
