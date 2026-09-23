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
package io.github.resilience4j.kotlin.bulkhead

import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.bulkhead.BulkheadConfig
import io.github.resilience4j.kotlin.isCancellation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.future.await
import kotlin.coroutines.coroutineContext

/**
 * Decorates and executes the given suspend function [block].
 *
 * If [BulkheadConfig.maxWaitDuration] is non-zero, *blocks* until the max wait time is reached or permission is obtained.
 * For this reason, it is not recommended to use this extension function with Bulkheads with non-zero max wait times.
 */
suspend fun <T> Bulkhead.executeSuspendFunction(block: suspend () -> T): T {
    acquirePermissionSuspend()
    return try {
        block().also { onComplete() }
    } catch (e: Throwable) {
        if (isCancellation(coroutineContext, e)) {
            releasePermission()
        } else {
            onComplete()
        }
        throw e
    }
}

/**
 * Decorates the given function [block] and returns it.
 *
 * If [BulkheadConfig.maxWaitDuration] is non-zero, *blocks* until the max wait time is reached or permission is obtained.
 * For this reason, it is not recommended to use this extension function with Bulkheads with non-zero max wait times.
 */
fun <T> Bulkhead.decorateFunction(block: () -> T): () -> T = {
    executeFunction(block)
}

/**
 * Decorates and executes the given function [block].
 *
 * If [BulkheadConfig.maxWaitDuration] is non-zero, *blocks* until the max wait time is reached or permission is obtained.
 * For this reason, it is not recommended to use this extension function with Bulkheads with non-zero max wait times.
 */
fun <T> Bulkhead.executeFunction(block: () -> T): T {
    return this.executeCallable(block)
}

/**
 * Decorates the given suspend function [block] and returns it.
 *
 * If [BulkheadConfig.maxWaitDuration] is non-zero, *blocks* until the max wait time is reached or permission is obtained.
 * For this reason, it is not recommended to use this extension function with Bulkheads with non-zero max wait times.
 */
fun <T> Bulkhead.decorateSuspendFunction(block: suspend () -> T): suspend () -> T = {
    executeSuspendFunction(block)
}

/**
 * Acquires a permission without blocking a thread. When the bulkhead is full and a max wait
 * duration is configured, the request is queued and this coroutine suspends until the permission
 * is granted or the wait duration has elapsed, which fails with a
 * [io.github.resilience4j.bulkhead.BulkheadFullException]. Cancelling the coroutine withdraws the
 * queued request.
 */
internal suspend fun Bulkhead.acquirePermissionSuspend() {
    val permission = acquirePermissionAsync()
    try {
        permission.await()
    } catch (e: CancellationException) {
        // The wait was cancelled. A permission granted concurrently has to be handed back; the grant
        // may still complete the future after this point, so release from its completion.
        permission.whenComplete { _, failure -> if (failure == null) releasePermission() }
        throw e
    }
}
