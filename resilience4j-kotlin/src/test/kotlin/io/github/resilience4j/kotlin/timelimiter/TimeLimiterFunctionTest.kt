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

import io.github.resilience4j.kotlin.HelloWorldService
import io.github.resilience4j.timelimiter.TimeLimiter
import io.github.resilience4j.timelimiter.event.TimeLimiterOnErrorEvent
import io.github.resilience4j.timelimiter.event.TimeLimiterOnSuccessEvent
import io.github.resilience4j.timelimiter.event.TimeLimiterOnTimeoutEvent
import org.assertj.core.api.Assertions
import org.junit.Test
import java.time.Duration
import java.util.concurrent.TimeoutException

class TimeLimiterFunctionTest {

    @Test
    fun `should execute successful function`() {
        val timelimiter = TimeLimiter.ofDefaults()
        val helloWorldService = HelloWorldService()
        val successfulEvents = mutableListOf<TimeLimiterOnSuccessEvent>()
        timelimiter.eventPublisher.onSuccess(successfulEvents::add)

        // When
        val result = timelimiter.executeFunction {
            helloWorldService.returnHelloWorld()
        }

        // Then
        Assertions.assertThat(result).isEqualTo("Hello world")
        Assertions.assertThat(helloWorldService.invocationCounter).isEqualTo(1)
        Assertions.assertThat(successfulEvents).hasSize(1)
    }

    @Test
    fun `should execute unsuccessful function`() {
        val timelimiter = TimeLimiter.ofDefaults()
        val helloWorldService = HelloWorldService()
        val errorEvents = mutableListOf<TimeLimiterOnErrorEvent>()
        timelimiter.eventPublisher.onError(errorEvents::add)

        // When
        try {
            timelimiter.executeFunction {
                helloWorldService.throwException()
            }
            Assertions.failBecauseExceptionWasNotThrown<Nothing>(IllegalStateException::class.java)
        } catch (e: IllegalStateException) {
            // nothing - proceed
        }

        // Then
        Assertions.assertThat(helloWorldService.invocationCounter).isEqualTo(1)
        Assertions.assertThat(errorEvents).hasSize(1)
    }

    @Test
    fun `should timeout operation that takes too long`() {
        val timelimiter = TimeLimiter.of(TimeLimiterConfig { timeoutDuration(Duration.ofMillis(10)) })
        val timeoutEvents = mutableListOf<TimeLimiterOnTimeoutEvent>()
        timelimiter.eventPublisher.onTimeout(timeoutEvents::add)

        // When
        try {
            timelimiter.executeFunction {
                Thread.sleep(1000)
                "Hello world"
            }
            Assertions.failBecauseExceptionWasNotThrown<Nothing>(TimeoutException::class.java)
        } catch (e: TimeoutException) {
            // nothing - proceed
        }

        // Then
        Assertions.assertThat(timeoutEvents).hasSize(1)
    }

    @Test
    fun `should decorate successful function`() {
        val timelimiter = TimeLimiter.ofDefaults()
        val helloWorldService = HelloWorldService()
        val successfulEvents = mutableListOf<TimeLimiterOnSuccessEvent>()
        timelimiter.eventPublisher.onSuccess(successfulEvents::add)

        // When
        val function = timelimiter.decorateFunction {
            helloWorldService.returnHelloWorld()
        }

        // Then
        Assertions.assertThat(function()).isEqualTo("Hello world")
        Assertions.assertThat(helloWorldService.invocationCounter).isEqualTo(1)
        Assertions.assertThat(successfulEvents).hasSize(1)
    }

    @Test
    fun `should decorate unsuccessful function`() {
        val timelimiter = TimeLimiter.ofDefaults()
        val helloWorldService = HelloWorldService()
        val errorEvents = mutableListOf<TimeLimiterOnErrorEvent>()
        timelimiter.eventPublisher.onError(errorEvents::add)

        // When
        val function = timelimiter.decorateFunction {
            helloWorldService.throwException()
        }

        try {
            function()
            Assertions.failBecauseExceptionWasNotThrown<Nothing>(IllegalStateException::class.java)
        } catch (e: IllegalStateException) {
            // nothing - proceed
        }

        // Then
        Assertions.assertThat(helloWorldService.invocationCounter).isEqualTo(1)
        Assertions.assertThat(errorEvents).hasSize(1)
    }
}
