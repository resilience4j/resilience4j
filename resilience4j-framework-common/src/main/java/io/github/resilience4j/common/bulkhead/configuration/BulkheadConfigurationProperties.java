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
package io.github.resilience4j.common.bulkhead.configuration;

import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.StringUtils;
import io.github.resilience4j.core.lang.Nullable;

import javax.validation.constraints.Min;
import java.util.HashMap;
import java.util.Map;

public class BulkheadConfigurationProperties {

    private Map<String, BackendProperties> backends = new HashMap<>();
    private Map<String, BackendProperties> configs = new HashMap<>();

    public io.github.resilience4j.bulkhead.BulkheadConfig createBulkheadConfig(BackendProperties backendProperties) {
        if (!StringUtils.isNullOrEmpty(backendProperties.getBaseConfig())) {
            BackendProperties baseProperties = configs.get(backendProperties.getBaseConfig());
            if (baseProperties == null) {
                throw new ConfigurationNotFoundException(backendProperties.getBaseConfig());
            }
            return buildConfigFromBaseConfig(baseProperties, backendProperties);
        }
        return buildBulkheadConfig(io.github.resilience4j.bulkhead.BulkheadConfig.custom(), backendProperties);
    }

    private io.github.resilience4j.bulkhead.BulkheadConfig buildConfigFromBaseConfig(BackendProperties baseProperties, BackendProperties backendProperties) {
        io.github.resilience4j.bulkhead.BulkheadConfig baseConfig = buildBulkheadConfig(io.github.resilience4j.bulkhead.BulkheadConfig.custom(), baseProperties);
        return buildBulkheadConfig(io.github.resilience4j.bulkhead.BulkheadConfig.from(baseConfig), backendProperties);
    }

    private io.github.resilience4j.bulkhead.BulkheadConfig buildBulkheadConfig(io.github.resilience4j.bulkhead.BulkheadConfig.Builder builder, BackendProperties backendProperties) {
        if (backendProperties.getMaxConcurrentCalls() != null) {
            builder.maxConcurrentCalls(backendProperties.getMaxConcurrentCalls());
        }
        if (backendProperties.getMaxWaitTime() != null) {
            builder.maxWaitTime(backendProperties.getMaxWaitTime());
        }
        return builder.build();
    }

    @Nullable
    public BackendProperties getBackendProperties(String backend) {
        return backends.get(backend);
    }

    public Map<String, BackendProperties> getBackends() {
        return backends;
    }

    public Map<String, BackendProperties> getConfigs() {
        return configs;
    }

    /**
     * Bulkhead config adapter for integration with Ratpack. {@link #maxWaitTime} should
     * almost always be set to 0, so the compute threads would not be blocked upon execution.
     */
    public static class BackendProperties {

        @Min(1)
        private Integer maxConcurrentCalls;
        @Min(0)
        private Long maxWaitTime;
        @Nullable
        private String baseConfig;
        @Min(1)
        private Integer eventConsumerBufferSize = 100;

        public BackendProperties setMaxConcurrentCalls(Integer maxConcurrentCalls) {
            this.maxConcurrentCalls = maxConcurrentCalls;
            return this;
        }

        public BackendProperties setMaxWaitTime(Long maxWaitTime) {
            this.maxWaitTime = maxWaitTime;
            return this;
        }

        public BackendProperties setBaseConfig(String baseConfig) {
            this.baseConfig = baseConfig;
            return this;
        }

        public BackendProperties eventConsumerBufferSize(Integer eventConsumerBufferSize) {
            this.eventConsumerBufferSize = eventConsumerBufferSize;
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

        public Integer getEventConsumerBufferSize() {
            return eventConsumerBufferSize;
        }

    }

}
