package io.github.resilience4j.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * the composite  of any spring resilience4j type config customizer  implementations.
 */
public class CompositeCustomizer<T extends CustomizerWithName> {

    private final Map<String, T> customizerMap = new HashMap<>();

    public CompositeCustomizer(List<T> customizers) {
        if (customizers != null && !customizers.isEmpty()) {
            customizers.forEach(customizer -> {
                if (customizerMap.containsKey(customizer.name())) {
                    throw new IllegalStateException(
                        "It is not possible to define more than one customizer per instance name "
                            + customizer.name());
                } else {
                    customizerMap.put(customizer.name(), customizer);
                }

            });
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
