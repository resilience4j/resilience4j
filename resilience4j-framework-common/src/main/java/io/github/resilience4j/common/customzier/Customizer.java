package io.github.resilience4j.common.customzier;

/**
 * Abstraction to enable customization in Resilience4j internals programmatically.
 *
 * @param <T> type that could be customized.
 */
public interface Customizer<T> {

    /**
     * Customize type passed.
     *
     * @param t type to be customized
     */
    void customize(T t);

    /**
     * @return name of the instance to be customized
     */
    String name();
}
