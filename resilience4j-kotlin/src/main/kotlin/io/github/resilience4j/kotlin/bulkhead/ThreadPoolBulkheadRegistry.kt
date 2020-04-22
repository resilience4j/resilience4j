@file:Suppress("FunctionName")

package io.github.resilience4j.kotlin.bulkhead

import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry

/**
 * Creates new custom [ThreadPoolBulkheadRegistry].
 *
 * ```kotlin
 * val bulkheadRegistry = ThreadPoolBulkheadRegistry {
 *     withThreadPoolBulkheadConfig(defaultConfig)
 *     withTags(commonTags)
 * }
 * ```
 *
 * @param config methods of [ThreadPoolBulkheadRegistry.Builder] that customize resulting `ThreadPoolBulkheadRegistry`
 */
inline fun ThreadPoolBulkheadRegistry(
    config: ThreadPoolBulkheadRegistry.Builder.() -> Unit
): ThreadPoolBulkheadRegistry {
    return ThreadPoolBulkheadRegistry.custom().apply(config).build()
}
