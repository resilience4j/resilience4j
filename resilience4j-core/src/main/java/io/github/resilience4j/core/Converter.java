package io.github.resilience4j.core;

/**
 * A functional interface for converting an object of type S to an object of type T.
 *
 * <p>
 *     This interface can be used to define custom conversion logic between different types.
 *     Implementations should provide the actual conversion mechanism in the {@link #convert(Object)} method.
 * </p>
 *
 * @param <S> the source type to convert from
 * @param <T> the target type to convert to
 */
@FunctionalInterface
public interface Converter<S, T> {

    /**
     * Converts the given source object to an object of type T.
     *
     * @param source the source object to convert, must not be null
     * @return the converted object of type T
     * @throws IllegalArgumentException if the source object is not valid for conversion
     */
    T convert(S source);
}
