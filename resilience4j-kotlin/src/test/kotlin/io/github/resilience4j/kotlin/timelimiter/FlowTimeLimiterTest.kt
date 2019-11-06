/*
 *
 *  Copyright 2019: authors
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
package io.github.resilience4j.kotlin.timelimiter

import io.github.resilience4j.kotlin.HelloWorldService
import io.github.resilience4j.timelimiter.TimeLimiter
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.Test
import java.time.Duration

class FlowTimeLimiterTest {

    @Test
    fun `should execute successful function`() {
        runBlocking {
            val timelimiter = TimeLimiter.ofDefaults()
            val helloWorldService = HelloWorldService()
            val resultList = mutableListOf<String>()

            //When
            flow {
                repeat(3) {
                    emit(helloWorldService.returnHelloWorld() + it)
                }
            }
                .timeLimiter(timelimiter)
                .toList(resultList)

            //Then
            repeat(3) {
                Assertions.assertThat(resultList[it]).isEqualTo("Hello world$it")
            }
            Assertions.assertThat(resultList.size).isEqualTo(3)
            // Then the helloWorldService should be invoked 1 time
            Assertions.assertThat(helloWorldService.invocationCounter).isEqualTo(3)
        }
    }

    @Test
    fun `should execute unsuccessful function`() {
        runBlocking {
            val timelimiter = TimeLimiter.ofDefaults()
            val helloWorldService = HelloWorldService()
            val resultList = mutableListOf<String>()

            //When
            try {
                flow<String> { helloWorldService.throwException() }
                    .timeLimiter(timelimiter)
                    .toList(resultList)

                Assertions.failBecauseExceptionWasNotThrown<Nothing>(IllegalStateException::class.java)
            } catch (e: IllegalStateException) {
                // nothing - proceed
            }

            //Then
            Assertions.assertThat(resultList.size).isEqualTo(0)
            // Then the helloWorldService should be invoked 1 time
            Assertions.assertThat(helloWorldService.invocationCounter).isEqualTo(1)
        }
    }

    @Test
    fun `should cancel operation that times out`() {
        runBlocking {
            val timelimiter = TimeLimiter.of(TimeLimiterConfig.custom().timeoutDuration(Duration.ofMillis(10)).build())

            val helloWorldService = HelloWorldService()
            val resultList = mutableListOf<String>()

            //When
            try {
                flow<String> { helloWorldService.wait() }
                    .timeLimiter(timelimiter)
                    .toList(resultList)
                Assertions.failBecauseExceptionWasNotThrown<Nothing>(CancellationException::class.java)
            } catch (e: CancellationException) {
                // nothing - proceed
            }

            //Then
            Assertions.assertThat(resultList.size).isEqualTo(0)
            // Then the helloWorldService should be invoked 1 time
            Assertions.assertThat(helloWorldService.invocationCounter).isEqualTo(1)
        }
    }
}