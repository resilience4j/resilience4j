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
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry

/**
 * Creates new custom [CircuitBreakerRegistry].
 *
 * ```kotlin
 * val circuitBreakerRegistry = CircuitBreakerRegistry {
 *     withCircuitBreaker(defaultConfig)
 *     withTags(commonTags)
 * }
 * ```
 *
 * @param config methods of [CircuitBreakerRegistry.Builder] that customize resulting `CircuitBreakerRegistry`
 */
inline fun CircuitBreakerRegistry(
    config: CircuitBreakerRegistry.Builder.() -> Unit
): CircuitBreakerRegistry {
    return CircuitBreakerRegistry.custom().apply(config).build()
}

/**
 * Configures a [CircuitBreakerRegistry] with a custom default CircuitBreaker configuration.
 *
 * ```kotlin
 * val circuitBreakerRegistry = CircuitBreakerRegistry {
 *     withCircuitBreakerConfig {
 *         failureRateThreshold(50)
 *         waitDurationInOpenState(Duration.ofSeconds(30))
 *     }
 * }
 * ```
 *
 * @param config methods of [CircuitBreakerConfig.Builder] that customize the default `CircuitBreakerConfig`
 */
inline fun CircuitBreakerRegistry.Builder.withCircuitBreakerConfig(
    config: CircuitBreakerConfig.Builder.() -> Unit
) {
    withCircuitBreakerConfig(CircuitBreakerConfig(config))
}

/**
 * Configures a [CircuitBreakerRegistry] with a custom default CircuitBreaker configuration.
 *
 * ```kotlin
 * val circuitBreakerRegistry = CircuitBreakerRegistry {
 *     withCircuitBreakerConfig(baseCircuitBreakerConfig) {
 *         failureRateThreshold(50)
 *         waitDurationInOpenState(Duration.ofSeconds(30))
 *     }
 * }
 * ```
 *
 * @param baseConfig base `CircuitBreakerConfig`
 * @param config methods of [CircuitBreakerConfig.Builder] that customize the default `CircuitBreakerConfig`
 */
inline fun CircuitBreakerRegistry.Builder.withCircuitBreakerConfig(
    baseConfig: CircuitBreakerConfig,
    config: CircuitBreakerConfig.Builder.() -> Unit
) {
    withCircuitBreakerConfig(CircuitBreakerConfig(baseConfig, config))
}

/**
 * Configures a [CircuitBreakerRegistry] with a custom CircuitBreaker configuration.
 *
 * ```kotlin
 * val circuitBreakerRegistry = CircuitBreakerRegistry {
 *     addCircuitBreakerConfig("sharedConfig1") {
 *         failureRateThreshold(50)
 *         waitDurationInOpenState(Duration.ofSeconds(30))
 *     }
 * }
 * ```
 *
 * @param configName configName for a custom shared CircuitBreaker configuration
 * @param config methods of [CircuitBreakerConfig.Builder] that customize resulting `CircuitBreakerConfig`
 */
inline fun CircuitBreakerRegistry.Builder.addCircuitBreakerConfig(
    configName: String,
    config: CircuitBreakerConfig.Builder.() -> Unit
) {
    addCircuitBreakerConfig(configName, CircuitBreakerConfig(config))
}

/**
 * Configures a [CircuitBreakerRegistry] with a custom CircuitBreaker configuration.
 *
 * ```kotlin
 * val circuitBreakerRegistry = CircuitBreakerRegistry {
 *     addCircuitBreakerConfig("sharedConfig1", baseCircuitBreakerConfig) {
 *         failureRateThreshold(50)
 *         waitDurationInOpenState(Duration.ofSeconds(30))
 *     }
 * }
 * ```
 *
 * @param configName configName for a custom shared CircuitBreaker configuration
 * @param baseConfig base `CircuitBreakerConfig`
 * @param config methods of [CircuitBreakerConfig.Builder] that customize resulting `CircuitBreakerConfig`
 */
inline fun CircuitBreakerRegistry.Builder.addCircuitBreakerConfig(
    configName: String,
    baseConfig: CircuitBreakerConfig,
    config: CircuitBreakerConfig.Builder.() -> Unit
) {
    addCircuitBreakerConfig(configName, CircuitBreakerConfig(baseConfig, config))
}

/**
 * Configures a [CircuitBreakerRegistry] with Tags.
 *
 * Tags added to the registry will be added to every instance created by this registry.
 *
 * @param tags default tags to add to the registry.
 */
fun CircuitBreakerRegistry.Builder.withTags(tags: Map<String, String>) {
    withTags(tags)
}

/**
 * Configures a [CircuitBreakerRegistry] with Tags.
 *
 * Tags added to the registry will be added to every instance created by this registry.
 *
 * @param tags default tags to add to the registry.
 */
fun CircuitBreakerRegistry.Builder.withTags(vararg tags: Pair<String, String>) {
    withTags(HashMap(tags.toMap()))
}
