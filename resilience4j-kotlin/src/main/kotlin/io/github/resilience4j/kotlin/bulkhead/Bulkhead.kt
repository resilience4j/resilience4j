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

/**
 * Decorates and executes the given suspend function [block].
 *
 * If [BulkheadConfig.maxWaitTime] is non-zero, *blocks* until the max wait time is reached or permission is obtained.
 * For this reason, it is not recommended to use this extension function with Bulkheads with non-zero max wait times.
 */
suspend fun <T> Bulkhead.executeSuspendFunction(block: suspend () -> T): T {
    acquirePermission()
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