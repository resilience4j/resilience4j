package io.github.resilience4j.circuitbreaker.internal;

import org.springframework.core.convert.converter.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringToThrowableClassConverter implements Converter<String, Class<? extends Throwable>> {

    private static final Logger LOG = LoggerFactory.getLogger(StringToThrowableClassConverter.class);
    private final boolean ignoreUnknownExceptions;

    public StringToThrowableClassConverter(boolean ignoreUnknownExceptions) {
        this.ignoreUnknownExceptions = ignoreUnknownExceptions;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Throwable> convert(String source) {
        try {
            return (Class<? extends Throwable>) Class.forName(source);
        } catch (ClassNotFoundException ex) {
            if (ignoreUnknownExceptions) {
                LOG.warn("Ignore Unknown Exceptions set to true.");
                LOG.warn("Class not found: {}. Ignoring...", source);
                return PlaceHolderException.class;
            } else {
                throw new IllegalArgumentException("Class not found: " + source, ex);
            }
        }
    }

    public static class PlaceHolderException extends Throwable {}
}
