@file:Suppress("FunctionName")

package io.github.resilience4j.kotlin.ratelimiter

import io.github.resilience4j.ratelimiter.RateLimiterRegistry

/**
 * Creates new custom [RateLimiterRegistry].
 *
 * ```kotlin
 * val rateLimiterRegistry = RateLimiterRegistry {
 *     withRateLimiterConfig(defaultConfig)
 *     withTags(commonTags)
 * }
 * ```
 *
 * @param config methods of [RateLimiterRegistry.Builder] that customize resulting `RateLimiterRegistry`
 */
inline fun RateLimiterRegistry(
    config: RateLimiterRegistry.Builder.() -> Unit
): RateLimiterRegistry {
    return RateLimiterRegistry.custom().apply(config).build()
}
