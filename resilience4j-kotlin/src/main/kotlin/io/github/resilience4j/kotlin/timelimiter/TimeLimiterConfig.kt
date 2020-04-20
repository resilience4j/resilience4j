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
