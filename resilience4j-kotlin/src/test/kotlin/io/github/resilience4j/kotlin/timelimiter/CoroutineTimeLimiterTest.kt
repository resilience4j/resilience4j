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
package io.github.resilience4j.kotlin.timelimiter

import io.github.resilience4j.kotlin.CoroutineHelloWorldService
import io.github.resilience4j.timelimiter.TimeLimiter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.Test
import java.time.Duration

class CoroutineTimeLimiterTest {
    @Test
    fun `should execute successful function`() {
        runBlocking {
            val timelimiter = TimeLimiter.ofDefaults()
            val helloWorldService = CoroutineHelloWorldService()

            //When
            val result = timelimiter.executeSuspendFunction {
                helloWorldService.returnHelloWorld()
            }

            //Then
            Assertions.assertThat(result).isEqualTo("Hello world")
            // Then the helloWorldService should be invoked 1 time
            Assertions.assertThat(helloWorldService.invocationCounter).isEqualTo(1)
        }
    }

    @Test
    fun `should execute unsuccessful function`() {
        runBlocking {
            val timelimiter = TimeLimiter.ofDefaults()
            val helloWorldService = CoroutineHelloWorldService()

            //When
            try {
                timelimiter.executeSuspendFunction {
                    helloWorldService.throwException()
                }
                Assertions.failBecauseExceptionWasNotThrown<Nothing>(IllegalStateException::class.java)
            } catch (e: IllegalStateException) {
                // nothing - proceed
            }

            //Then
            // Then the helloWorldService should be invoked 1 time
            Assertions.assertThat(helloWorldService.invocationCounter).isEqualTo(1)
        }
    }

    @Test
    fun `should cancel operation that times out`() {
        runBlocking {
            val timelimiter = TimeLimiter.of(TimeLimiterConfig { timeoutDuration(Duration.ofMillis(10)) })

            val helloWorldService = CoroutineHelloWorldService()

            //When
            try {
                timelimiter.executeSuspendFunction {
                    helloWorldService.wait()
                }
                Assertions.failBecauseExceptionWasNotThrown<Nothing>(CancellationException::class.java)
            } catch (e: CancellationException) {
                // nothing - proceed
            }

            //Then
            // Then the helloWorldService should be invoked 1 time
            Assertions.assertThat(helloWorldService.invocationCounter).isEqualTo(1)
        }
    }

    @Test
    fun `should decorate successful function`() {
        runBlocking {
            val timelimiter = TimeLimiter.ofDefaults()
            val helloWorldService = CoroutineHelloWorldService()

            //When
            val function = timelimiter.decorateSuspendFunction {
                helloWorldService.returnHelloWorld()
            }

            //Then
            Assertions.assertThat(function()).isEqualTo("Hello world")
            // Then the helloWorldService should be invoked 1 time
            Assertions.assertThat(helloWorldService.invocationCounter).isEqualTo(1)
        }
    }
}
