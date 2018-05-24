package io.github.resilience4j.circuitbreaker.autoconfigure;
/*
 * Copyright 2017 Robert Winkler
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

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.*;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

@ConfigurationProperties(prefix = "resilience4j.circuitbreaker")
@Component
public class CircuitBreakerProperties {
    // This property gives you control over CircuitBreaker aspect application order.
    // By default CircuitBreaker will be executed BEFORE RateLimiter.
    // By adjusting RateLimiterProperties.rateLimiterAspectOrder and CircuitBreakerProperties.circuitBreakerAspectOrder
    // you explicitly define aspects CircuitBreaker and RateLimiter execution sequence.
    private int circuitBreakerAspectOrder = Integer.MAX_VALUE - 1;
    private Map<String, BackendProperties> backends = new HashMap<>();

    public int getCircuitBreakerAspectOrder() {
        return circuitBreakerAspectOrder;
    }

    public void setCircuitBreakerAspectOrder(int circuitBreakerAspectOrder) {
        this.circuitBreakerAspectOrder = circuitBreakerAspectOrder;
    }

    private BackendProperties getBackendProperties(String backend) {
        return backends.get(backend);
    }

    public CircuitBreakerConfig createCircuitBreakerConfig(String backend) {
        return createCircuitBreakerConfig(getBackendProperties(backend));
    }

    private CircuitBreakerConfig createCircuitBreakerConfig(BackendProperties backendProperties) {
        return buildCircuitBreakerConfig(backendProperties).build();
    }

    public Builder buildCircuitBreakerConfig(BackendProperties backendProperties) {
        if (backendProperties == null) {
            return new Builder();
        }

        Builder circuitBreakerConfigBuilder = CircuitBreakerConfig.custom();

        if (backendProperties.getWaitInterval() != null) {
            circuitBreakerConfigBuilder.waitDurationInOpenState(Duration.ofMillis(backendProperties.getWaitInterval()));
        }

        if (backendProperties.getFailureRateThreshold() != null) {
            circuitBreakerConfigBuilder.failureRateThreshold(backendProperties.getFailureRateThreshold());
        }

        if (backendProperties.getRingBufferSizeInClosedState() != null) {
            circuitBreakerConfigBuilder.ringBufferSizeInClosedState(backendProperties.getRingBufferSizeInClosedState());
        }

        if (backendProperties.getRingBufferSizeInHalfOpenState() != null) {
            circuitBreakerConfigBuilder.ringBufferSizeInHalfOpenState(backendProperties.getRingBufferSizeInHalfOpenState());
        }

        if (backendProperties.recordFailurePredicate != null) {
            circuitBreakerConfigBuilder.recordFailure(BeanUtils.instantiateClass(backendProperties.getRecordFailurePredicate()));
        }

        if (backendProperties.recordExceptions != null) {
            circuitBreakerConfigBuilder.recordExceptions(backendProperties.recordExceptions);
        }

        if (backendProperties.ignoreExceptions != null) {
            circuitBreakerConfigBuilder.ignoreExceptions(backendProperties.ignoreExceptions);
        }

        return circuitBreakerConfigBuilder;
    }

    public Map<String, BackendProperties> getBackends() {
        return backends;
    }

    /**
     * Class storing property values for configuring {@link io.github.resilience4j.circuitbreaker.CircuitBreaker} instances.
     */
    public static class BackendProperties {

        private Integer waitInterval;

        private Integer failureRateThreshold;

        private Integer ringBufferSizeInClosedState;

        private Integer ringBufferSizeInHalfOpenState;

        private Integer eventConsumerBufferSize = 100;

        private Boolean registerHealthIndicator = false;

        private Class<Predicate<Throwable>> recordFailurePredicate;

        private Class<? extends Throwable>[] recordExceptions;

        private Class<? extends Throwable>[] ignoreExceptions;

        /**
         * Returns the wait duration in seconds the CircuitBreaker will stay open, before it switches to half closed.
         *
         * @return the wait duration
         */
        public Integer getWaitInterval() {
            return waitInterval;
        }

        /**
         * Sets the wait duration in seconds the CircuitBreaker should stay open, before it switches to half closed.
         *
         * @param waitInterval the wait duration
         */
        public void setWaitInterval(Integer waitInterval) {
            this.waitInterval = waitInterval;
        }

        /**
         * Returns the failure rate threshold for the circuit breaker as percentage.
         *
         * @return the failure rate threshold
         */
        public Integer getFailureRateThreshold() {
            return failureRateThreshold;
        }

        /**
         * Sets the failure rate threshold for the circuit breaker as percentage.
         *
         * @param failureRateThreshold the failure rate threshold
         */
        public void setFailureRateThreshold(Integer failureRateThreshold) {
            this.failureRateThreshold = failureRateThreshold;
        }

        /**
         * Returns the ring buffer size for the circuit breaker while in closed state.
         *
         * @return the ring buffer size
         */
        public Integer getRingBufferSizeInClosedState() {
            return ringBufferSizeInClosedState;
        }

        /**
         * Sets the ring buffer size for the circuit breaker while in closed state.
         *
         * @param ringBufferSizeInClosedState the ring buffer size
         */
        public void setRingBufferSizeInClosedState(Integer ringBufferSizeInClosedState) {
            this.ringBufferSizeInClosedState = ringBufferSizeInClosedState;
        }

        /**
         * Returns the ring buffer size for the circuit breaker while in half open state.
         *
         * @return the ring buffer size
         */
        public Integer getRingBufferSizeInHalfOpenState() {
            return ringBufferSizeInHalfOpenState;
        }

        /**
         * Sets the ring buffer size for the circuit breaker while in half open state.
         *
         * @param ringBufferSizeInHalfOpenState the ring buffer size
         */
        public void setRingBufferSizeInHalfOpenState(Integer ringBufferSizeInHalfOpenState) {
            this.ringBufferSizeInHalfOpenState = ringBufferSizeInHalfOpenState;
        }

        public Integer getEventConsumerBufferSize() {
            return eventConsumerBufferSize;
        }

        public void setEventConsumerBufferSize(Integer eventConsumerBufferSize) {
            this.eventConsumerBufferSize = eventConsumerBufferSize;
        }

        public Boolean getRegisterHealthIndicator() {
            return registerHealthIndicator;
        }

        public void setRegisterHealthIndicator(Boolean registerHealthIndicator) {
            this.registerHealthIndicator = registerHealthIndicator;
        }

        public Class<Predicate<Throwable>> getRecordFailurePredicate() {
            return recordFailurePredicate;
        }

        public void setRecordFailurePredicate(Class<Predicate<Throwable>> recordFailurePredicate) {
            this.recordFailurePredicate = recordFailurePredicate;
        }

        public Class<? extends Throwable>[] getRecordExceptions() {
            return recordExceptions;
        }

        public void setRecordExceptions(Class<? extends Throwable>[] recordExceptions) {
            this.recordExceptions = recordExceptions;
        }

        public Class<? extends Throwable>[] getIgnoreExceptions() {
            return ignoreExceptions;
        }

        public void setIgnoreExceptions(Class<? extends Throwable>[] ignoreExceptions) {
            this.ignoreExceptions = ignoreExceptions;
        }
    }

}
