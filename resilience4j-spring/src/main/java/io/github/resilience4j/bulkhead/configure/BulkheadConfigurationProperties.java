/*
 * Copyright 2019 lespinsideg
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
package io.github.resilience4j.bulkhead.configure;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;

import javax.validation.constraints.Min;
import java.util.HashMap;
import java.util.Map;

import static io.github.resilience4j.bulkhead.BulkheadConfig.Builder;
import static io.github.resilience4j.bulkhead.BulkheadConfig.custom;

public class BulkheadConfigurationProperties {
    /*
    This property gives you control over Bulkhead aspect application order.
    By default Bulkhead will be executed BEFORE CircuitBreaker and RateLimiter.
    By adjusting each aspect order from ConfigurationProperties
    you explicitly define aspects execution sequence.
    */
    private int bulkheadAspectOrder = Integer.MAX_VALUE - 2;
    private Map<String, BackendProperties> backends = new HashMap<>();

    public int getBulkheadAspectOrder() {
        return bulkheadAspectOrder;
    }

    public void setBulkheadAspectOrder(int bulkheadAspectOrder) {
        this.bulkheadAspectOrder = bulkheadAspectOrder;
    }

    private BackendProperties getBackendProperties(String backend) {
        return backends.get(backend);
    }

    public BulkheadConfig createBulkheadConfig(String backend) {
        return createBulkheadConfig(getBackendProperties(backend));
    }

    private BulkheadConfig createBulkheadConfig(BackendProperties backendProperties) {
        return buildBulkheadConfig(backendProperties).build();
    }

    public Builder buildBulkheadConfig(BackendProperties properties) {
        if (properties == null) {
            return new Builder();
        }

        Builder builder = custom();

        if (properties.getMaxConcurrentCall() != null) {
            builder.maxConcurrentCalls(properties.getMaxConcurrentCall());
        }

        if (properties.getMaxWaitTime() != null) {
            builder.maxWaitTime(properties.getMaxWaitTime());
        }

        return builder;
    }

    public Map<String, BackendProperties> getBackends() {
        return backends;
    }

    /**
     * Class storing property values for configuring {@link Bulkhead} instances.
     */
    public static class BackendProperties {
        @Min(1)
        private Integer maxConcurrentCall;

        @Min(0)
        private Long maxWaitTime;

        @Min(1)
        private Integer eventConsumerBufferSize = 100;

        /**
         * Returns the max concurrent call of the bulkhead.
         *
         * @return the max concurrent call
         */
        public Integer getMaxConcurrentCall() {
            return maxConcurrentCall;
        }

        /**
         * Sets the max concurrent call of the bulkhead.
         *
         * @param maxConcurrentCall the max concurrent call
         */
        public void setMaxConcurrentCall(Integer maxConcurrentCall) {
            this.maxConcurrentCall = maxConcurrentCall;
        }

        /**
         * Returns the max wait time for the bulkhead in milliseconds.
         *
         * @return the failure rate threshold
         */
        public Long getMaxWaitTime() {
            return maxWaitTime;
        }

        /**
         * Sets the max wait time for the bulkhead in milliseconds.
         *
         * @param maxWaitTime the max wait time
         */
        public void setMaxWaitTime(Long maxWaitTime) {
            this.maxWaitTime = maxWaitTime;
        }

        public Integer getEventConsumerBufferSize() {
            return eventConsumerBufferSize;
        }

        public void setEventConsumerBufferSize(Integer eventConsumerBufferSize) {
            this.eventConsumerBufferSize = eventConsumerBufferSize;
        }
    }
}
