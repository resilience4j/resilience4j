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
package io.github.resilience4j.kotlin.circuitbreaker

import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.kotlin.CoroutineHelloWorldService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CoroutineCircuitBreakerTest {
    @Test
    fun `should execute successful function`() {
        runBlocking {
            val circuitBreaker = CircuitBreaker.ofDefaults("testName")
            val metrics = circuitBreaker.metrics
            assertThat(metrics.numberOfBufferedCalls).isZero()
            val helloWorldService = CoroutineHelloWorldService()

            //When
            val result = circuitBreaker.executeSuspendFunction {
                helloWorldService.returnHelloWorld()
            }

            //Then
            assertThat(result).isEqualTo("Hello world")
            assertThat(metrics.numberOfBufferedCalls).isEqualTo(1)
            assertThat(metrics.numberOfFailedCalls).isZero()
            assertThat(metrics.numberOfSuccessfulCalls).isEqualTo(1)
            // Then the helloWorldService should be invoked 1 time
            assertThat(helloWorldService.invocationCounter).isEqualTo(1)
        }
    }

    @Test
    fun `should not execute function when open`() {
        runBlocking {
            val circuitBreaker = CircuitBreaker.ofDefaults("testName")
            circuitBreaker.transitionToOpenState()
            val metrics = circuitBreaker.metrics
            assertThat(metrics.numberOfBufferedCalls).isZero()
            val helloWorldService = CoroutineHelloWorldService()

            //When
            try {
                circuitBreaker.executeSuspendFunction {
                    helloWorldService.returnHelloWorld()
                }
                Assertions.failBecauseExceptionWasNotThrown<Nothing>(CallNotPermittedException::class.java)
            } catch (e: CallNotPermittedException) {
                // nothing - proceed
            }

            //Then
            assertThat(metrics.numberOfBufferedCalls).isZero()
            assertThat(metrics.numberOfFailedCalls).isZero()
            assertThat(metrics.numberOfSuccessfulCalls).isZero()
            assertThat(metrics.numberOfNotPermittedCalls).isEqualTo(1)
            // Then the helloWorldService should not be invoked
            assertThat(helloWorldService.invocationCounter).isZero()
        }
    }

    @Test
    fun `should decorate suspend function and return with success`() {
        runBlocking {
            // Given
            val circuitBreaker = CircuitBreaker.ofDefaults("testName")
            val metrics = circuitBreaker.metrics
            assertThat(metrics.numberOfBufferedCalls).isZero()
            val helloWorldService = CoroutineHelloWorldService()

            //When
            val function = circuitBreaker.decorateSuspendFunction {
                helloWorldService.returnHelloWorld()
            }

            //Then
            assertThat(function()).isEqualTo("Hello world")
            assertThat(metrics.numberOfBufferedCalls).isEqualTo(1)
            assertThat(metrics.numberOfFailedCalls).isZero()
            assertThat(metrics.numberOfSuccessfulCalls).isEqualTo(1)
            // Then the helloWorldService should be invoked 1 time
            assertThat(helloWorldService.invocationCounter).isEqualTo(1)
        }
    }

    @Test
    fun `should decorate suspend function and return an exception`() {
        runBlocking {
            // Given
            val circuitBreaker = CircuitBreaker.ofDefaults("testName")
            val metrics = circuitBreaker.metrics
            assertThat(metrics.numberOfBufferedCalls).isZero()
            val helloWorldService = CoroutineHelloWorldService()

            //When
            val function = circuitBreaker.decorateSuspendFunction {
                helloWorldService.throwException()
            }

            //Then
            try {
                function()
                Assertions.failBecauseExceptionWasNotThrown<Nothing>(IllegalStateException::class.java)
            } catch (e: IllegalStateException) {
                // nothing - proceed
            }
            assertThat(metrics.numberOfBufferedCalls).isEqualTo(1)
            assertThat(metrics.numberOfFailedCalls).isEqualTo(1)
            assertThat(metrics.numberOfSuccessfulCalls).isZero()
            // Then the helloWorldService should be invoked 1 time
            assertThat(helloWorldService.invocationCounter).isEqualTo(1)
        }
    }

    @Test
    fun `executeSuspendFunctionAndRecordCancellationError should record a CancellationException`() {
        testCancellation(
            { executeSuspendFunctionAndRecordCancellationError(it) },
            expectedNumberOfFailedCalls = 1
        )
    }

    @Test
    fun `executeSuspendFunction should ignore a CancellationException`() {
        testCancellation(
            { executeSuspendFunction(it) },
            expectedNumberOfFailedCalls = 0
        )
    }

    private fun testCancellation(
        execute: suspend (CircuitBreaker.(suspend () -> Unit) -> Unit),
        expectedNumberOfFailedCalls: Int
    ) {
        // Given
        val circuitBreaker = CircuitBreaker.ofDefaults("testName")
        val metrics = circuitBreaker.metrics
        assertThat(metrics.numberOfBufferedCalls).isEqualTo(0)
        val helloWorldService = CoroutineHelloWorldService()
        var cancellationException: CancellationException? = null
        //When
        runBlocking {
            val job = launch {
                try {
                    circuitBreaker.execute(helloWorldService::cancel)
                    Assertions.failBecauseExceptionWasNotThrown<Nothing>(CancellationException::class.java)
                } catch (expected: CancellationException) {
                    cancellationException = expected
                }

            }
            job.join()
            //Then
            assertThat(job.isCancelled).isTrue
            assertThat(cancellationException).isNotNull
            assertThat(cancellationException!!.message).isEqualTo("test cancel")
            assertThat(metrics.numberOfBufferedCalls).isEqualTo(expectedNumberOfFailedCalls)
            assertThat(metrics.numberOfFailedCalls).isEqualTo(expectedNumberOfFailedCalls)
            assertThat(metrics.numberOfSuccessfulCalls).isZero()
            // Then the helloWorldService should be invoked 1 time
            assertThat(helloWorldService.invocationCounter).isEqualTo(1)
        }
    }

}
