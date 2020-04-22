@file:Suppress("FunctionName")

package io.github.resilience4j.kotlin.retry

import io.github.resilience4j.retry.RetryRegistry

/**
 * Creates new custom [RetryRegistry].
 *
 * ```kotlin
 * val retryRegistry = RetryRegistry {
 *     withRetryConfig(defaultConfig)
 *     withTags(commonTags)
 * }
 * ```
 *
 * @param config methods of [RetryRegistry.Builder] that customize resulting `RetryRegistry`
 */
inline fun RetryRegistry(
    config: RetryRegistry.Builder.() -> Unit
): RetryRegistry {
    return RetryRegistry.custom().apply(config).build()
}
