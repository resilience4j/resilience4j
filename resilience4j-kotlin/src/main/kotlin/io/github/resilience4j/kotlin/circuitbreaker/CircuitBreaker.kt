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
import kotlin.coroutines.coroutineContext

/**
 * Decorates and executes the given suspend function [block].
 */
suspend fun <T> CircuitBreaker.executeSuspendFunction(block: suspend () -> T): T {
    acquirePermission()
    val start = System.nanoTime()
    try {
        val result = block()
        val durationInNanos = System.nanoTime() - start
        onSuccess(durationInNanos, TimeUnit.NANOSECONDS)
        return result
    } catch (exception: Throwable) {
        if (isCancellation(coroutineContext, exception)) {
            releasePermission()
        } else {
            val durationInNanos = System.nanoTime() - start
            onError(durationInNanos, TimeUnit.NANOSECONDS, exception)
        }
        throw exception
    }
}

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
