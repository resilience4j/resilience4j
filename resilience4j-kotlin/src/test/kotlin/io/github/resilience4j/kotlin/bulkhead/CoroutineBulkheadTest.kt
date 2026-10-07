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
package io.github.resilience4j.kotlin.bulkhead

import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.bulkhead.BulkheadFullException
import io.github.resilience4j.kotlin.CoroutineHelloWorldService
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class CoroutineBulkheadTest {

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
            val helloWorldService = CoroutineHelloWorldService()

            //When
            val result = bulkhead.executeSuspendFunction {
                helloWorldService.returnHelloWorld()
            }

            //Then
            assertThat(result).isEqualTo("Hello world")
            assertThat(permittedEvents).isEqualTo(1)
            assertThat(rejectedEvents).isZero()
            assertThat(finishedEvents).isEqualTo(1)
            // Then the helloWorldService should be invoked 1 time
            assertThat(helloWorldService.invocationCounter).isEqualTo(1)
        }
    }

    @Test
    fun `should not execute function when full`() {
        runBlocking {
            val bulkhead = Bulkhead.of("testName") {
                BulkheadConfig {
                    maxConcurrentCalls(1)
                    maxWaitDuration(Duration.ZERO)
                }
            }.registerEventListener()

            val sync = Channel<Unit>(Channel.RENDEZVOUS)
            val firstCall = launch {
                bulkhead.executeSuspendFunction {
                    sync.receive()
                    sync.receive()
                }
            }

            // wait until our first coroutine is inside the bulkhead
            sync.send(Unit)

            assertThat(permittedEvents).isEqualTo(1)
            assertThat(rejectedEvents).isZero()
            assertThat(finishedEvents).isZero()

            val helloWorldService = CoroutineHelloWorldService()

            //When
            try {
                bulkhead.executeSuspendFunction {
                    helloWorldService.returnHelloWorld()
                }
                Assertions.failBecauseExceptionWasNotThrown<Nothing>(BulkheadFullException::class.java)
            } catch (e: BulkheadFullException) {
                // nothing - proceed
            }

            assertThat(permittedEvents).isEqualTo(1)
            assertThat(rejectedEvents).isEqualTo(1)
            assertThat(finishedEvents).isZero()

            // allow our first call to complete, and then wait for it
            sync.send(Unit)
            firstCall.join()

            //Then
            assertThat(permittedEvents).isEqualTo(1)
            assertThat(rejectedEvents).isEqualTo(1)
            assertThat(finishedEvents).isEqualTo(1)
            // Then the helloWorldService should not be invoked
            assertThat(helloWorldService.invocationCounter).isZero()
        }
    }

    @Test
    fun `should execute unsuccessful function`() {
        runBlocking {
            val bulkhead = Bulkhead.ofDefaults("testName").registerEventListener()
            val helloWorldService = CoroutineHelloWorldService()

            //When
            try {
                bulkhead.executeSuspendFunction {
                    helloWorldService.throwException()
                }
                Assertions.failBecauseExceptionWasNotThrown<Nothing>(IllegalStateException::class.java)
            } catch (e: IllegalStateException) {
                // nothing - proceed
            }

            //Then
            assertThat(permittedEvents).isEqualTo(1)
            assertThat(rejectedEvents).isZero()
            assertThat(finishedEvents).isEqualTo(1)
            // Then the helloWorldService should be invoked 1 time
            assertThat(helloWorldService.invocationCounter).isEqualTo(1)
        }
    }

    @Test
    fun `should decorate successful function`() {
        runBlocking {
            val bulkhead = Bulkhead.ofDefaults("testName").registerEventListener()
            val helloWorldService = CoroutineHelloWorldService()

            //When
            val function = bulkhead.decorateSuspendFunction {
                helloWorldService.returnHelloWorld()
            }

            //Then
            assertThat(function()).isEqualTo("Hello world")
            assertThat(permittedEvents).isEqualTo(1)
            assertThat(rejectedEvents).isZero()
            assertThat(finishedEvents).isEqualTo(1)
            // Then the helloWorldService should be invoked 1 time
            assertThat(helloWorldService.invocationCounter).isEqualTo(1)
        }
    }

    @Test
    @Timeout(5)
    fun `should suspend until a permission is released instead of failing fast`() {
        runBlocking {
            val bulkhead = Bulkhead.of("testName") {
                BulkheadConfig {
                    maxConcurrentCalls(1)
                    maxWaitDuration(Duration.ofSeconds(10))
                }
            }.registerEventListener()
            assertThat(bulkhead.tryAcquirePermission()).isTrue()

            // UNDISPATCHED runs the coroutine on this thread until it suspends waiting for the permission
            val waiting = async(start = CoroutineStart.UNDISPATCHED) { bulkhead.executeSuspendFunction { "done" } }
            assertThat(waiting.isCompleted).isFalse()

            bulkhead.onComplete()

            assertThat(waiting.await()).isEqualTo("done")
            assertThat(bulkhead.metrics.availableConcurrentCalls).isEqualTo(1)
            assertThat(permittedEvents).isEqualTo(2)
            assertThat(rejectedEvents).isZero()
            assertThat(finishedEvents).isEqualTo(2)
        }
    }

    @Test
    @Timeout(5)
    fun `should withdraw the queued permission request when the waiting coroutine is cancelled`() {
        runBlocking {
            val bulkhead = Bulkhead.of("testName") {
                BulkheadConfig {
                    maxConcurrentCalls(1)
                    maxWaitDuration(Duration.ofSeconds(10))
                }
            }.registerEventListener()
            assertThat(bulkhead.tryAcquirePermission()).isTrue()
            val waiting = launch(start = CoroutineStart.UNDISPATCHED) { bulkhead.executeSuspendFunction { "never" } }
            assertThat(waiting.isActive).isTrue()

            waiting.cancelAndJoin()
            bulkhead.onComplete()

            assertThat(bulkhead.metrics.availableConcurrentCalls)
                .`as`("a cancelled waiting request must not consume the released permit")
                .isEqualTo(1)
            assertThat(rejectedEvents).isZero()
            assertThat(bulkhead.executeSuspendFunction { "after" }).isEqualTo("after")
        }
    }

    @Test
    @Timeout(5)
    fun `should release a permission granted concurrently with the cancellation of the wait`() {
        runBlocking {
            val bulkhead = mock(Bulkhead::class.java)
            // models a grant which wins the race against the cancellation of the request
            val permission = object : CompletableFuture<Void>() {
                override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false
            }
            given(bulkhead.acquirePermissionAsync()).willReturn(permission)
            val waiting = launch(start = CoroutineStart.UNDISPATCHED) { bulkhead.executeSuspendFunction { "never" } }

            waiting.cancelAndJoin()
            verify(bulkhead, never()).releasePermission()
            permission.complete(null)

            verify(bulkhead).releasePermission()
            verify(bulkhead, never()).onComplete()
        }
    }

    @Test
    fun `should fail with BulkheadFullException once the max wait duration elapsed`() {
        runBlocking {
            val bulkhead = Bulkhead.of("testName") {
                BulkheadConfig {
                    maxConcurrentCalls(1)
                    maxWaitDuration(Duration.ofMillis(100))
                }
            }.registerEventListener()
            assertThat(bulkhead.tryAcquirePermission()).isTrue()

            val rejected = CountDownLatch(1)
            bulkhead.eventPublisher.onCallRejected { rejected.countDown() }

            try {
                bulkhead.executeSuspendFunction { "never" }
                Assertions.failBecauseExceptionWasNotThrown<Nothing>(BulkheadFullException::class.java)
            } catch (e: BulkheadFullException) {
                // expected
            }

            // the rejection event is published by the bulkhead's own completion callback, which may
            // run after the coroutine has already been resumed with the exception
            assertThat(rejected.await(2, TimeUnit.SECONDS)).isTrue()
            bulkhead.onComplete()
            assertThat(bulkhead.metrics.availableConcurrentCalls).isEqualTo(1)
        }
    }
}
