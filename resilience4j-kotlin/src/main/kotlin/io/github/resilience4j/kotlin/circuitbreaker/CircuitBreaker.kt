/*
 *
 *  Copyright 2019: Guido Pio Mariotti, Brad Newman
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.kotlin.circuitbreaker

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.kotlin.isCancellation
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * Decorates and executes the given suspend function [block].
 * Cancellation errors are ignored.
 */
suspend fun <T> CircuitBreaker.executeSuspendFunction(
    block: suspend () -> T
): T = executeSuspendFunction({ t, c -> isCancellation(c, t) }, block)

/**
 * Decorates and executes the given suspend function [block].
 * Release permission without recording error if [ignoreThrowablePredicate] is true.
 */
@Suppress("UsePropertyAccessSyntax") // current timestamp is not a property of CB
suspend fun <T> CircuitBreaker.executeSuspendFunction(
    ignoreThrowablePredicate: (Throwable, CoroutineContext) -> Boolean,
    block: suspend () -> T
): T {
    acquirePermission()
    val start = getCurrentTimestamp()
    try {
        val result = block()
        val durationInNanos = getCurrentTimestamp() - start
        onResult(durationInNanos, TimeUnit.NANOSECONDS, result)
        return result
    } catch (exception: Throwable) {
        val shouldIgnore =
            try { ignoreThrowablePredicate(exception, coroutineContext) } catch (_: Exception) { false }
        if (shouldIgnore) {
            releasePermission()
        } else {
            val durationInNanos = getCurrentTimestamp() - start
            onError(durationInNanos, TimeUnit.NANOSECONDS, exception)
        }
        throw exception
    }
}

/**
 * Decorates and executes the given suspend function [block].
 * All types of throwable including cancellation exceptions are recorded with [CircuitBreaker.onError]
 */
suspend fun <T> CircuitBreaker.executeSuspendFunctionAndRecordCancellationError(block: suspend () -> T): T =
    executeSuspendFunction({ _, _ -> false }, block)


/**
 * Decorates the given *suspend* function [block] and returns it.
 */
fun <T> CircuitBreaker.decorateSuspendFunction(block: suspend () -> T): suspend () -> T = {
    executeSuspendFunction(block)
}

/**
 * Decorates and executes the given function [block].
 */
fun <T> CircuitBreaker.executeFunction(block: () -> T): T {
    return this.decorateCallable(block).call()
}

/**
 * Decorates the given function [block] and returns it.
 */
fun <T> CircuitBreaker.decorateFunction(block: () -> T): () -> T = {
    executeFunction(block)
}
