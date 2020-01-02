package io.github.resilience4j.customizer;

/**
 * Abstraction to enable customization in Resilience4j internals programmatically.
 *
 * @param <T> type that could be customized.
 * @see CompositeBuilderCustomizer
 */
@FunctionalInterface
public interface Customizer<T> {

    /**
     * Customize type passed.
     *
     * @param name of the instance to be customized
     * @param t type to be customized
     */
    void customize(String name, T t);
}
