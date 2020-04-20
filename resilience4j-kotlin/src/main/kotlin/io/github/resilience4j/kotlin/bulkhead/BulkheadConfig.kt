@file:Suppress("FunctionName")

package io.github.resilience4j.kotlin.bulkhead

import io.github.resilience4j.bulkhead.BulkheadConfig

/**
 * Creates new custom [BulkheadConfig].
 *
 * ```kotlin
 * val bulkheadConfig = BulkheadConfig {
 *     maxConcurrentCalls(1)
 *     maxWaitDuration(Duration.ZERO)
 * }
 * ```
 *
 * @param config methods of [BulkheadConfig.Builder] that customize resulting `BulkheadConfig`
 */
inline fun BulkheadConfig(
    config: BulkheadConfig.Builder.() -> Unit
): BulkheadConfig {
    return BulkheadConfig.custom().apply(config).build()
}

/**
 * Creates new custom [BulkheadConfig] based on [baseConfig].
 *
 * ```kotlin
 * val bulkheadConfig = BulkheadConfig(baseBulkheadConfig) {
 *     maxConcurrentCalls(1)
 *     maxWaitDuration(Duration.ZERO)
 * }
 * ```
 *
 * @param baseConfig base `BulkheadConfig`
 * @param config methods of [BulkheadConfig.Builder] that customize resulting `BulkheadConfig`
 */
inline fun BulkheadConfig(
    baseConfig: BulkheadConfig,
    config: BulkheadConfig.Builder.() -> Unit
): BulkheadConfig {
    return BulkheadConfig.from(baseConfig).apply(config).build()
}
