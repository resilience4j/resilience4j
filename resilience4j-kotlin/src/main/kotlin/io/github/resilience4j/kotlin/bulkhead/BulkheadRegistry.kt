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

package io.github.resilience4j.kotlin.bulkhead

import io.github.resilience4j.bulkhead.BulkheadConfig
import io.github.resilience4j.bulkhead.BulkheadRegistry

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
    withTags(tags)
}

/**
 * Configures a [BulkheadRegistry] with Tags.
 *
 * Tags added to the registry will be added to every instance created by this registry.
 *
 * @param tags default tags to add to the registry.
 */
fun BulkheadRegistry.Builder.withTags(vararg tags: Pair<String, String>) {
    withTags(HashMap(tags.toMap()))
}
