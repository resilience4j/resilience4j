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

package io.github.resilience4j.kotlin.timelimiter

import io.github.resilience4j.timelimiter.TimeLimiterConfig

/**
 * Creates new custom [TimeLimiterConfig].
 *
 * ```kotlin
 * val timeLimiterConfig = TimeLimiterConfig {
 *     timeoutDuration(Duration.ofMillis(10))
 * }
 * ```
 *
 * @param config methods of [TimeLimiterConfig.Builder] that customize resulting `TimeLimiterConfig`
 */
inline fun TimeLimiterConfig(
    config: TimeLimiterConfig.Builder.() -> Unit
): TimeLimiterConfig {
    return TimeLimiterConfig.custom().apply(config).build()
}

/**
 * Creates new custom [TimeLimiterConfig] based on [baseConfig].
 *
 * ```kotlin
 * val timeLimiterConfig = TimeLimiterConfig(baseTimeLimiterConfig) {
 *     timeoutDuration(Duration.ofMillis(10))
 * }
 * ```
 *
 * @param baseConfig base `TimeLimiterConfig`
 * @param config methods of [TimeLimiterConfig.Builder] that customize resulting `TimeLimiterConfig`
 */
inline fun TimeLimiterConfig(
    baseConfig: TimeLimiterConfig,
    config: TimeLimiterConfig.Builder.() -> Unit
): TimeLimiterConfig {
    return TimeLimiterConfig.from(baseConfig).apply(config).build()
}
