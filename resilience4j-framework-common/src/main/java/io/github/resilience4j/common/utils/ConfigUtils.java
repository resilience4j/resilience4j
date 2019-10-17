/*
 * Copyright 2019 Mahmoud Romeh
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
package io.github.resilience4j.common.utils;

import io.github.resilience4j.common.bulkhead.configuration.BulkheadConfigurationProperties;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties;
import io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties;
import io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties;

/**
 * resilience4j configuration util
 */
public class ConfigUtils {

    private ConfigUtils() {
    }

    /**
     * merge only properties that are not part of retry config if any match the conditions of merge
     *
     * @param baseProperties     base config properties
     * @param instanceProperties instance properties
     */
    public static void mergePropertiesIfAny(
        CircuitBreakerConfigurationProperties.InstanceProperties instanceProperties,
        CircuitBreakerConfigurationProperties.InstanceProperties baseProperties) {
        if (instanceProperties.getRegisterHealthIndicator() == null) {
            if (baseProperties.getRegisterHealthIndicator() != null) {
                instanceProperties
                    .setRegisterHealthIndicator(baseProperties.getRegisterHealthIndicator());
            }
        }
        if (instanceProperties.getEventConsumerBufferSize() == null) {
            if (baseProperties.getEventConsumerBufferSize() != null) {
                instanceProperties
                    .setEventConsumerBufferSize(baseProperties.getEventConsumerBufferSize());
            }
        }
    }

    /**
     * merge only properties that are not part of retry config if any match the conditions of merge
     *
     * @param baseProperties     base config properties
     * @param instanceProperties instance properties
     */
    public static void mergePropertiesIfAny(
        BulkheadConfigurationProperties.InstanceProperties baseProperties,
        BulkheadConfigurationProperties.InstanceProperties instanceProperties) {
        if (instanceProperties.getEventConsumerBufferSize() == null) {
            if (baseProperties.getEventConsumerBufferSize() != null) {
                instanceProperties
                    .setEventConsumerBufferSize(baseProperties.getEventConsumerBufferSize());
            }
        }
    }

    /**
     * merge only properties that are not part of retry config if any match the conditions of merge
     *
     * @param baseProperties     base config properties
     * @param instanceProperties instance properties
     */
    public static void mergePropertiesIfAny(
        RateLimiterConfigurationProperties.InstanceProperties baseProperties,
        RateLimiterConfigurationProperties.InstanceProperties instanceProperties) {
        if (instanceProperties.getRegisterHealthIndicator() == null) {
            if (baseProperties.getRegisterHealthIndicator() != null) {
                instanceProperties
                    .setRegisterHealthIndicator(baseProperties.getRegisterHealthIndicator());
            }
        }
        if (instanceProperties.getSubscribeForEvents() == null) {
            if (baseProperties.getSubscribeForEvents() != null) {
                instanceProperties.setSubscribeForEvents(baseProperties.getSubscribeForEvents());
            }
        }
        if (instanceProperties.getEventConsumerBufferSize() == null) {
            if (baseProperties.getEventConsumerBufferSize() != null) {
                instanceProperties
                    .setEventConsumerBufferSize(baseProperties.getEventConsumerBufferSize());
            }
        }
    }

    /**
     * merge only properties that are not part of retry config if any match the conditions of merge
     *
     * @param baseProperties     base config properties
     * @param instanceProperties instance properties
     */
    public static void mergePropertiesIfAny(
        RetryConfigurationProperties.InstanceProperties baseProperties,
        RetryConfigurationProperties.InstanceProperties instanceProperties) {
        if (instanceProperties.getEnableExponentialBackoff() == null) {
            if (baseProperties.getEnableExponentialBackoff() != null) {
                instanceProperties
                    .setEnableExponentialBackoff(baseProperties.getEnableExponentialBackoff());
            }
        }
        if (instanceProperties.getEnableRandomizedWait() == null) {
            if (baseProperties.getEnableRandomizedWait() != null) {
                instanceProperties
                    .setEnableRandomizedWait(baseProperties.getEnableRandomizedWait());
            }
        }
        if (instanceProperties.getExponentialBackoffMultiplier() == null) {
            if (baseProperties.getExponentialBackoffMultiplier() != null) {
                instanceProperties.setExponentialBackoffMultiplier(
                    baseProperties.getExponentialBackoffMultiplier());
            }
        }
    }

}
