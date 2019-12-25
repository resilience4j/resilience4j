package io.github.resilience4j.customizer;

/**
 * Abstraction to enable customization in Resilience4j internals programmatically.
 *
 * @param <T> type that could be customized.
 * @see CompositeRegistryCustomizer
 */
public interface Customizer<T> {

    /**
     * Customize type passed.
     *
     * @param t type to be customized
     */
    void customize(T t);
}
