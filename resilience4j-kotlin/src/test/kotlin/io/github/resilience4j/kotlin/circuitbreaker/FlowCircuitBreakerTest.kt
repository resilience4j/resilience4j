/*
 *
 *  Copyright 2019 authors
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
@file:Suppress("EXPERIMENTAL_API_USAGE")

package io.github.resilience4j.kotlin.circuitbreaker


import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.lang.IllegalStateException
import java.util.concurrent.Phaser

class FlowCircuitBreakerTest {

    @Test
    fun `should collect successfully`() {
        runBlocking {
            val circuitBreaker = CircuitBreaker.ofDefaults("testName")
            val metrics = circuitBreaker.metrics
            assertThat(metrics.numberOfBufferedCalls).isEqualTo(0)
            val resultList = mutableListOf<Int>()

            //When
            flow {
                repeat(3){
                    emit(it)
                }
            }
                .circuitBreaker(circuitBreaker)
                .toList(resultList)

            //Then
            repeat(3){
                assertThat(resultList[it]).isEqualTo(it)
            }
            assertThat(metrics.numberOfBufferedCalls).isEqualTo(1)
            assertThat(metrics.numberOfFailedCalls).isEqualTo(0)
            assertThat(metrics.numberOfSuccessfulCalls).isEqualTo(1)
        }
    }

    @Test
    fun `should not collect when open`() {
        runBlocking {

            val circuitBreaker = CircuitBreaker.ofDefaults("testName")
            circuitBreaker.transitionToOpenState()
            val metrics = circuitBreaker.metrics
            assertThat(metrics.numberOfBufferedCalls).isEqualTo(0)

            //When
            val resultList = mutableListOf<Int>()
            try {
                flow {
                    Assertions.failBecauseExceptionWasNotThrown<Nothing>(CallNotPermittedException::class.java)
                    repeat(3){
                        emit(it)
                    }
                }
                    .circuitBreaker(circuitBreaker)
                    .toList(resultList)
            } catch (e: CallNotPermittedException) {
                // nothing - proceed
            }

            //Then
            assertThat(resultList).isEmpty()
            assertThat(metrics.numberOfBufferedCalls).isEqualTo(0)
            assertThat(metrics.numberOfFailedCalls).isEqualTo(0)
            assertThat(metrics.numberOfSuccessfulCalls).isEqualTo(0)
            assertThat(metrics.numberOfNotPermittedCalls).isEqualTo(1)
        }
    }

    @Test
    fun `should not start flow when open`() {
        runBlocking {

            var wasStarted = false
            val resultList = mutableListOf<Int>()
            val circuitBreaker = CircuitBreaker.ofDefaults("testName")
            circuitBreaker.transitionToOpenState()
            val metrics = circuitBreaker.metrics
            assertThat(metrics.numberOfBufferedCalls).isEqualTo(0)

            //When
            try {
                flow {
                    wasStarted = true
                    repeat(3){
                        emit(it)
                    }
                }
                    .circuitBreaker(circuitBreaker)
                    .toList(resultList)

                Assertions.failBecauseExceptionWasNotThrown<Nothing>(CallNotPermittedException::class.java)
            } catch (e: Throwable) {
                assertThat(e).isInstanceOf(CallNotPermittedException::class.java)
            }

            //Then
            assertThat(wasStarted).isFalse()
            assertThat(resultList).isEmpty()
            assertThat(metrics.numberOfBufferedCalls).isEqualTo(0)
            assertThat(metrics.numberOfFailedCalls).isEqualTo(0)
            assertThat(metrics.numberOfSuccessfulCalls).isEqualTo(0)
            assertThat(metrics.numberOfNotPermittedCalls).isEqualTo(1)
        }
    }

    @Test
    fun `should record failed flows`() {
        runBlocking {
            val resultList = mutableListOf<Int>()
            val circuitBreaker = CircuitBreaker.ofDefaults("testName")
            val metrics = circuitBreaker.metrics
            assertThat(metrics.numberOfBufferedCalls).isEqualTo(0)

            //When
            try {
                flow {
                    repeat(6){
                        if(it == 4) error("failed")
                        emit(it)
                    }
                }
                    .circuitBreaker(circuitBreaker)
                    .toList(resultList)

                Assertions.failBecauseExceptionWasNotThrown<Nothing>(IllegalStateException::class.java)
            } catch (e: IllegalStateException) {
                // nothing - proceed
            }

            //Then
            assertThat(resultList.size).isEqualTo(4)
            assertThat(metrics.numberOfBufferedCalls).isEqualTo(1)
            assertThat(metrics.numberOfFailedCalls).isEqualTo(1)
            assertThat(metrics.numberOfSuccessfulCalls).isEqualTo(0)
            assertThat(metrics.numberOfNotPermittedCalls).isEqualTo(0)
        }
    }

    @Test
    fun `should not record failed call when cancelled normally`() {
        runBlocking {

            val phaser = Phaser(1)
            var flowCompleted = false
            val circuitBreaker = CircuitBreaker.ofDefaults("testName")
            val metrics = circuitBreaker.metrics
            assertThat(metrics.numberOfBufferedCalls).isEqualTo(0)

            //When
            val job = launch(start = CoroutineStart.ATOMIC) {
                flow {
                    phaser.arrive()
                    delay(5000L)
                    emit(1)
                    flowCompleted = true
                }
                    .circuitBreaker(circuitBreaker)
                    .first()
            }

            phaser.awaitAdvance(1)
            job.cancelAndJoin()

            //Then
            assertThat(job.isCompleted).isTrue()
            assertThat(job.isCancelled).isTrue()
            assertThat(flowCompleted).isFalse()
            assertThat(metrics.numberOfBufferedCalls).isEqualTo(0)
            assertThat(metrics.numberOfFailedCalls).isEqualTo(0)
            assertThat(metrics.numberOfSuccessfulCalls).isEqualTo(0)
            assertThat(metrics.numberOfNotPermittedCalls).isEqualTo(0)
        }
    }

    @Test
    fun `should not record failed call when cancelled exceptionally`() {
        runBlocking(Dispatchers.Default) {

            val phaser = Phaser(1)
            var flowCompleted = false
            val parentJob = Job()
            val circuitBreaker = CircuitBreaker.ofDefaults("testName")
            val metrics = circuitBreaker.metrics
            assertThat(metrics.numberOfBufferedCalls).isEqualTo(0)

            //When
            val job = launch(parentJob) {
                launch(start = CoroutineStart.ATOMIC) {
                    flow {
                        phaser.arrive()
                        delay(5000L)
                        emit(1)
                        flowCompleted = true
                    }
                        .circuitBreaker(circuitBreaker)
                        .first()
                }
                error("exceptional cancellation")
            }

            phaser.awaitAdvance(1)
            parentJob.runCatching { join() }

            //Then
            assertThat(job.isCompleted).isTrue()
            assertThat(job.isCancelled).isTrue()
            assertThat(flowCompleted).isFalse()
            assertThat(metrics.numberOfBufferedCalls).isEqualTo(0)
            assertThat(metrics.numberOfFailedCalls).isEqualTo(0)
            assertThat(metrics.numberOfSuccessfulCalls).isEqualTo(0)
            assertThat(metrics.numberOfNotPermittedCalls).isEqualTo(0)
        }
    }
}