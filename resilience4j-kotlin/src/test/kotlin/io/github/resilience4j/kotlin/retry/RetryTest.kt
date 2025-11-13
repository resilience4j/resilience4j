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

import io.github.resilience4j.kotlin.HelloWorldService
import io.github.resilience4j.retry.Retry
import org.assertj.core.api.Assertions
import org.junit.Test
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.BiConsumer
import java.util.function.Predicate

class RetryTest {

    @Test
    fun `should execute successful function`() {
        val retry = Retry.ofDefaults("testName")
        val metrics = retry.metrics
        val helloWorldService = HelloWorldService()

        //When
        val result = retry.executeFunction {
            helloWorldService.returnHelloWorld()
        }

        //Then
        Assertions.assertThat(result).isEqualTo("Hello world")
        Assertions.assertThat(metrics.numberOfSuccessfulCallsWithoutRetryAttempt).isEqualTo(1)
        Assertions.assertThat(metrics.numberOfSuccessfulCallsWithRetryAttempt).isZero()
        Assertions.assertThat(metrics.numberOfFailedCallsWithoutRetryAttempt).isZero()
        Assertions.assertThat(metrics.numberOfFailedCallsWithRetryAttempt).isZero()
        // Then the helloWorldService should be invoked 1 time
        Assertions.assertThat(helloWorldService.invocationCounter).isEqualTo(1)
    }

    @Test
    fun `should execute function with retries`() {
        val retry = Retry.of("testName") {
            RetryConfig { waitDuration(Duration.ofMillis(10)) }
        }
        val metrics = retry.metrics
        val helloWorldService = HelloWorldService()

        //When
        val result = retry.executeFunction {
            when (helloWorldService.invocationCounter) {
                0 -> helloWorldService.throwException()
                else -> helloWorldService.returnHelloWorld()
            }
        }

        //Then
        Assertions.assertThat(result).isEqualTo("Hello world")
        Assertions.assertThat(metrics.numberOfSuccessfulCallsWithoutRetryAttempt).isZero()
        Assertions.assertThat(metrics.numberOfSuccessfulCallsWithRetryAttempt).isEqualTo(1)
        Assertions.assertThat(metrics.numberOfFailedCallsWithoutRetryAttempt).isZero()
        Assertions.assertThat(metrics.numberOfFailedCallsWithRetryAttempt).isZero()
        // Then the helloWorldService should be invoked twice
        Assertions.assertThat(helloWorldService.invocationCounter).isEqualTo(2)
    }

    @Test
    fun `should execute function with retry of result`() {
        val helloWorldService = HelloWorldService()
        val retry = Retry.of("testName") {
            RetryConfig {
                waitDuration(Duration.ofMillis(10))
                retryOnResult { helloWorldService.invocationCounter < 2 }
            }
        }
        val metrics = retry.metrics

        //When
        val result = retry.executeFunction {
            helloWorldService.returnHelloWorld()
        }

        //Then
        Assertions.assertThat(result).isEqualTo("Hello world")
        Assertions.assertThat(metrics.numberOfSuccessfulCallsWithoutRetryAttempt).isZero()
        Assertions.assertThat(metrics.numberOfSuccessfulCallsWithRetryAttempt).isEqualTo(1)
        Assertions.assertThat(metrics.numberOfFailedCallsWithoutRetryAttempt).isZero()
        Assertions.assertThat(metrics.numberOfFailedCallsWithRetryAttempt).isZero()
        // Then the helloWorldService should be invoked twice
        Assertions.assertThat(helloWorldService.invocationCounter).isEqualTo(2)
    }

    @Test
    fun `should execute function with repeated failures`() {
        val retry = Retry.of("testName") {
            RetryConfig { waitDuration(Duration.ofMillis(10)) }
        }
        val metrics = retry.metrics
        val helloWorldService = HelloWorldService()

        //When
        try {
            retry.executeFunction {
                helloWorldService.throwException()
            }
            Assertions.failBecauseExceptionWasNotThrown<Nothing>(IllegalStateException::class.java)
        } catch (e: IllegalStateException) {
            // nothing - proceed
        }

        //Then
        Assertions.assertThat(metrics.numberOfSuccessfulCallsWithoutRetryAttempt).isZero()
        Assertions.assertThat(metrics.numberOfSuccessfulCallsWithRetryAttempt).isZero()
        Assertions.assertThat(metrics.numberOfFailedCallsWithoutRetryAttempt).isZero()
        Assertions.assertThat(metrics.numberOfFailedCallsWithRetryAttempt).isEqualTo(1)
        // Then the helloWorldService should be invoked the maximum number of times
        Assertions.assertThat(helloWorldService.invocationCounter).isEqualTo(retry.retryConfig.maxAttempts)
    }

    @Test
    fun `should decorate successful function`() {
        val retry = Retry.ofDefaults("testName")
        val metrics = retry.metrics
        val helloWorldService = HelloWorldService()

        //When
        val function = retry.decorateFunction {
            helloWorldService.returnHelloWorld()
        }

        //Then
        Assertions.assertThat(function()).isEqualTo("Hello world")
        Assertions.assertThat(metrics.numberOfSuccessfulCallsWithoutRetryAttempt).isEqualTo(1)
        Assertions.assertThat(metrics.numberOfSuccessfulCallsWithRetryAttempt).isZero()
        Assertions.assertThat(metrics.numberOfFailedCallsWithoutRetryAttempt).isZero()
        Assertions.assertThat(metrics.numberOfFailedCallsWithRetryAttempt).isZero()
        // Then the helloWorldService should be invoked 1 time
        Assertions.assertThat(helloWorldService.invocationCounter).isEqualTo(1)
    }

    @Test
    fun `should perform consumeResultBeforeRetryAttempt on retry`() {
        val helloWorldService = HelloWorldService()
        val helloWorldServiceReturnValue = "Hello world"

        val shouldRetry = Predicate { s: String? -> helloWorldServiceReturnValue == s }

        val consumerInvocations = AtomicInteger(0)
        val consumeResultBeforeRetryAttempt = BiConsumer { currentAttempt: Int?, value: String ->
                if (helloWorldServiceReturnValue == value) {
                    consumerInvocations.set(currentAttempt!!)
                }
            }

        val config = io.github.resilience4j.retry.RetryConfig.Builder<String>()
            .retryOnResult(shouldRetry)
            .consumeResultBeforeRetryAttempt(consumeResultBeforeRetryAttempt)
            .build()

        val retry = Retry.of("id", config)
        val supplier = Retry.decorateSupplier(retry) { helloWorldService.returnHelloWorld() }
        val result = supplier.get()

        Assertions.assertThat(helloWorldService.invocationCounter).isEqualTo(3)
        Assertions.assertThat(result).isEqualTo(helloWorldServiceReturnValue)
        Assertions.assertThat(consumerInvocations.get()).isEqualTo(3)
    }
}
