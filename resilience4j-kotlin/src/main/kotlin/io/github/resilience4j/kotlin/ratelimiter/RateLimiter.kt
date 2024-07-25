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
package io.github.resilience4j.kotlin.ratelimiter

import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RequestNotPermitted
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

/**
 * Decorates and executes the given suspend function [block].
 *
 * If [RateLimiterConfig.timeoutDuration] is non-zero, the returned function suspends until a permission is available.
 */
suspend fun <T> RateLimiter.executeSuspendFunction(block: suspend () -> T): T {
    awaitPermission()

    try {
        val result = block()
        onResult(result)
        return result
    } catch (e: Throwable) {
        onError(e)
        throw e
    }
}

/**
 * Decorates the given suspend function [block] and returns it.
 *
 * If [RateLimiterConfig.timeoutDuration] is non-zero, the returned function suspends until a permission is available.
 */
fun <T> RateLimiter.decorateSuspendFunction(block: suspend () -> T): suspend () -> T = {
    executeSuspendFunction(block)
}

/**
 * Decorates and executes the given function [block].
 *
 * If [RateLimiterConfig.timeoutDuration] is non-zero, the returned function suspends until a permission is available.
 */
fun <T> RateLimiter.executeFunction(block: () -> T): T {
    return this.executeCallable(block)
}

/**
 * Decorates the given function [block] and returns it.
 *
 * If [RateLimiterConfig.timeoutDuration] is non-zero, the returned function suspends until a permission is available.
 */
fun <T> RateLimiter.decorateFunction(block: () -> T): () -> T = {
    executeFunction(block)
}

internal suspend fun RateLimiter.awaitPermission() {
    val waitTimeNs = reservePermission()
    when {
        waitTimeNs > 0 -> delay(TimeUnit.NANOSECONDS.toMillis(waitTimeNs))
        waitTimeNs < 0 -> throw RequestNotPermitted.createRequestNotPermitted(this)
    }
}
