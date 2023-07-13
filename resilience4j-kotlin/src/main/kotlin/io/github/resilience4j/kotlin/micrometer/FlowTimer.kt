/*
 *
 *  Copyright 2023 Mariusz Kopylec
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
package io.github.resilience4j.kotlin.micrometer

import io.github.resilience4j.micrometer.Timer
import io.github.resilience4j.micrometer.Timer.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import java.util.concurrent.ConcurrentHashMap.newKeySet

fun <T> Flow<T>.timer(timer: Timer): Flow<T> {
    var context: Context? = null
    val output = newKeySet<ValueWrapper<T>>()
    return onStart {
        context = timer.createContext()
    }.onEach {
        output += ValueWrapper(it)
    }.onCompletion { throwable ->
        when (throwable) {
            null -> context?.onSuccess(output.map { it.value })
            else -> context?.onFailure(throwable)
        }
    }
}

/**
 * Wraps a value to prevent the same values be treated as equal ones when adding to Set.
 * @param <T> value type
 */
private class ValueWrapper<T>(val value: T?)
