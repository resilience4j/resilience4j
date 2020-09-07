/*
 * Copyright Mahmoud Romeh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
