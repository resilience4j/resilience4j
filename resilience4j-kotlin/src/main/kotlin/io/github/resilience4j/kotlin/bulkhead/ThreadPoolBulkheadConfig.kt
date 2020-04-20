@file:Suppress("FunctionName")

package io.github.resilience4j.kotlin.bulkhead

import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig

/**
 * Creates new custom [ThreadPoolBulkheadConfig].
 *
 * ```kotlin
 * val bulkheadConfig = ThreadPoolBulkheadConfig {
 *     maxThreadPoolSize(8)
 *     queueCapacity(10)
 *     keepAliveDuration(Duration.ofSeconds(1))
 * }
 * ```
 *
 * @param config methods of [ThreadPoolBulkheadConfig.Builder] that customize resulting `ThreadPoolBulkheadConfig`
 */
inline fun ThreadPoolBulkheadConfig(
    config: ThreadPoolBulkheadConfig.Builder.() -> Unit
): ThreadPoolBulkheadConfig {
    return ThreadPoolBulkheadConfig.custom().apply(config).build()
}

/**
 * Creates new custom [ThreadPoolBulkheadConfig] based on [baseConfig].
 *
 * ```kotlin
 * val bulkheadConfig = ThreadPoolBulkheadConfig(baseBulkheadConfig) {
 *     maxThreadPoolSize(8)
 *     queueCapacity(10)
 *     keepAliveDuration(Duration.ofSeconds(1))
 * }
 * ```
 *
 * @param baseConfig base `ThreadPoolBulkheadConfig`
 * @param config methods of [ThreadPoolBulkheadConfig.Builder] that customize resulting `ThreadPoolBulkheadConfig`
 */
inline fun ThreadPoolBulkheadConfig(
    baseConfig: ThreadPoolBulkheadConfig,
    config: ThreadPoolBulkheadConfig.Builder.() -> Unit
): ThreadPoolBulkheadConfig {
    return ThreadPoolBulkheadConfig.from(baseConfig).apply(config).build()
}
