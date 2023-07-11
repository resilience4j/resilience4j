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

import io.github.resilience4j.kotlin.CoroutineHelloWorldService
import io.github.resilience4j.micrometer.Timer
import io.github.resilience4j.micrometer.tagged.TagNames.KIND
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.BDDAssertions.failBecauseExceptionWasNotThrown
import org.assertj.core.api.BDDAssertions.then
import org.junit.Test

class TimerTest {

    @Test
    fun `should time successful suspended operation`() {
        runBlocking {
            val registry: MeterRegistry = SimpleMeterRegistry()
            val timer = Timer.of("timer 1", registry, TimerConfig<String> {
                successResultNameResolver {
                    then(it).isEqualTo("Hello world")
                    it
                }
            })
            val sampleService = CoroutineHelloWorldService()
            val output = timer.executeSuspendFunction {
                sampleService.returnHelloWorld()
            }

            then(output).isEqualTo("Hello world")
            then(sampleService.invocationCounter).isEqualTo(1)
            thenSuccessTimed(registry, timer)
        }
    }

    @Test
    fun `should time failed suspended operation`() {
        runBlocking {
            val registry: MeterRegistry = SimpleMeterRegistry()
            val timer = Timer.of("timer 1", registry, TimerConfig<String> {
                failureResultNameResolver {
                    then(it).isInstanceOf(IllegalStateException::class.java)
                    "fail"
                }
            })
            val sampleService = CoroutineHelloWorldService()
            try {
                timer.executeSuspendFunction {
                    sampleService.throwException()
                }

                failBecauseExceptionWasNotThrown<Nothing>(IllegalStateException::class.java)
            } catch (e: IllegalStateException) {
                // nothing - proceed
            }
            then(sampleService.invocationCounter).isEqualTo(1)
            thenFailureTimed(registry, timer)
        }
    }

    @Test
    fun `should time successful flow operation`() {
        runBlocking {
            val registry: MeterRegistry = SimpleMeterRegistry()
            val timer = Timer.of("timer 1", registry, TimerConfig<Any> {
                successResultNameResolver {
                    then(it).isNull()
                    "success"
                }
            })
            val sampleService = CoroutineHelloWorldService()
            val output = flow {
                repeat(3) {
                    emit(sampleService.returnHelloWorld() + it)
                }
            }.timer(timer).toList()

            then(output).hasSize(3)
            repeat(output.size) {
                then(output[it]).isEqualTo("Hello world$it")
            }
            then(sampleService.invocationCounter).isEqualTo(output.size)
            thenSuccessTimed(registry, timer)
        }
    }

    @Test
    fun `should time failed flow operation`() {
        runBlocking {
            val registry: MeterRegistry = SimpleMeterRegistry()
            val timer = Timer.of("timer 1", registry, TimerConfig<String> {
                failureResultNameResolver {
                    then(it).isInstanceOf(IllegalStateException::class.java)
                    "fail"
                }
            })
            val sampleService = CoroutineHelloWorldService()
            try {
                flow<Any> { sampleService.throwException() }
                    .timer(timer)
                    .toList()

                failBecauseExceptionWasNotThrown<Nothing>(IllegalStateException::class.java)
            } catch (e: IllegalStateException) {
                // nothing - proceed
            }
            then(sampleService.invocationCounter).isEqualTo(1)
            thenFailureTimed(registry, timer)
        }
    }

    private fun thenSuccessTimed(registry: MeterRegistry, timer: Timer) {
        thenTimed(registry, timer, "successful")
    }

    private fun thenFailureTimed(registry: MeterRegistry, timer: Timer) {
        thenTimed(registry, timer, "failed")
    }

    private fun thenTimed(registry: MeterRegistry, timer: Timer, resultKind: String) {
        val meters = registry.meters.filter { it.id.name == timer.timerConfig.metricNames }
        then(meters.size).isEqualTo(1)
        val meter = meters[0] as io.micrometer.core.instrument.Timer
        then(meter.count()).isEqualTo(1)
        then(meter.id.tags).containsOnlyOnce(Tag.of(KIND, resultKind))
        registry.clear()
    }
}
