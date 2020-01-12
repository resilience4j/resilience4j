package io.github.resilience4j.customizer;

import io.github.resilience4j.core.lang.NonNull;

import java.util.function.Consumer;

import static io.github.resilience4j.core.StringUtils.isNotEmpty;
import static java.util.Objects.requireNonNull;

/**
 * Abstraction to enable customization in Resilience4j internals programmatically.
 *
 * @param <T> type that could be customized.
 * @see CompositeBuilderCustomizer
 */

public interface Customizer<T> {


    /**
     * Method to apply customizer if InstanceName matches the argument passed.
     *
     * @param instanceName the name of the instance
     * @param t            type that needs to be customized
     */
    default void customizeIfNameMatch(String instanceName, T t) {
        if (isNotEmpty(instanceName) && instanceName.equalsIgnoreCase(instanceName())) {
            customize(t);
        }
    }

    /**
     * A convenient method to create Customizer using {@link Consumer}
     *
     * @param instanceName the name of the instance
     * @param consumer     delegate call to Consumer when  {@link Customizer#customize(Object)} is
     *                     called
     * @param <T>          generic type of Customizer
     * @return Customizer instance
     */
    static <T> Customizer<T> of(@NonNull String instanceName, @NonNull Consumer<T> consumer) {
        requireNonNull(instanceName, "InstanceName should be non null");
        return new Customizer<T>() {

            @Override
            public void customize(T obj) {
                consumer.accept(obj);
            }

            @Override
            public String instanceName() {
                return instanceName;
            }
        };
    }

    /**
     * The type to be customized
     *
     * @param t the type to be customized
     */
    void customize(T t);


    /**
     * The name of the backend instance
     *
     * @return name of the backend instance
     */
    String instanceName();
}
