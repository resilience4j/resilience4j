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
