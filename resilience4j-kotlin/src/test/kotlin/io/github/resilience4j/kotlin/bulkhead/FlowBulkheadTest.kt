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
package io.github.resilience4j.kotlin.bulkhead

import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.bulkhead.BulkheadConfig
import io.github.resilience4j.bulkhead.BulkheadFullException
import io.github.resilience4j.kotlin.HelloWorldService
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.time.Duration
import java.util.concurrent.Phaser

class FlowBulkheadTest {

    private var permittedEvents = 0
    private var rejectedEvents = 0
    private var finishedEvents = 0

    private fun Bulkhead.registerEventListener(): Bulkhead {
        eventPublisher.apply {
            onCallPermitted { permittedEvents++ }
            onCallRejected { rejectedEvents++ }
            onCallFinished { finishedEvents++ }
        }
        return this
    }

    @Test
    fun `should execute successful function`() {
        runBlocking {
            val bulkhead = Bulkhead.ofDefaults("testName").registerEventListener()
            val resultList = mutableListOf<Int>()

            //When
            flow {
                repeat(3) {
                    emit(it)
                }
            }
                .bulkhead(bulkhead)
                .toList(resultList)


            //Then
            repeat(3) {
                assertThat(resultList[it]).isEqualTo(it)
            }
            assertThat(permittedEvents).isEqualTo(1)
            assertThat(rejectedEvents).isEqualTo(0)
            assertThat(finishedEvents).isEqualTo(1)
        }
    }

    @Test
    fun `should not execute function when full`() {
        runBlocking {
            val bulkhead = Bulkhead.of("testName") {
                BulkheadConfig.custom()
                    .maxConcurrentCalls(1)
                    .maxWaitDuration(Duration.ZERO)
                    .build()
            }.registerEventListener()

            val resultList = mutableListOf<Int>()

            //When


            val sync = Channel<Int>(Channel.RENDEZVOUS)
            val testFlow = flow {
                emit(sync.receive())
                emit(sync.receive())
            }.bulkhead(bulkhead)

            val firstCall = launch {
                testFlow.toList(resultList)
            }

            // wait until our first coroutine is inside the bulkhead
            sync.send(1)

            assertThat(permittedEvents).isEqualTo(1)
            assertThat(rejectedEvents).isEqualTo(0)
            assertThat(finishedEvents).isEqualTo(0)
            assertThat(resultList.size).isEqualTo(1)
            assertThat(resultList[0]).isEqualTo(1)

            val helloWorldService = HelloWorldService()

            //When
            try {
                flow { emit(helloWorldService.returnHelloWorld()) }
                    .bulkhead(bulkhead)
                    .single()

                Assertions.failBecauseExceptionWasNotThrown<Nothing>(BulkheadFullException::class.java)
            } catch (e: BulkheadFullException) {
                // nothing - proceed
            }

            assertThat(permittedEvents).isEqualTo(1)
            assertThat(rejectedEvents).isEqualTo(1)
            assertThat(finishedEvents).isEqualTo(0)

            // allow our first call to complete, and then wait for it
            sync.send(2)
            firstCall.join()

            //Then
            assertThat(permittedEvents).isEqualTo(1)
            assertThat(rejectedEvents).isEqualTo(1)
            assertThat(finishedEvents).isEqualTo(1)
            assertThat(resultList.size).isEqualTo(2)
            assertThat(resultList[1]).isEqualTo(2)
            // Then the helloWorldService should not be invoked
            assertThat(helloWorldService.invocationCounter).isEqualTo(0)
        }
    }

    @Test
    fun `should execute unsuccessful function`() {
        runBlocking {
            val bulkhead = Bulkhead.ofDefaults("testName").registerEventListener()
            val resultList = mutableListOf<Int>()

            //When
            try {
                flow {
                    repeat(3) {
                        emit(it)
                    }
                    error("failed")
                }
                    .bulkhead(bulkhead)
                    .toList(resultList)

                Assertions.failBecauseExceptionWasNotThrown<Nothing>(IllegalStateException::class.java)
            } catch (e: IllegalStateException) {
                // nothing - proceed
            }

            //Then
            repeat(3) {
                assertThat(resultList[it]).isEqualTo(it)
            }
            assertThat(permittedEvents).isEqualTo(1)
            assertThat(rejectedEvents).isEqualTo(0)
            assertThat(finishedEvents).isEqualTo(1)
        }
    }

    @Test
    fun `should not record call finished when cancelled normally`() {
        runBlocking {

            val phaser = Phaser(1)
            var flowCompleted = false
            val bulkhead = Bulkhead.of("testName") {
                BulkheadConfig.custom()
                    .maxConcurrentCalls(1)
                    .maxWaitDuration(Duration.ZERO)
                    .build()
            }.registerEventListener()

            //When
            val job = launch(start = CoroutineStart.ATOMIC) {
                flow {
                    phaser.arrive()
                    delay(5000L)
                    emit(1)
                    flowCompleted = true
                }
                    .bulkhead(bulkhead)
                    .first()
            }

            phaser.awaitAdvance(1)
            job.cancelAndJoin()

            //Then
            assertThat(job.isCompleted).isTrue()
            assertThat(job.isCancelled).isTrue()
            assertThat(flowCompleted).isFalse()
            assertThat(permittedEvents).isEqualTo(1)
            assertThat(rejectedEvents).isEqualTo(0)
            assertThat(finishedEvents).isEqualTo(0)
        }
    }

    @Test
    fun `should not record call finished when cancelled exceptionally`() {
        runBlocking(Dispatchers.Default) {

            val phaser = Phaser(1)
            val parentJob = Job()
            var flowCompleted = false
            val bulkhead = Bulkhead.of("testName") {
                BulkheadConfig.custom()
                    .maxConcurrentCalls(1)
                    .maxWaitDuration(Duration.ZERO)
                    .build()
            }.registerEventListener()

            //When
            val job = launch(parentJob) {
                launch(start = CoroutineStart.ATOMIC) {
                    flow {
                        phaser.arrive()
                        delay(5000L)
                        emit(1)
                        flowCompleted = true
                    }
                        .bulkhead(bulkhead)
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
            assertThat(permittedEvents).isEqualTo(1)
            assertThat(rejectedEvents).isEqualTo(0)
            assertThat(finishedEvents).isEqualTo(0)
        }
    }
}