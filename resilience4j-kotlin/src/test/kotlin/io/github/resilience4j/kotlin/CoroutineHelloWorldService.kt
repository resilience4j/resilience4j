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
package io.github.resilience4j.kotlin

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay

class CoroutineHelloWorldService {
    var invocationCounter = 0
        private set

    private val sync = Channel<Unit>(Channel.UNLIMITED)

    suspend fun returnHelloWorld(): String {
        delay(0) // so tests are fast, but compiler agrees suspend modifier is required
        invocationCounter++
        return "Hello world"
    }

    suspend fun throwException() {
        delay(0) // so tests are fast, but compiler agrees suspend modifier is required
        invocationCounter++
        error("test exception")
    }

    /**
     * Suspend until a matching [proceed] call.
     */
    suspend fun wait() {
        invocationCounter++
        sync.receive()
    }

    /**
     * Allow a call into [wait] to proceed.
     */
    fun proceed() = sync.offer(Unit)
}
