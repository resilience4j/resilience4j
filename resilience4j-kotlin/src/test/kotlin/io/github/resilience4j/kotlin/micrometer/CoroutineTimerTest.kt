/*
 *
 *  Copyright 2023: Mariusz Kopylec
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
import io.github.resilience4j.micrometer.TimerAssertions.thenFailureTimed
import io.github.resilience4j.micrometer.TimerAssertions.thenSuccessTimed
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.BDDAssertions.failBecauseExceptionWasNotThrown
import org.assertj.core.api.BDDAssertions.then
import org.junit.Test

class CoroutineTimerTest {

    @Test
    fun `should time successful coroutine`() {
        runBlocking {
            val message = "Hello!"
            val registry: MeterRegistry = SimpleMeterRegistry()
            val timer = Timer.of("timer 1", registry)
            val result = timer.executeSuspendFunction {
                delay(0)
                message
            }

            then(result).isEqualTo(message)
            thenSuccessTimed(registry, timer)
        }
    }

    @Test
    fun `should time failed coroutine`() {
        runBlocking {
            val exception = IllegalStateException()
            val registry: MeterRegistry = SimpleMeterRegistry()
            val timer = Timer.of("timer 1", registry, TimerConfig {
                onFailureTagResolver {
                    then(it).isEqualTo(exception)
                    it.toString()
                }
            })
            try {
                timer.executeSuspendFunction {
                    delay(0)
                    throw exception
                }

                failBecauseExceptionWasNotThrown<Nothing>(exception::class.java)
            } catch (e: Exception) {
                thenFailureTimed(registry, timer, e)
            }
        }
    }
}
