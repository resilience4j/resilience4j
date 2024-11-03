package io.github.resilience4j.circuitbreaker.internal;

import org.springframework.core.convert.converter.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A converter that converts a fully qualified class name represented as a {@link String}
 * into a {@link Class} of {@link Throwable} type. This converter is used for the
 * {@code ignoreUnknownExceptions} configuration in a circuit breaker.
 *
 * <p>
 *     If {@code ignoreUnknownExceptions} is set to {@code true}, any unknown exceptions
 *     (those not found on the classpath) are ignored and converted to a placeholder exception.
 *     If {@code false}, an {@link IllegalArgumentException} is thrown when the class is not found.
 * </p>
 */
public class IgnoreUnknownExceptionConverter implements Converter<String, Class<? extends Throwable>> {

    private static final Logger LOG = LoggerFactory.getLogger(IgnoreUnknownExceptionConverter.class);
    private final boolean ignoreUnknownExceptions;

    public IgnoreUnknownExceptionConverter(boolean ignoreUnknownExceptions) {
        this.ignoreUnknownExceptions = ignoreUnknownExceptions;
        if (ignoreUnknownExceptions) {
            LOG.info("IgnoreUnknownExceptions set to true.");
            LOG.info("Ignoring all unknown exceptions.");
        }
    }

    /**
     * Converts the given exception class name (as a {@link String}) into a {@link Class} of
     * {@link Throwable} type.
     *
     * <p>
     *     If the class cannot be found and {@code ignoreUnknownExceptions} is {@code true},
     *     it will return {@link PlaceHolderException} as a substitute. Otherwise, it will throw
     *     an {@link IllegalArgumentException} if the class cannot be found.
     * </p>
     *
     * @param source the fully qualified name of the exception class to convert.
     * @return the {@link Class} object corresponding to the specified class name, or
     *         {@link PlaceHolderException} if the class is not found and {@code ignoreUnknownExceptions} is true.
     * @throws IllegalArgumentException if the class cannot be found and {@code ignoreUnknownExceptions} is false.
     */
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

    /**
     * A placeholder exception class used when an unknown exception class is encountered
     * and {@code ignoreUnknownExceptions} is set to true.
     */
    public static class PlaceHolderException extends Throwable {}
}
