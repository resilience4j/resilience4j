package io.github.resilience4j.circuitbreaker.configure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;

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
public class IgnoreClassBindingExceptionConverter implements Converter<String, Class<? extends Throwable>> {

    private static final Logger LOG = LoggerFactory.getLogger(IgnoreClassBindingExceptionConverter.class);
    private final boolean ignoreUnknownExceptions;

    public IgnoreClassBindingExceptionConverter(boolean ignoreUnknownExceptions) {
        this.ignoreUnknownExceptions = ignoreUnknownExceptions;
        if (ignoreUnknownExceptions) {
            LOG.debug("Configured to ignore unknown exceptions. Unknown exceptions will be replaced with a placeholder.");
        }
    }

    /**
     * Converts the given exception class name as a {@link String} into a Class of
     * {@link Throwable} type.
     *
     * <p>
     *     If the class cannot be found and {@code ignoreUnknownExceptions} is {@code true},
     *     it will return {@link PlaceHolderException} as a substitute. Otherwise, it will throw
     *     an {@link IllegalArgumentException} if the class cannot be found.
     * </p>
     *
     * @param source the fully qualified name of the exception class to convert.
     * @return the Class object corresponding to the specified class name, or
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
                LOG.debug("Exception class '{}' could not be found. Ignoring and substituting with placeholder exception.", source);
                return IgnoreClassBindingExceptionConverter.PlaceHolderException.class;
            } else {
                LOG.error("Class '{}' not found and ignoreUnknownExceptions is set to false.", source);
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