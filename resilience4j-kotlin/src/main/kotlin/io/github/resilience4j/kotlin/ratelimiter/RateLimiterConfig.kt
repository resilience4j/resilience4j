/*
 *
 * Copyright 2020
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
@file:Suppress("FunctionName")

package io.github.resilience4j.kotlin.ratelimiter

import io.github.resilience4j.ratelimiter.RateLimiterConfig

/**
 * Creates new custom [RateLimiterConfig].
 *
 * ```kotlin
 * val rateLimiterConfig = RateLimiterConfig {
 *     limitRefreshPeriod(Duration.ofSeconds(10))
 *     limitForPeriod(10)
 *     timeoutDuration(Duration.ofSeconds(1))
 * }
 * ```
 *
 * @param config methods of [RateLimiterConfig.Builder] that customize resulting `RateLimiterConfig`
 */
inline fun RateLimiterConfig(
    config: RateLimiterConfig.Builder.() -> Unit
): RateLimiterConfig {
    return RateLimiterConfig.custom().apply(config).build()
}

/**
 * Creates new custom [RateLimiterConfig] based on [baseConfig].
 *
 * ```kotlin
 * val rateLimiterConfig = RateLimiterConfig(baseRateLimiterConfig) {
 *     limitRefreshPeriod(Duration.ofSeconds(10))
 *     limitForPeriod(10)
 *     timeoutDuration(Duration.ofSeconds(1))
 * }
 * ```
 *
 * @param baseConfig base `RateLimiterConfig`
 * @param config methods of [RateLimiterConfig.Builder] that customize resulting `RateLimiterConfig`
 */
inline fun RateLimiterConfig(
    baseConfig: RateLimiterConfig,
    config: RateLimiterConfig.Builder.() -> Unit
): RateLimiterConfig {
    return RateLimiterConfig.from(baseConfig).apply(config).build()
}
