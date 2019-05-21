/*
 * Copyright 2019 Dan Maas
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
package io.github.resilience4j.ratpack.bulkhead;

import com.google.common.base.Strings;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.lang.Nullable;

import java.util.HashMap;
import java.util.Map;

public class BulkheadConfigurationProperties {

    private Map<String, BulkheadConfig> backends = new HashMap<>();
    private Map<String, BulkheadConfig> configs = new HashMap<>();

    public io.github.resilience4j.bulkhead.BulkheadConfig createBulkheadConfig(BulkheadConfig backendProperties) {
        if (!Strings.isNullOrEmpty(backendProperties.getBaseConfig())) {
            BulkheadConfig baseProperties = configs.get(backendProperties.getBaseConfig());
            if (baseProperties == null) {
                throw new ConfigurationNotFoundException(backendProperties.getBaseConfig());
            }
            return buildConfigFromBaseConfig(baseProperties, backendProperties);
        }
        return buildBulkheadConfig(io.github.resilience4j.bulkhead.BulkheadConfig.custom(), backendProperties);
    }

    private io.github.resilience4j.bulkhead.BulkheadConfig buildConfigFromBaseConfig(BulkheadConfig baseProperties, BulkheadConfig backendProperties) {
        io.github.resilience4j.bulkhead.BulkheadConfig baseConfig = buildBulkheadConfig(io.github.resilience4j.bulkhead.BulkheadConfig.custom(), baseProperties);
        return buildBulkheadConfig(io.github.resilience4j.bulkhead.BulkheadConfig.from(baseConfig), backendProperties);
    }

    private io.github.resilience4j.bulkhead.BulkheadConfig buildBulkheadConfig(io.github.resilience4j.bulkhead.BulkheadConfig.Builder builder, BulkheadConfig backendProperties) {
        if (backendProperties.getMaxConcurrentCalls() != null) {
            builder.maxConcurrentCalls(backendProperties.getMaxConcurrentCalls());
        }
        if (backendProperties.getMaxWaitTime() != null) {
            builder.maxWaitTime(backendProperties.getMaxWaitTime());
        }
        return builder.build();
    }

    public Map<String, BulkheadConfig> getBackends() {
        return backends;
    }

    public Map<String, BulkheadConfig> getConfigs() {
        return configs;
    }

    /**
     * Bulkhead config adapter for integration with Ratpack. {@link #maxWaitTime} should
     * almost always be set to 0, so the compute threads would not be blocked upon execution.
     */
    public static class BulkheadConfig {

        @Nullable
        private Integer maxConcurrentCalls;
        @Nullable
        private Long maxWaitTime;
        @Nullable
        private String baseConfig;

        public BulkheadConfig maxConcurrentCalls(Integer maxConcurrentCalls) {
            this.maxConcurrentCalls = maxConcurrentCalls;
            return this;
        }

        public BulkheadConfig maxWaitTime(Long maxWaitTime) {
            this.maxWaitTime = maxWaitTime;
            return this;
        }

        public BulkheadConfig baseConfig(String baseConfig) {
            this.baseConfig = baseConfig;
            return this;
        }

        public Integer getMaxConcurrentCalls() {
            return maxConcurrentCalls;
        }

        public Long getMaxWaitTime() {
            return maxWaitTime;
        }

        public String getBaseConfig() {
            return baseConfig;
        }

    }

}
