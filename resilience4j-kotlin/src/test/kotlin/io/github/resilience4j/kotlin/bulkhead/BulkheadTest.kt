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
import io.github.resilience4j.bulkhead.BulkheadConfig
import io.github.resilience4j.bulkhead.BulkheadFullException
import io.github.resilience4j.kotlin.HelloWorldService
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch

class BulkheadTest {

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
        val bulkhead = Bulkhead.ofDefaults("testName").registerEventListener()
        val helloWorldService = HelloWorldService()

        //When
        val result = bulkhead.executeFunction {
            helloWorldService.returnHelloWorld()
        }

        //Then
        assertThat(result).isEqualTo("Hello world")
        assertThat(permittedEvents).isEqualTo(1)
        assertThat(rejectedEvents).isEqualTo(0)
        assertThat(finishedEvents).isEqualTo(1)
        // Then the helloWorldService should be invoked 1 time
        assertThat(helloWorldService.invocationCounter).isEqualTo(1)
    }

    @Test
    fun `should not execute function when full`() {
        val bulkhead = Bulkhead.of("testName") {
            BulkheadConfig.custom()
                .maxConcurrentCalls(1)
                .maxWaitDuration(Duration.ZERO)
                .build()
        }.registerEventListener()
        val helloWorldService = HelloWorldService()
        val latch = CountDownLatch(1)

        //When
        try {
            val c1 = CompletableFuture.supplyAsync {
                bulkhead.executeFunction {
                    latch.countDown()
                    Thread.sleep(500) // force concurrent execution
                    helloWorldService.returnHelloWorld()
                }
            }
            val c2 = CompletableFuture.supplyAsync {
                latch.await()
                bulkhead.executeFunction {
                    helloWorldService.returnHelloWorld()
                }
            }.exceptionally {
                assertThat(it).hasCauseInstanceOf(BulkheadFullException::class.java)
                "ok"
            }
            CompletableFuture.allOf(c1, c2).join()
        } catch (e: BulkheadFullException) {
            // nothing - proceed
        }

        assertThat(permittedEvents).isEqualTo(1)
        assertThat(rejectedEvents).isEqualTo(1)
        assertThat(finishedEvents).isEqualTo(1)
        assertThat(helloWorldService.invocationCounter).isEqualTo(1)
    }

    @Test
    fun `should execute unsuccessful function`() {
        val bulkhead = Bulkhead.ofDefaults("testName").registerEventListener()
        val helloWorldService = HelloWorldService()

        //When
        try {
            bulkhead.executeFunction {
                helloWorldService.throwException()
            }
            Assertions.failBecauseExceptionWasNotThrown<Nothing>(IllegalStateException::class.java)
        } catch (e: IllegalStateException) {
            // nothing - proceed
        }

        //Then
        assertThat(permittedEvents).isEqualTo(1)
        assertThat(rejectedEvents).isEqualTo(0)
        assertThat(finishedEvents).isEqualTo(1)
        // Then the helloWorldService should be invoked 1 time
        assertThat(helloWorldService.invocationCounter).isEqualTo(1)
    }

    @Test
    fun `should decorate successful function`() {
        val bulkhead = Bulkhead.ofDefaults("testName").registerEventListener()
        val helloWorldService = HelloWorldService()

        //When
        val function = bulkhead.decorateFunction {
            helloWorldService.returnHelloWorld()
        }

        //Then
        assertThat(function()).isEqualTo("Hello world")
        assertThat(permittedEvents).isEqualTo(1)
        assertThat(rejectedEvents).isEqualTo(0)
        assertThat(finishedEvents).isEqualTo(1)
        // Then the helloWorldService should be invoked 1 time
        assertThat(helloWorldService.invocationCounter).isEqualTo(1)
    }
}
