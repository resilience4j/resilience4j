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
import io.github.resilience4j.kotlin.HelloWorldService
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CircuitBreakerTest {

    @Test
    fun `should execute successful function`() {
        val circuitBreaker = CircuitBreaker.ofDefaults("testName")
        val metrics = circuitBreaker.metrics
        assertThat(metrics.numberOfBufferedCalls).isEqualTo(0)
        val helloWorldService = HelloWorldService()

        //When
        val result = circuitBreaker.executeFunction {
            helloWorldService.returnHelloWorld()
        }

        //Then
        assertThat(result).isEqualTo("Hello world")
        assertThat(metrics.numberOfBufferedCalls).isEqualTo(1)
        assertThat(metrics.numberOfFailedCalls).isEqualTo(0)
        assertThat(metrics.numberOfSuccessfulCalls).isEqualTo(1)
        // Then the helloWorldService should be invoked 1 time
        assertThat(helloWorldService.invocationCounter).isEqualTo(1)
    }

    @Test
    fun `should not execute function when open`() {
        val circuitBreaker = CircuitBreaker.ofDefaults("testName")
        circuitBreaker.transitionToOpenState()
        val metrics = circuitBreaker.metrics
        assertThat(metrics.numberOfBufferedCalls).isEqualTo(0)
        val helloWorldService = HelloWorldService()

        //When
        try {
            val x = circuitBreaker.executeFunction {
                helloWorldService.returnHelloWorld()
            }
            Assertions.failBecauseExceptionWasNotThrown<Nothing>(CallNotPermittedException::class.java)
        } catch (e: CallNotPermittedException) {
            // nothing - proceed
        }

        //Then
        assertThat(metrics.numberOfBufferedCalls).isEqualTo(0)
        assertThat(metrics.numberOfFailedCalls).isEqualTo(0)
        assertThat(metrics.numberOfSuccessfulCalls).isEqualTo(0)
        assertThat(metrics.numberOfNotPermittedCalls).isEqualTo(1)
        // Then the helloWorldService should not be invoked
        assertThat(helloWorldService.invocationCounter).isEqualTo(0)
    }

    @Test
    fun `should decorate suspend function and return with success`() {
        // Given
        val circuitBreaker = CircuitBreaker.ofDefaults("testName")
        val metrics = circuitBreaker.metrics
        assertThat(metrics.numberOfBufferedCalls).isEqualTo(0)
        val helloWorldService = HelloWorldService()

        //When
        val function = circuitBreaker.decorateFunction {
            helloWorldService.returnHelloWorld()
        }

        //Then
        assertThat(function()).isEqualTo("Hello world")
        assertThat(metrics.numberOfBufferedCalls).isEqualTo(1)
        assertThat(metrics.numberOfFailedCalls).isEqualTo(0)
        assertThat(metrics.numberOfSuccessfulCalls).isEqualTo(1)
        // Then the helloWorldService should be invoked 1 time
        assertThat(helloWorldService.invocationCounter).isEqualTo(1)
    }

    @Test
    fun `should decorate suspend function and return an exception`() {
        // Given
        val circuitBreaker = CircuitBreaker.ofDefaults("testName")
        val metrics = circuitBreaker.metrics
        assertThat(metrics.numberOfBufferedCalls).isEqualTo(0)
        val helloWorldService = HelloWorldService()

        //When
        val function = circuitBreaker.decorateFunction {
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
        assertThat(metrics.numberOfSuccessfulCalls).isEqualTo(0)
        // Then the helloWorldService should be invoked 1 time
        assertThat(helloWorldService.invocationCounter).isEqualTo(1)
    }
}
