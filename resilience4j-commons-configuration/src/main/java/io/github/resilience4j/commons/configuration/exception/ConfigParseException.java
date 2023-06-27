package io.github.resilience4j.commons.configuration.exception;

public class ConfigParseException extends RuntimeException{
    public ConfigParseException(String message) {
        super(message);
    }

    public ConfigParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
