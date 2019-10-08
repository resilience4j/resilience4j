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
package io.github.resilience4j.kotlin.circuitbreaker

import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.kotlin.isCancellation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/**
 * Wraps the given flows collection with a permission check on the supplied [circuitBreaker].
 *
 * The events are published to the circuit breaker via the `onCompletion` operator chained to the given flow.
 *
 * When the circuit breaker is OPEN, flow collection throws a [CallNotPermittedException]
 * ```
 * flowOf("a", "b", "c")
 *     .circuitBreaker(circuitBreaker)
 *     .collect { println(it) } // Throws CallNotPermittedException
 * ```
 *
 * Coroutine cancellation (_normal_ and _exceptional_) do not record any events on the circuit breaker
 * and the acquired permission is released.
 *
 */
@UseExperimental(ExperimentalCoroutinesApi::class)
fun <T> Flow<T>.circuitBreaker(circuitBreaker: CircuitBreaker): Flow<T> =
    flow {
        circuitBreaker.acquirePermission()

        val start = System.nanoTime()
        val source = this@circuitBreaker.onCompletion { e ->
            when {
                isCancellation(coroutineContext, e) -> circuitBreaker
                    .releasePermission()

                e == null -> circuitBreaker
                    .onSuccess(System.nanoTime() - start, TimeUnit.NANOSECONDS)

                else -> circuitBreaker
                    .onError(System.nanoTime() - start, TimeUnit.NANOSECONDS, e)
            }
        }

        emitAll(source)
    }

