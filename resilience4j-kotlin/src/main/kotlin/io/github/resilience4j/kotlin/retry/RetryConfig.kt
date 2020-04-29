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

/**
 * Creates new custom [RetryConfig].
 *
 * ```kotlin
 * val retryConfig = RetryConfig<String> {
 *     waitDuration(Duration.ofMillis(10))
 *     retryOnResult { result -> result == "ERROR" }
 * }
 * ```
 *
 * @param config methods of [RetryConfig.Builder] that customize resulting `RetryConfig`
 * @param T input parameter type of `retryOnResult` predicate
 */
inline fun <T> RetryConfig(
    config: RetryConfig.Builder<T>.() -> Unit
): RetryConfig {
    return RetryConfig.custom<T>().apply(config).build()
}

/**
 * Creates new custom [RetryConfig].
 *
 * ```kotlin
 * val retryConfig = RetryConfig {
 *     waitDuration(Duration.ofMillis(10))
 * }
 * ```
 *
 * @param config methods of [RetryConfig.Builder] that customize resulting `RetryConfig`
 */
@JvmName("UntypedRetryConfig")
inline fun RetryConfig(
    config: RetryConfig.Builder<Any?>.() -> Unit
): RetryConfig {
    return RetryConfig.custom<Any?>().apply(config).build()
}

/**
 * Creates new custom [RetryConfig] based on [baseConfig].
 *
 * ```kotlin
 * val retryConfig = RetryConfig<String>(baseRetryConfig) {
 *     waitDuration(Duration.ofMillis(10))
 *     retryOnResult { result -> result == "ERROR" }
 * }
 * ```
 *
 * @param baseConfig base `RetryConfig`
 * @param config methods of [RetryConfig.Builder] that customize resulting `RetryConfig`
 * @param T input parameter type of `retryOnResult` predicate
 */
inline fun <T> RetryConfig(
    baseConfig: RetryConfig,
    config: RetryConfig.Builder<T>.() -> Unit
): RetryConfig {
    return RetryConfig.from<T>(baseConfig).apply(config).build()
}

/**
 * Creates new custom [RetryConfig] based on [baseConfig].
 *
 * ```kotlin
 * val retryConfig = RetryConfig(baseRetryConfig) {
 *     waitDuration(Duration.ofMillis(10))
 * }
 * ```
 *
 * @param baseConfig base `RetryConfig`
 * @param config methods of [RetryConfig.Builder] that customize resulting `RetryConfig`
 */
@JvmName("UntypedRetryConfig")
inline fun RetryConfig(
    baseConfig: RetryConfig,
    config: RetryConfig.Builder<Any?>.() -> Unit
): RetryConfig {
    return RetryConfig.from<Any?>(baseConfig).apply(config).build()
}
