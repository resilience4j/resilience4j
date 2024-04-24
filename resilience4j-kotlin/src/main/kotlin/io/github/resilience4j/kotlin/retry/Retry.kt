/*
 *
 *  Copyright 2019: Brad Newman
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
package io.github.resilience4j.kotlin.retry

import io.github.resilience4j.retry.Retry
import kotlinx.coroutines.delay

/**
 * Decorates and executes the given suspend function [block].
 *
 * Between attempts, suspends based on the configured interval function.
 */
suspend fun <T> Retry.executeSuspendFunction(block: suspend () -> T): T {
    val retryContext = asyncContext<T>()
    while (true) {
        try {
            val result = block()
            val delayMs = retryContext.onResult(result)
            if (delayMs < 0) {
                retryContext.onComplete()
                return result
            } else {
                delay(delayMs)
            }
        } catch (e: Exception) {
            val delayMs = retryContext.onError(e)
            if (delayMs < 0) {
                throw e
            } else {
                delay(delayMs)
            }
        }
    }
}

/**
 * Decorates the given function [block] and returns it.
 *
 * Between attempts, suspends based on the configured interval function.
 */
fun <T> Retry.decorateSuspendFunction(block: suspend () -> T): suspend () -> T = {
    executeSuspendFunction(block)
}

/**
 * Decorates the given function [block] and returns it.
 *
 * Between attempts, suspends based on the configured interval function.
 */
fun <T> Retry.decorateFunction(block: () -> T): () -> T = {
    executeFunction(block)
}

/**
 * Decorates and executes the given function [block].
 *
 * Between attempts, suspends based on the configured interval function.
 */
fun <T> Retry.executeFunction(block: () -> T): T {
    return this.executeCallable(block)
}
