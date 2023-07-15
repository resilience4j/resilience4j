/*
 *   Copyright 2023: Deepak Kumar
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.github.resilience4j.commons.configuration.circuitbreaker.configure;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import io.github.resilience4j.common.circuitbreaker.configuration.CommonCircuitBreakerConfigurationProperties;
import org.apache.commons.configuration2.Configuration;

import java.util.Map;
import java.util.stream.Collectors;

public class CommonsConfigurationCircuitBreakerRegistry {
    private CommonsConfigurationCircuitBreakerRegistry() {
    }

    /***
     * Create a CircuitBreakerRegistry from apache commons configuration instance
     * @param configuration - apache commons configuration instance
     * @param customizer - customizer for circuit breaker configuration
     * @return a CircuitBreakerRegistry with a Map of shared CircuitBreaker configurations.
     */
    public static CircuitBreakerRegistry of(Configuration configuration, CompositeCustomizer<CircuitBreakerConfigCustomizer> customizer){
        CommonCircuitBreakerConfigurationProperties circuitBreakerProperties = CommonsConfigurationCircuitBreakerConfiguration.of(configuration);
        Map<String, CircuitBreakerConfig> circuitBreakerConfigMap = circuitBreakerProperties.getInstances()
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> circuitBreakerProperties.createCircuitBreakerConfig(entry.getKey(), entry.getValue(), customizer)));
        return CircuitBreakerRegistry.of(circuitBreakerConfigMap);
    }
}
