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

import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig
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

/**
 * Configures a [ThreadPoolBulkheadRegistry] with a custom default ThreadPoolBulkhead configuration.
 *
 * ```kotlin
 * val bulkheadRegistry = ThreadPoolBulkheadRegistry {
 *     withThreadPoolBulkheadConfig {
 *         maxThreadPoolSize(8)
 *         queueCapacity(10)
 *         keepAliveDuration(Duration.ofSeconds(1))
 *     }
 * }
 * ```
 *
 * @param config methods of [ThreadPoolBulkheadConfig.Builder] that customize the default `ThreadPoolBulkheadConfig`
 */
inline fun ThreadPoolBulkheadRegistry.Builder.withThreadPoolBulkheadConfig(
    config: ThreadPoolBulkheadConfig.Builder.() -> Unit
) {
    withThreadPoolBulkheadConfig(ThreadPoolBulkheadConfig(config))
}

/**
 * Configures a [ThreadPoolBulkheadRegistry] with a custom default ThreadPoolBulkhead configuration.
 *
 * ```kotlin
 * val bulkheadRegistry = ThreadPoolBulkheadRegistry {
 *     withThreadPoolBulkheadConfig(baseThreadPoolBulkheadConfig) {
 *         maxThreadPoolSize(8)
 *         queueCapacity(10)
 *         keepAliveDuration(Duration.ofSeconds(1))
 *     }
 * }
 * ```
 *
 * @param baseConfig base `ThreadPoolBulkheadConfig`
 * @param config methods of [ThreadPoolBulkheadConfig.Builder] that customize the default `ThreadPoolBulkheadConfig`
 */
inline fun ThreadPoolBulkheadRegistry.Builder.withThreadPoolBulkheadConfig(
    baseConfig: ThreadPoolBulkheadConfig,
    config: ThreadPoolBulkheadConfig.Builder.() -> Unit
) {
    withThreadPoolBulkheadConfig(ThreadPoolBulkheadConfig(baseConfig, config))
}

/**
 * Configures a [ThreadPoolBulkheadRegistry] with a custom ThreadPoolBulkhead configuration.
 *
 * ```kotlin
 * val bulkheadRegistry = ThreadPoolBulkheadRegistry {
 *     addThreadPoolBulkheadConfig("sharedConfig1") {
 *         maxThreadPoolSize(8)
 *         queueCapacity(10)
 *         keepAliveDuration(Duration.ofSeconds(1))
 *     }
 * }
 * ```
 *
 * @param configName configName for a custom shared ThreadPoolBulkhead configuration
 * @param config methods of [ThreadPoolBulkheadConfig.Builder] that customize resulting `ThreadPoolBulkheadConfig`
 */
inline fun ThreadPoolBulkheadRegistry.Builder.addThreadPoolBulkheadConfig(
    configName: String,
    config: ThreadPoolBulkheadConfig.Builder.() -> Unit
) {
    addThreadPoolBulkheadConfig(configName, ThreadPoolBulkheadConfig(config))
}

/**
 * Configures a [ThreadPoolBulkheadRegistry] with a custom ThreadPoolBulkhead configuration.
 *
 * ```kotlin
 * val bulkheadRegistry = ThreadPoolBulkheadRegistry {
 *     addThreadPoolBulkheadConfig("sharedConfig1", baseThreadPoolBulkheadConfig) {
 *         maxThreadPoolSize(8)
 *         queueCapacity(10)
 *         keepAliveDuration(Duration.ofSeconds(1))
 *     }
 * }
 * ```
 *
 * @param configName configName for a custom shared ThreadPoolBulkhead configuration
 * @param baseConfig base `ThreadPoolBulkheadConfig`
 * @param config methods of [ThreadPoolBulkheadConfig.Builder] that customize resulting `ThreadPoolBulkheadConfig`
 */
inline fun ThreadPoolBulkheadRegistry.Builder.addThreadPoolBulkheadConfig(
    configName: String,
    baseConfig: ThreadPoolBulkheadConfig,
    config: ThreadPoolBulkheadConfig.Builder.() -> Unit
) {
    addThreadPoolBulkheadConfig(configName, ThreadPoolBulkheadConfig(baseConfig, config))
}

/**
 * Configures a [ThreadPoolBulkheadRegistry] with Tags.
 *
 * Tags added to the registry will be added to every instance created by this registry.
 *
 * @param tags default tags to add to the registry.
 */
fun ThreadPoolBulkheadRegistry.Builder.withTags(tags: Map<String, String>) {
    withTags(tags)
}

/**
 * Configures a [ThreadPoolBulkheadRegistry] with Tags.
 *
 * Tags added to the registry will be added to every instance created by this registry.
 *
 * @param tags default tags to add to the registry.
 */
fun ThreadPoolBulkheadRegistry.Builder.withTags(vararg tags: Pair<String, String>) {
    withTags(HashMap(tags.toMap()))
}
