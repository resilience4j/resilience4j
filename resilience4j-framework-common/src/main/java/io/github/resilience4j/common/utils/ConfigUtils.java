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

import io.github.resilience4j.common.bulkhead.configuration.CommonBulkheadConfigurationProperties;
import io.github.resilience4j.common.bulkhead.configuration.CommonThreadPoolBulkheadConfigurationProperties;
import io.github.resilience4j.common.circuitbreaker.configuration.CommonCircuitBreakerConfigurationProperties;
import io.github.resilience4j.common.micrometer.configuration.CommonTimerConfigurationProperties;
import io.github.resilience4j.common.ratelimiter.configuration.CommonRateLimiterConfigurationProperties;
import io.github.resilience4j.common.retry.configuration.CommonRetryConfigurationProperties;
import io.github.resilience4j.common.timelimiter.configuration.CommonTimeLimiterConfigurationProperties;

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
        CommonCircuitBreakerConfigurationProperties.InstanceProperties instanceProperties,
        CommonCircuitBreakerConfigurationProperties.InstanceProperties baseProperties) {
        if (instanceProperties.getRegisterHealthIndicator() == null &&
            baseProperties.getRegisterHealthIndicator() != null) {
            instanceProperties.setRegisterHealthIndicator(baseProperties.getRegisterHealthIndicator());
        }
        if (instanceProperties.getAllowHealthIndicatorToFail() == null &&
            baseProperties.getAllowHealthIndicatorToFail() != null) {
            instanceProperties.setAllowHealthIndicatorToFail(baseProperties.getAllowHealthIndicatorToFail());
        }
        if (instanceProperties.getEventConsumerBufferSize() == null &&
            baseProperties.getEventConsumerBufferSize() != null) {
            instanceProperties.setEventConsumerBufferSize(baseProperties.getEventConsumerBufferSize());
        }
    }

    /**
     * merge only properties that are not part of retry config if any match the conditions of merge
     *
     * @param baseProperties     base config properties
     * @param instanceProperties instance properties
     */
    public static void mergePropertiesIfAny(
        CommonBulkheadConfigurationProperties.InstanceProperties baseProperties,
        CommonBulkheadConfigurationProperties.InstanceProperties instanceProperties) {
        if (instanceProperties.getEventConsumerBufferSize() == null &&
            baseProperties.getEventConsumerBufferSize() != null) {
                instanceProperties.setEventConsumerBufferSize(baseProperties.getEventConsumerBufferSize());
        }
    }

    /**
     * merge only properties that are not part of retry config if any match the conditions of merge
     *
     * @param baseProperties     base config properties
     * @param instanceProperties instance properties
     */
    public static void mergePropertiesIfAny(
        CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties baseProperties,
        CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties instanceProperties) {
        if (instanceProperties.getEventConsumerBufferSize() == null &&
            baseProperties.getEventConsumerBufferSize() != null) {
                instanceProperties.setEventConsumerBufferSize(baseProperties.getEventConsumerBufferSize());
        }
    }

    /**
     * merge only properties that are not part of retry config if any match the conditions of merge
     *
     * @param baseProperties     base config properties
     * @param instanceProperties instance properties
     */
    public static void mergePropertiesIfAny(
        CommonRateLimiterConfigurationProperties.InstanceProperties baseProperties,
        CommonRateLimiterConfigurationProperties.InstanceProperties instanceProperties) {
        if (instanceProperties.getRegisterHealthIndicator() == null &&
            baseProperties.getRegisterHealthIndicator() != null) {
            instanceProperties.setRegisterHealthIndicator(baseProperties.getRegisterHealthIndicator());
        }
        if (instanceProperties.getAllowHealthIndicatorToFail() == null &&
            baseProperties.getAllowHealthIndicatorToFail() != null) {
            instanceProperties.setAllowHealthIndicatorToFail(baseProperties.getAllowHealthIndicatorToFail());
        }
        if (instanceProperties.getSubscribeForEvents() == null &&
            baseProperties.getSubscribeForEvents() != null) {
            instanceProperties.setSubscribeForEvents(baseProperties.getSubscribeForEvents());
        }
        if (instanceProperties.getEventConsumerBufferSize() == null &&
            baseProperties.getEventConsumerBufferSize() != null) {
            instanceProperties.setEventConsumerBufferSize(baseProperties.getEventConsumerBufferSize());
        }
    }

    /**
     * merge only properties that are not part of retry config if any match the conditions of merge
     *
     * @param baseProperties     base config properties
     * @param instanceProperties instance properties
     */
    public static void mergePropertiesIfAny(
        CommonRetryConfigurationProperties.InstanceProperties baseProperties,
        CommonRetryConfigurationProperties.InstanceProperties instanceProperties) {
        if (instanceProperties.getEnableExponentialBackoff() == null &&
            baseProperties.getEnableExponentialBackoff() != null) {
            instanceProperties.setEnableExponentialBackoff(baseProperties.getEnableExponentialBackoff());
        }
        if (instanceProperties.getEnableRandomizedWait() == null &&
            baseProperties.getEnableRandomizedWait() != null) {
            instanceProperties.setEnableRandomizedWait(baseProperties.getEnableRandomizedWait());
        }
        if (instanceProperties.getExponentialBackoffMultiplier() == null &&
            baseProperties.getExponentialBackoffMultiplier() != null) {
            instanceProperties.setExponentialBackoffMultiplier(baseProperties.getExponentialBackoffMultiplier());
        }
        if (instanceProperties.getExponentialMaxWaitDuration() == null &&
            baseProperties.getExponentialMaxWaitDuration() != null) {
            instanceProperties.setExponentialMaxWaitDuration(baseProperties.getExponentialMaxWaitDuration());
        }
    }

	/**
	 * merge only properties that are not part of timeLimiter config if any match the conditions of merge
	 *
	 * @param baseProperties     base config properties
	 * @param instanceProperties instance properties
	 */
	public static void mergePropertiesIfAny(CommonTimeLimiterConfigurationProperties.InstanceProperties baseProperties,
											CommonTimeLimiterConfigurationProperties.InstanceProperties instanceProperties) {
		if (instanceProperties.getEventConsumerBufferSize() == null
            && baseProperties.getEventConsumerBufferSize() != null) {
            instanceProperties.setEventConsumerBufferSize(baseProperties.getEventConsumerBufferSize());
		}
	}

    /**
	 * merge only properties that are not part of timer config if any match the conditions of merge
	 *
	 * @param baseProperties     base config properties
	 * @param instanceProperties instance properties
	 */
	public static void mergePropertiesIfAny(CommonTimerConfigurationProperties.InstanceProperties baseProperties,
                                            CommonTimerConfigurationProperties.InstanceProperties instanceProperties) {
		if (instanceProperties.getEventConsumerBufferSize() == null
            && baseProperties.getEventConsumerBufferSize() != null) {
            instanceProperties.setEventConsumerBufferSize(baseProperties.getEventConsumerBufferSize());
		}
	}
}
