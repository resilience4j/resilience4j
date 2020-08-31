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
package io.github.resilience4j.kotlin.timelimiter

import io.github.resilience4j.kotlin.isCancellation
import io.github.resilience4j.timelimiter.TimeLimiter
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.coroutineContext

/**
 * Decorates and executes the given suspend function [block].
 *
 * This is an alias for [withTimeout], reading the timeout from the receiver's
 * [timeLimiterConfig][TimeLimiter.getTimeLimiterConfig].  Specifically, this means:
 *
 * 1. On timeout, a [TimeoutCancellationException] is raised, rather than a TimeoutException as with methods for
 *    non-suspending functions.
 * 1. When a timeout occurs, the coroutine is cancelled, rather than the thread being interrupted as with methods for
 *    non-suspending functions.
 * 1. After the timeout, the given block can only be stopped at a cancellable suspending function call.
 * 1. The `cancelRunningFuture` configuration setting is ignored - on timeout, the suspend function is always cancelled
 *    even if the `cancelRunningFuture` is set to `false`.
 */
suspend fun <T> TimeLimiter.executeSuspendFunction(block: suspend () -> T): T =
    try {
        withTimeout(timeLimiterConfig.timeoutDuration.toMillis()) {
            block().also { onSuccess() }
        }
    } catch (t: Throwable) {
        if (isCancellation(coroutineContext, t)) {
            onError(t)
        } else {
            onSuccess()
        }
        throw t
    }


/**
 * Decorates the given suspend function [block] and returns it.
 *
 * This uses [withTimeout], reading the timeout from the receiver's
 * [timeLimiterConfig][TimeLimiter.getTimeLimiterConfig].  Specifically, this means:
 *
 * 1. On timeout, a [TimeoutCancellationException] is raised, rather than a TimeoutException as with methods for
 *    non-suspending functions.
 * 1. When a timeout occurs, the coroutine is cancelled, rather than the thread being interrupted as with methods for
 *    non-suspending functions.
 * 1. After the timeout, the given block can only be stopped at a cancellable suspending function call.
 * 1. The `cancelRunningFuture` configuration setting is ignored - on timeout, the suspend function is always cancelled
 *    even if the `cancelRunningFuture` is set to `false`.
 */
fun <T> TimeLimiter.decorateSuspendFunction(block: suspend () -> T): suspend () -> T = {
    executeSuspendFunction(block)
}
