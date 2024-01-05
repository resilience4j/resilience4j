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

package io.github.resilience4j.commons.configuration.retry.configure;

import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.retry.configuration.CommonRetryConfigurationProperties;
import io.github.resilience4j.common.retry.configuration.RetryConfigCustomizer;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.apache.commons.configuration2.Configuration;

import java.util.Map;
import java.util.stream.Collectors;

public class CommonsConfigurationRetryRegistry {
    private CommonsConfigurationRetryRegistry(){
    }

    /**
     * Create a RetryRegistry from apache commons configuration instance
     * @param configuration - apache commons configuration instance
     * @param customizer - customizer for retry configuration
     * @return a RetryRegistry with a Map of shared Retry configurations.
     */
    public static RetryRegistry of(Configuration configuration, CompositeCustomizer<RetryConfigCustomizer> customizer) {
        CommonRetryConfigurationProperties retryConfiguration = CommonsConfigurationRetryConfiguration.of(configuration);
        Map<String, RetryConfig> retryConfigMap = retryConfiguration.getInstances()
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> retryConfiguration.createRetryConfig(entry.getValue(), customizer, entry.getKey())));
        return RetryRegistry.of(retryConfigMap);
    }
}
