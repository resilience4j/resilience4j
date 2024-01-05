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

package io.github.resilience4j.commons.configuration.timelimiter.configure;

import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.timelimiter.configuration.CommonTimeLimiterConfigurationProperties;
import io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigCustomizer;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.apache.commons.configuration2.Configuration;

import java.util.Map;
import java.util.stream.Collectors;

public class CommonsConfigurationTimeLimiterRegistry {
    private CommonsConfigurationTimeLimiterRegistry() {
    }

    /**
     * Create a TimeLimiterRegistry from apache commons configuration instance
     * @param configuration - apache commons configuration instance
     * @param customizer - customizer for time limiter configuration
     * @return a TimeLimiterRegistry with a Map of shared TimeLimiter configurations.
     */
    public static TimeLimiterRegistry of(Configuration configuration, CompositeCustomizer<TimeLimiterConfigCustomizer> customizer){
        CommonTimeLimiterConfigurationProperties timeLimiterProperties = CommonsConfigurationTimeLimiterConfiguration.of(configuration);
        Map<String, TimeLimiterConfig> timeLimiterConfigMap = timeLimiterProperties.getInstances()
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> timeLimiterProperties.createTimeLimiterConfig(entry.getKey(), entry.getValue(), customizer)));
        return TimeLimiterRegistry.of(timeLimiterConfigMap);
    }
}
