package io.github.resilience4j.circuitbreaker.internal;

import org.springframework.core.convert.converter.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CircuitBreakerExceptionClassConverter implements Converter<String, Class<? extends Throwable>> {

    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakerExceptionClassConverter.class);
    private final boolean ignoreUnknownExceptions;

    public CircuitBreakerExceptionClassConverter(boolean ignoreUnknownExceptions) {
        this.ignoreUnknownExceptions = ignoreUnknownExceptions;
        if (ignoreUnknownExceptions) {
            LOG.info("IgnoreUnknownExceptions set to true.");
            LOG.info("Ignoring all unknown exceptions.");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Throwable> convert(String source) {
        try {
            return (Class<? extends Throwable>) Class.forName(source);
        } catch (ClassNotFoundException ex) {
            if (ignoreUnknownExceptions) {
                LOG.warn("Ignoring unknown exception: {}", source);
                return PlaceHolderException.class;
            } else {
                throw new IllegalArgumentException("Class not found: " + source, ex);
            }
        }
    }

    public static class PlaceHolderException extends Throwable {}
}
