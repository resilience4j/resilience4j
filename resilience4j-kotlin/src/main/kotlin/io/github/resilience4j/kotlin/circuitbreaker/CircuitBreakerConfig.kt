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
