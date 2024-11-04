package io.github.resilience4j.circuitbreaker;

import io.github.resilience4j.core.Converter;

import java.util.HashMap;
import java.util.Map;

/**
 * A registry for managing converters that convert objects from one type to another.
 *
 * <p>
 *     This class allows the registration, retrieval, and removal of converters for
 *     specific source types. It provides a single method to convert objects using
 *     the registered converters.
 * </p>
 */
public class ConverterRegistry {

    private final Map<Class<?>, Converter<?, ?>> converters;

    public ConverterRegistry() {
        this.converters = new HashMap<>();
    }

    /**
     * Adds a converter for a specific source type.
     *
     * @param sourceType the class type of the source objects that the converter can handle.
     * @param converter the converter instance to be registered.
     */
    public void registerConverter(Class<?> sourceType, Converter<?, ?> converter) {
        converters.put(sourceType, converter);
    }

    /**
     * Removes a converter from the registry given the specified source type.
     *
     * @param sourceType the class type of the source objects for which the converter should remove.
     */
    public void removeConverter(Class<?> sourceType) {
        converters.remove(sourceType);
    }

    /**
     * Converts a source object of type S to an object of type T using the registered converter.
     *
     * @param source the source object to convert
     * @param target the class type of the target object
     * @param <S>    the type of the source object
     * @param <T>    the type of the target object
     * @return the converted object of type T
     * @throws IllegalArgumentException if no converter is registered for the source object's class
     */
    @SuppressWarnings("unchecked")
    public <S, T> T convert(S source, Class<T> target) {
        Converter<S, T> converter = (Converter<S, T>) converters.get(source.getClass());
        if (converter == null) {
            throw new IllegalArgumentException("No converter registered for " + source.getClass());
        }
        return converter.convert(source);
    }

    /**
     * Retrieves the converter registered for a specific source type.
     *
     * @param source the class type of the source objects for which the converter is to be retrieved
     * @return the converter registered for the specified source type, or null if no converter is found
     */
    public Converter<?, ?> getConverter(Class<?> source) {
        return converters.get(source);
    }
}
