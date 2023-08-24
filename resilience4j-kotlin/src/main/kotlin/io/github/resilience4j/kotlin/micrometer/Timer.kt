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

/**
 * Decorates and executes the given suspend function [block].
 */
suspend fun <T> Timer.executeSuspendFunction(block: suspend () -> T): T = decorateSuspendFunction(block)()

/**
 * Decorates the given suspend function [block] and returns it.
 */
fun <T> Timer.decorateSuspendFunction(block: suspend () -> T): suspend () -> T = {
    val context = createContext()
    try {
        block().also { context.onSuccess() }
    } catch (e: Throwable) {
        context.onFailure(e)
        throw e
    }
}
