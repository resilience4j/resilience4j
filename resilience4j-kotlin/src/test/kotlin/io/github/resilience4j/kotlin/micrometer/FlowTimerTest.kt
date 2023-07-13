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
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.BDDAssertions.failBecauseExceptionWasNotThrown
import org.assertj.core.api.BDDAssertions.then
import org.junit.Test

class FlowTimerTest {

    @Test
    fun `should time successful, non-empty flow`() {
        runBlocking {
            val messages = listOf("Hello! 1", "Hello! 1", "Hello! 2")
            val registry: MeterRegistry = SimpleMeterRegistry()
            val timer = Timer.of("timer 1", registry, TimerConfig<List<String>> {
                successResultNameResolver {
                    then(it).containsExactlyInAnyOrderElementsOf(messages)
                    it.size.toString()
                }
            })
            val output = messages.asFlow()
                .timer(timer)
                .toList()

            then(output).containsExactlyInAnyOrderElementsOf(messages)
            thenSuccessTimed(registry, timer, output)
        }
    }

    @Test
    fun `should time successful, empty flow`() {
        runBlocking {
            val registry: MeterRegistry = SimpleMeterRegistry()
            val timer = Timer.of("timer 1", registry, TimerConfig<List<String>> {
                successResultNameResolver {
                    then(it).isEmpty()
                    it.size.toString()
                }
            })
            val output = flowOf<Any>()
                .timer(timer)
                .toList()

            then(output).isEmpty()
            thenSuccessTimed(registry, timer, output)
        }
    }

    @Test
    fun `should time failed flow`() {
        runBlocking {
            val exception = IllegalStateException()
            val registry: MeterRegistry = SimpleMeterRegistry()
            val timer = Timer.of("timer 1", registry, TimerConfig<String> {
                failureResultNameResolver {
                    then(it).isEqualTo(exception)
                    it.toString()
                }
            })
            try {
                flow<Any> {
                    delay(0)
                    throw exception
                }.timer(timer).toList()

                failBecauseExceptionWasNotThrown<Nothing>(exception::class.java)
            } catch (e: Exception) {
                thenFailureTimed(registry, timer, e)
            }
        }
    }
}
