package io.github.resilience4j.kotlin.bulkhead

import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.bulkhead.BulkheadConfig

/**
 * Decorates and executes the given suspend function [block].
 *
 * If [BulkheadConfig.maxWaitTime] is non-zero, *blocks* until the max wait time is reached or permission is obtained.
 * For this reason, it is not recommended to use this extension function with Bulkheads with non-zero max wait times.
 */
suspend fun <T> Bulkhead.executeSuspendFunction(block: suspend () -> T): T {
    obtainPermission()
    return try {
        block()
    } finally {
        onComplete()
    }
}

/**
 * Decorates the given suspend function [block] and returns it.
 *
 * If [BulkheadConfig.maxWaitTime] is non-zero, *blocks* until the max wait time is reached or permission is obtained.
 * For this reason, it is not recommended to use this extension function with Bulkheads with non-zero max wait times.
 */
fun <T> Bulkhead.decorateSuspendFunction(block: suspend () -> T): suspend () -> T = {
    executeSuspendFunction(block)
}