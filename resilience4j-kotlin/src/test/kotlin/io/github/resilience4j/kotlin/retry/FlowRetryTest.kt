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
package io.github.resilience4j.kotlin.retry

import io.github.resilience4j.kotlin.CoroutineHelloWorldService
import io.github.resilience4j.retry.Retry
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.Test
import java.time.Duration

class FlowRetryTest {
    @Test
    fun `should execute successful function`() {
        runBlocking {
            val retry = Retry.ofDefaults("testName")
            val metrics = retry.metrics
            val helloWorldService = CoroutineHelloWorldService()
            val resultList = mutableListOf<String>()

            //When
            flow {
                repeat(3) {
                    emit(helloWorldService.returnHelloWorld() + it)
                }
            }
                .retry(retry)
                .toList(resultList)


            //Then
            repeat(3) {
                Assertions.assertThat(resultList[it]).isEqualTo("Hello world$it")
            }
            Assertions.assertThat(resultList.size).isEqualTo(3)
            Assertions.assertThat(metrics.numberOfSuccessfulCallsWithoutRetryAttempt).isEqualTo(1)
            Assertions.assertThat(metrics.numberOfSuccessfulCallsWithRetryAttempt).isEqualTo(0)
            Assertions.assertThat(metrics.numberOfFailedCallsWithoutRetryAttempt).isEqualTo(0)
            Assertions.assertThat(metrics.numberOfFailedCallsWithRetryAttempt).isEqualTo(0)
            // Then the helloWorldService should be invoked 1 time
            Assertions.assertThat(helloWorldService.invocationCounter).isEqualTo(3)
        }
    }

    @Test
    fun `should execute function with retries`() {
        runBlocking {
            val retry = Retry.of("testName") {
                RetryConfig { waitDuration(Duration.ofMillis(10)) }
            }
            val metrics = retry.metrics
            val helloWorldService = CoroutineHelloWorldService()
            val resultList = mutableListOf<String>()

            //When
            flow {
                repeat(3) {
                    when (helloWorldService.invocationCounter) {
                        0 -> helloWorldService.throwException()
                        else -> emit(helloWorldService.returnHelloWorld() + it)
                    }
                }
            }
                .retry(retry)
                .toList(resultList)


            //Then
            repeat(3) {
                Assertions.assertThat(resultList[it]).isEqualTo("Hello world$it")
            }
            Assertions.assertThat(resultList.size).isEqualTo(3)
            Assertions.assertThat(metrics.numberOfSuccessfulCallsWithoutRetryAttempt).isEqualTo(0)
            Assertions.assertThat(metrics.numberOfSuccessfulCallsWithRetryAttempt).isEqualTo(1)
            Assertions.assertThat(metrics.numberOfFailedCallsWithoutRetryAttempt).isEqualTo(0)
            Assertions.assertThat(metrics.numberOfFailedCallsWithRetryAttempt).isEqualTo(0)
            // Then the helloWorldService should be invoked twice
            Assertions.assertThat(helloWorldService.invocationCounter).isEqualTo(4)
        }
    }

    @Test
    fun `should execute function with retry of result`() {
        runBlocking {
            val helloWorldService = CoroutineHelloWorldService()
            val resultList = mutableListOf<String>()
            val retry = Retry.of("testName") {
                RetryConfig {
                    waitDuration(Duration.ofMillis(10))
                    retryOnResult { helloWorldService.invocationCounter < 2 }
                }
            }
            val metrics = retry.metrics

            //When
            flow { emit(helloWorldService.returnHelloWorld()) }
                .retry(retry)
                .toList(resultList)

            //Then
            Assertions.assertThat(resultList.size).isEqualTo(1)
            Assertions.assertThat(resultList[0]).isEqualTo("Hello world")
            Assertions.assertThat(metrics.numberOfSuccessfulCallsWithoutRetryAttempt).isEqualTo(0)
            Assertions.assertThat(metrics.numberOfSuccessfulCallsWithRetryAttempt).isEqualTo(1)
            Assertions.assertThat(metrics.numberOfFailedCallsWithoutRetryAttempt).isEqualTo(0)
            Assertions.assertThat(metrics.numberOfFailedCallsWithRetryAttempt).isEqualTo(0)
            // Then the helloWorldService should be invoked twice
            Assertions.assertThat(helloWorldService.invocationCounter).isEqualTo(2)
        }
    }

    @Test
    fun `should execute function with repeated failures`() {
        runBlocking {
            val retry = Retry.of("testName") {
                RetryConfig { waitDuration(Duration.ofMillis(10)) }
            }
            val metrics = retry.metrics
            val helloWorldService = CoroutineHelloWorldService()
            val resultList = mutableListOf<String>()

            //When
            try {
                retry.executeSuspendFunction {
                    helloWorldService.throwException()
                }
                //When
                flow<String> { helloWorldService.throwException() }
                    .retry(retry)
                    .toList(resultList)
                Assertions.failBecauseExceptionWasNotThrown<Nothing>(IllegalStateException::class.java)
            } catch (e: IllegalStateException) {
                // nothing - proceed
            }

            //Then
            Assertions.assertThat(resultList).isEmpty()
            Assertions.assertThat(metrics.numberOfSuccessfulCallsWithoutRetryAttempt).isEqualTo(0)
            Assertions.assertThat(metrics.numberOfSuccessfulCallsWithRetryAttempt).isEqualTo(0)
            Assertions.assertThat(metrics.numberOfFailedCallsWithoutRetryAttempt).isEqualTo(0)
            Assertions.assertThat(metrics.numberOfFailedCallsWithRetryAttempt).isEqualTo(1)
            // Then the helloWorldService should be invoked the maximum number of times
            Assertions.assertThat(helloWorldService.invocationCounter).isEqualTo(retry.retryConfig.maxAttempts)
        }
    }
}
