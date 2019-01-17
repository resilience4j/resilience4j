package io.github.resilience4j.kotlin.circuitbreaker

import decorateSuspendingFunction
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CircuitBreakerTest {

    @Test
    fun `should decorate suspending function and return with success`() {
        runBlocking {
            // Given
            val circuitBreaker = CircuitBreaker.ofDefaults("testName")
            val metrics = circuitBreaker.metrics
            assertThat(metrics.numberOfBufferedCalls).isEqualTo(0)
            val helloWorldService = HelloWorldService()

            //When
            val function = decorateSuspendingFunction(circuitBreaker) {
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
    }

    @Test
    fun `should decorate suspending function and return an exception`() {
        runBlocking {
            // Given
            val circuitBreaker = CircuitBreaker.ofDefaults("testName")
            val metrics = circuitBreaker.metrics
            assertThat(metrics.numberOfBufferedCalls).isEqualTo(0)
            val helloWorldService = HelloWorldService()

            //When
            val function = decorateSuspendingFunction(circuitBreaker) {
                helloWorldService.throwException()
            }

            //Then
            try {
                function()
            } catch (e: Exception) {
                assertThat(e).isInstanceOf(Exception::class.java)
            }
            assertThat(metrics.numberOfBufferedCalls).isEqualTo(1)
            assertThat(metrics.numberOfFailedCalls).isEqualTo(1)
            assertThat(metrics.numberOfSuccessfulCalls).isEqualTo(0)
            // Then the helloWorldService should be invoked 1 time
            assertThat(helloWorldService.invocationCounter).isEqualTo(1)
        }
    }
}

private class HelloWorldService {
    var invocationCounter = 0

    suspend fun returnHelloWorld(): String {
        invocationCounter++
        return "Hello world"
    }

    suspend fun throwException() {
        invocationCounter++
        throw Exception()
    }
}
