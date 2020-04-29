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

package io.github.resilience4j.kotlin.circuitbreaker

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig

/**
 * Creates new custom [CircuitBreakerConfig].
 *
 * ```kotlin
 * val circuitBreakerConfig = CircuitBreakerConfig {
 *     failureRateThreshold(50)
 *     waitDurationInOpenState(Duration.ofSeconds(30))
 * }
 * ```
 *
 * @param config methods of [CircuitBreakerConfig.Builder] that customize resulting `CircuitBreakerConfig`
 */
inline fun CircuitBreakerConfig(
    config: CircuitBreakerConfig.Builder.() -> Unit
): CircuitBreakerConfig {
    return CircuitBreakerConfig.custom().apply(config).build()
}

/**
 * Creates new custom [CircuitBreakerConfig] based on [baseConfig].
 *
 * ```kotlin
 * val circuitBreakerConfig = CircuitBreakerConfig(baseCircuitBreakerConfig) {
 *     failureRateThreshold(50)
 *     waitDurationInOpenState(Duration.ofSeconds(30))
 * }
 * ```
 *
 * @param baseConfig base `CircuitBreakerConfig`
 * @param config methods of [CircuitBreakerConfig.Builder] that customize resulting `CircuitBreakerConfig`
 */
inline fun CircuitBreakerConfig(
    baseConfig: CircuitBreakerConfig,
    config: CircuitBreakerConfig.Builder.() -> Unit
): CircuitBreakerConfig {
    return CircuitBreakerConfig.from(baseConfig).apply(config).build()
}
