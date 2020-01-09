package io.github.resilience4j.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * the composite  of any spring resilience4j type config customizer  implementations.
 */
public class CompositeCustomizer<T extends CustomizerWithName> {

    private final Map<String, T> customizerMap = new HashMap<>();

    public CompositeCustomizer(List<T> customizers) {
        if (customizers != null && !customizers.isEmpty()) {
            customizerMap.putAll(customizers.stream()
                .collect(
                    Collectors.toMap(CustomizerWithName::name, Function.identity())));
        }
    }

    /**
     * @param instanceName the resilience4j instance name
     * @return the found spring customizer if any .
     */
    public Optional<T> getCustomizer(String instanceName) {
        return Optional.ofNullable(customizerMap.get(instanceName));
    }

}
