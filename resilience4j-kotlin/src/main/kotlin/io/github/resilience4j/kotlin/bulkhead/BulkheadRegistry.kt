@file:Suppress("FunctionName")

package io.github.resilience4j.kotlin.bulkhead

import io.github.resilience4j.bulkhead.BulkheadConfig
import io.github.resilience4j.bulkhead.BulkheadRegistry
import io.vavr.Tuple2 as VavrTuple2
import io.vavr.collection.HashMap as VavrHashMap

/**
 * Creates new custom [BulkheadRegistry].
 *
 * ```kotlin
 * val bulkheadRegistry = BulkheadRegistry {
 *     withBulkheadConfig(defaultConfig)
 *     withTags(commonTags)
 * }
 * ```
 *
 * @param config methods of [BulkheadRegistry.Builder] that customize resulting `BulkheadRegistry`
 */
inline fun BulkheadRegistry(
    config: BulkheadRegistry.Builder.() -> Unit
): BulkheadRegistry {
    return BulkheadRegistry.custom().apply(config).build()
}

/**
 * Configures a [BulkheadRegistry] with a custom default Bulkhead configuration.
 *
 * ```kotlin
 * val bulkheadRegistry = BulkheadRegistry {
 *     withBulkheadConfig {
 *         maxConcurrentCalls(2)
 *         maxWaitDuration(Duration.ZERO)
 *     }
 * }
 * ```
 *
 * @param config methods of [BulkheadConfig.Builder] that customize the default `BulkheadConfig`
 */
inline fun BulkheadRegistry.Builder.withBulkheadConfig(
    config: BulkheadConfig.Builder.() -> Unit
) {
    withBulkheadConfig(BulkheadConfig(config))
}

/**
 * Configures a [BulkheadRegistry] with a custom default Bulkhead configuration.
 *
 * ```kotlin
 * val bulkheadRegistry = BulkheadRegistry {
 *     withBulkheadConfig(baseBulkheadConfig) {
 *         maxConcurrentCalls(2)
 *         maxWaitDuration(Duration.ZERO)
 *     }
 * }
 * ```
 *
 * @param baseConfig base `BulkheadConfig`
 * @param config methods of [BulkheadConfig.Builder] that customize the default `BulkheadConfig`
 */
inline fun BulkheadRegistry.Builder.withBulkheadConfig(
    baseConfig: BulkheadConfig,
    config: BulkheadConfig.Builder.() -> Unit
) {
    withBulkheadConfig(BulkheadConfig(baseConfig, config))
}

/**
 * Configures a [BulkheadRegistry] with a custom Bulkhead configuration.
 *
 * ```kotlin
 * val bulkheadRegistry = BulkheadRegistry {
 *     addBulkheadConfig("sharedConfig1") {
 *         maxConcurrentCalls(2)
 *         maxWaitDuration(Duration.ZERO)
 *     }
 * }
 * ```
 *
 * @param configName configName for a custom shared Bulkhead configuration
 * @param config methods of [BulkheadConfig.Builder] that customize resulting `BulkheadConfig`
 */
inline fun BulkheadRegistry.Builder.addBulkheadConfig(
    configName: String,
    config: BulkheadConfig.Builder.() -> Unit
) {
    addBulkheadConfig(configName, BulkheadConfig(config))
}

/**
 * Configures a [BulkheadRegistry] with a custom Bulkhead configuration.
 *
 * ```kotlin
 * val bulkheadRegistry = BulkheadRegistry {
 *     addBulkheadConfig("sharedConfig1", baseBulkheadConfig) {
 *         maxConcurrentCalls(2)
 *         maxWaitDuration(Duration.ZERO)
 *     }
 * }
 * ```
 *
 * @param configName configName for a custom shared Bulkhead configuration
 * @param baseConfig base `BulkheadConfig`
 * @param config methods of [BulkheadConfig.Builder] that customize resulting `BulkheadConfig`
 */
inline fun BulkheadRegistry.Builder.addBulkheadConfig(
    configName: String,
    baseConfig: BulkheadConfig,
    config: BulkheadConfig.Builder.() -> Unit
) {
    addBulkheadConfig(configName, BulkheadConfig(baseConfig, config))
}

/**
 * Configures a [BulkheadRegistry] with Tags.
 *
 * Tags added to the registry will be added to every instance created by this registry.
 *
 * @param tags default tags to add to the registry.
 */
fun BulkheadRegistry.Builder.withTags(tags: Map<String, String>) {
    withTags(VavrHashMap.ofAll(tags))
}

/**
 * Configures a [BulkheadRegistry] with Tags.
 *
 * Tags added to the registry will be added to every instance created by this registry.
 *
 * @param tags default tags to add to the registry.
 */
fun BulkheadRegistry.Builder.withTags(vararg tags: Pair<String, String>) {
    withTags(VavrHashMap.ofEntries(tags.map { VavrTuple2(it.first, it.second) }))
}
