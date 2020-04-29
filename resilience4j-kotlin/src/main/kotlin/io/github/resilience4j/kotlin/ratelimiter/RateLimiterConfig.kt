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
