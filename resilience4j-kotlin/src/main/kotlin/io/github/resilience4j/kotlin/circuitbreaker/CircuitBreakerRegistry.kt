@file:Suppress("FunctionName")

package io.github.resilience4j.kotlin.circuitbreaker

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
