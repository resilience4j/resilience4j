/*
 *
 *  Copyright 2019 authors
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
import io.github.resilience4j.kotlin.isCancellation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlin.coroutines.coroutineContext

@UseExperimental(ExperimentalCoroutinesApi::class)
fun <T> Flow<T>.bulkhead(bulkhead: Bulkhead): Flow<T> =
    flow {
        bulkhead.acquirePermissionSuspend()

        val source = this@bulkhead.onCompletion { e ->
            if (isCancellation(coroutineContext, e)) {
                bulkhead.releasePermission()
            } else {
                bulkhead.onComplete()
            }
        }
        emitAll(source)
    }