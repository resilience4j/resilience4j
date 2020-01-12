package io.github.resilience4j.customizer;

import java.util.List;
import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * Convenience class that encapsulate {@link Customizer} instances, convert them into Map of
 * InstanceName and Customizer instance
 *
 * @param <T> supported types that could be customized
 */
public class CompositeBuilderCustomizer<T> {

    final private Map<String, Customizer<T>> customizerMap;

    public CompositeBuilderCustomizer(List<Customizer<T>> delegates) {
        this.customizerMap = delegates.stream()
            .collect(toMap(Customizer::instanceName, identity()));
    }

    public void customize(String name, T t) {
        Customizer<T> customizer = customizerMap.get(name);
        if (customizer != null) {
            customizer.customizeIfNameMatch(name, t);
        }
    }

}
