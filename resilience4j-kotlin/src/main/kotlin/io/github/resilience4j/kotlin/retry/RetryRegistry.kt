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

package io.github.resilience4j.kotlin.retry

import io.github.resilience4j.retry.RetryConfig
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

/**
 * Configures a [RetryRegistry] with a custom default Retry configuration.
 *
 * ```kotlin
 * val retryRegistry = RetryRegistry {
 *     withRetryConfig<String> {
 *         waitDuration(Duration.ofMillis(10))
 *         retryOnResult { result -> result == "ERROR" }
 *     }
 * }
 * ```
 *
 * @param config methods of [RetryConfig.Builder] that customize default `RetryConfig`
 */
inline fun <T> RetryRegistry.Builder.withRetryConfig(
    config: RetryConfig.Builder<T>.() -> Unit
) {
    withRetryConfig(RetryConfig(config))
}

/**
 * Configures a [RetryRegistry] with a custom default Retry configuration.
 *
 * ```kotlin
 * val retryRegistry = RetryRegistry {
 *     withRetryConfig {
 *         waitDuration(Duration.ofMillis(10))
 *         retryOnResult { result -> result == "ERROR" }
 *     }
 * }
 * ```
 *
 * @param config methods of [RetryConfig.Builder] that customize default `RetryConfig`
 */
@JvmName("withUntypedRetryConfig")
inline fun RetryRegistry.Builder.withRetryConfig(
    config: RetryConfig.Builder<Any?>.() -> Unit
) {
    withRetryConfig(RetryConfig(config))
}

/**
 * Configures a [RetryRegistry] with a custom default Retry configuration.
 *
 * ```kotlin
 * val retryRegistry = RetryRegistry {
 *     withRetryConfig<String>(baseRetryConfig) {
 *         waitDuration(Duration.ofMillis(10))
 *         retryOnResult { result -> result == "ERROR" }
 *     }
 * }
 * ```
 *
 * @param baseConfig base `RetryConfig`
 * @param config methods of [RetryConfig.Builder] that customize default `RetryConfig`
 */
inline fun <T> RetryRegistry.Builder.withRetryConfig(
    baseConfig: RetryConfig,
    config: RetryConfig.Builder<T>.() -> Unit
) {
    withRetryConfig(RetryConfig(baseConfig, config))
}

/**
 * Configures a [RetryRegistry] with a custom default Retry configuration.
 *
 * ```kotlin
 * val retryRegistry = RetryRegistry {
 *     withRetryConfig(baseRetryConfig) {
 *         waitDuration(Duration.ofMillis(10))
 *     }
 * }
 * ```
 *
 * @param baseConfig base `RetryConfig`
 * @param config methods of [RetryConfig.Builder] that customize default `RetryConfig`
 */
@JvmName("withUntypedRetryConfig")
inline fun RetryRegistry.Builder.withRetryConfig(
    baseConfig: RetryConfig,
    config: RetryConfig.Builder<Any?>.() -> Unit
) {
    withRetryConfig(RetryConfig(baseConfig, config))
}

/**
 * Configures a [RetryRegistry] with a custom Retry configuration.
 *
 * ```kotlin
 * val retryRegistry = RetryRegistry {
 *     addRetryConfig<String>("sharedConfig1") {
 *         waitDuration(Duration.ofMillis(10))
 *         retryOnResult { result -> result == "ERROR" }
 *     }
 * }
 * ```
 *
 * @param configName configName for a custom shared Retry configuration
 * @param config methods of [RetryConfig.Builder] that customize resulting `RetryConfig`
 */
inline fun <T> RetryRegistry.Builder.addRetryConfig(
    configName: String,
    config: RetryConfig.Builder<T>.() -> Unit
) {
    addRetryConfig(configName, RetryConfig(config))
}

/**
 * Configures a [RetryRegistry] with a custom Retry configuration.
 *
 * ```kotlin
 * val retryRegistry = RetryRegistry {
 *     addRetryConfig("sharedConfig1") {
 *         waitDuration(Duration.ofMillis(10))
 *     }
 * }
 * ```
 *
 * @param configName configName for a custom shared Retry configuration
 * @param config methods of [RetryConfig.Builder] that customize resulting `RetryConfig`
 */
@JvmName("addUntypedRetryConfig")
inline fun RetryRegistry.Builder.addRetryConfig(
    configName: String,
    config: RetryConfig.Builder<Any?>.() -> Unit
) {
    addRetryConfig(configName, RetryConfig(config))
}

/**
 * Configures a [RetryRegistry] with a custom Retry configuration.
 *
 * ```kotlin
 * val retryRegistry = RetryRegistry {
 *     addRetryConfig<String>("sharedConfig1", baseRetryConfig) {
 *         waitDuration(Duration.ofMillis(10))
 *         retryOnResult { result -> result == "ERROR" }
 *     }
 * }
 * ```
 *
 * @param configName configName for a custom shared Retry configuration
 * @param baseConfig base `RetryConfig`
 * @param config methods of [RetryConfig.Builder] that customize resulting `RetryConfig`
 */
inline fun <T> RetryRegistry.Builder.addRetryConfig(
    configName: String,
    baseConfig: RetryConfig,
    config: RetryConfig.Builder<T>.() -> Unit
) {
    addRetryConfig(configName, RetryConfig(baseConfig, config))
}

/**
 * Configures a [RetryRegistry] with a custom Retry configuration.
 *
 * ```kotlin
 * val retryRegistry = RetryRegistry {
 *     addRetryConfig("sharedConfig1", baseRetryConfig) {
 *         waitDuration(Duration.ofMillis(10))
 *     }
 * }
 * ```
 *
 * @param configName configName for a custom shared Retry configuration
 * @param baseConfig base `RetryConfig`
 * @param config methods of [RetryConfig.Builder] that customize resulting `RetryConfig`
 */
@JvmName("addUntypedRetryConfig")
inline fun RetryRegistry.Builder.addRetryConfig(
    configName: String,
    baseConfig: RetryConfig,
    config: RetryConfig.Builder<Any?>.() -> Unit
) {
    addRetryConfig(configName, RetryConfig(baseConfig, config))
}

/**
 * Configures a [RetryRegistry] with Tags.
 *
 * Tags added to the registry will be added to every instance created by this registry.
 *
 * @param tags default tags to add to the registry.
 */
fun RetryRegistry.Builder.withTags(tags: Map<String, String>) {
    withTags(tags)
}

/**
 * Configures a [RetryRegistry] with Tags.
 *
 * Tags added to the registry will be added to every instance created by this registry.
 *
 * @param tags default tags to add to the registry.
 */
fun RetryRegistry.Builder.withTags(vararg tags: Pair<String, String>) {
    withTags(HashMap(tags.toMap()))
}
