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
