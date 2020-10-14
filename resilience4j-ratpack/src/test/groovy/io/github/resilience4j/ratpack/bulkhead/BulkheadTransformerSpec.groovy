/*
 * Copyright 2017 Jan Sykora
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.ratpack.bulkhead

import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.bulkhead.BulkheadConfig
import io.github.resilience4j.bulkhead.BulkheadFullException
import ratpack.exec.Blocking
import ratpack.exec.ExecResult
import ratpack.exec.Promise
import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

class BulkheadTransformerSpec extends Specification {

    @Shared
    @AutoCleanup(value = "shutdown")
    ExecutorService executor = Executors.newCachedThreadPool()

    def "execution is permitted and completes"() {
        given:
        def bulkhead = buildBulkhead()
        BulkheadTransformer<String> transformer = BulkheadTransformer.of(bulkhead)
        AtomicInteger times = new AtomicInteger(0)

        when:
        def r = ExecHarness.yieldSingle {
            Blocking.<String> get {
                times.getAndIncrement();
                "s"
            }
                .transform(transformer)
        }

        then:
        with(r) {
            value == "s"
            !error
            throwable == null
        }
        times.get() == 1
    }

    def "transformer can be reused multiple times"() {
        given:
        def bulkhead = buildBulkhead()
        BulkheadTransformer<String> transformer = BulkheadTransformer.of(bulkhead)
        AtomicInteger times = new AtomicInteger(0)

        when:
        def r1 = ExecHarness.yieldSingle {
            Blocking.<String> get {
                times.getAndIncrement();
                "r1"
            }
                .transform(transformer)
        }

        and:
        def r2 = ExecHarness.yieldSingle {
            Blocking.<String> get {
                times.getAndIncrement();
                "r2"
            }
                .transform(transformer)
        }

        then:
        with(r1) {
            value == "r1"
            !error
            throwable == null
        }

        and:
        with(r2) {
            value == "r2"
            !error
            throwable == null
        }

        and:
        times.get() == 2
    }

    def "transformer can be reused multiple times on execution error"() {
        given:
        def bulkhead = buildBulkhead()
        BulkheadTransformer<String> transformer = BulkheadTransformer.of(bulkhead)
        AtomicInteger times = new AtomicInteger(0)

        when:
        def r1 = ExecHarness.yieldSingle {
            Blocking.<String> get {
                times.getAndIncrement();
                throw new RuntimeException("Expected")
            }
                .transform(transformer)
        }

        and:
        def r2 = ExecHarness.yieldSingle {
            Blocking.<String> get {
                times.getAndIncrement();
                "r2"
            }
                .transform(transformer)
        }

        then:
        with(r1) {
            value == null
            error
            throwable.message == "Expected"
        }

        and:
        with(r2) {
            value == "r2"
            !error
            throwable == null
        }

        and:
        times.get() == 2
    }

    def "transformer can be reused multiple times when upstream onError is handled"() {
        given:
        def bulkhead = buildBulkhead()
        BulkheadTransformer<String> transformer = BulkheadTransformer.of(bulkhead)

        and: "setup an event listener to track the number of onCallFinish"
        def bulkheadEvents = bulkhead.getEventPublisher()
        AtomicInteger timesOnCallFinished = new AtomicInteger(0)
        bulkheadEvents.onCallFinished({ timesOnCallFinished.getAndIncrement() })


        when: "The upstream has an error, but is swallowed by `onError`"
        def r1 = ExecHarness.yieldSingle {
            Blocking.<String> get {
                throw new RuntimeException("Expected")
            }
                .onError { e -> Promise.value("not foo") }
                .transform(transformer)
        }

        and: ""
        def r2 = ExecHarness.yieldSingle {
            Blocking.<String> get {
                "r2"
            }
                .transform(transformer)
        }

        then:
        with(r1) {
            value == null
            !error
            throwable == null
        }

        and:
        with(r2) {
            value == "r2"
            !error
            throwable == null
        }

        and:
        timesOnCallFinished.get() == 1

    }

    def "exception is thrown when execution is blocked with one execution"() {
        given:
        def bulkhead = buildBulkhead()
        BulkheadTransformer<String> transformer = BulkheadTransformer.of(bulkhead)
        AtomicInteger times = new AtomicInteger(0)

        and:
        def orderLatch = new CountDownLatch(1)
        def blockingLatch = new CountDownLatch(1)

        when:
        def acceptedFuture = executor.submit({
            ExecHarness.yieldSingle {
                Blocking.<String> get {
                    orderLatch.countDown()
                    assert blockingLatch.await(10, TimeUnit.SECONDS): "Timeout - test failure"
                    times.getAndIncrement()
                    "r"
                }
                    .transform(transformer)
            }
        } as Callable<ExecResult<String>>)

        and:
        assert orderLatch.await(10, TimeUnit.SECONDS)
        def denied = ExecHarness.yieldSingle {
            Blocking.<String> get {
                assert false: "Should never be called"
            }
                .transform(transformer)
        }

        then:
        with(denied) {
            error
            throwable instanceof BulkheadFullException
            throwable.message == "Bulkhead 'test' is full and does not permit further calls"
        }

        and:
        blockingLatch.countDown()
        with(acceptedFuture.get()) {
            !error
            value == "r"
            throwable == null
        }

        and:
        times.get() == 1
    }

    def "recovery function is called when execution is blocked"() {
        given:
        def bulkhead = buildBulkhead()
        BulkheadTransformer<String> transformer = BulkheadTransformer.of(bulkhead).recover {
            "recover"
        }
        AtomicInteger times = new AtomicInteger(0)

        and:
        def orderLatch = new CountDownLatch(1)
        def blockingLatch = new CountDownLatch(1)

        when:
        def acceptedFuture = executor.submit({
            ExecHarness.yieldSingle {
                Blocking.<String> get {
                    orderLatch.countDown()
                    assert blockingLatch.await(10, TimeUnit.SECONDS): "Timeout - test failure"
                    times.getAndIncrement()
                    "r"
                }
                    .transform(transformer)
            }
        } as Callable<ExecResult<String>>)

        and:
        assert orderLatch.await(10, TimeUnit.SECONDS)
        def recovered = ExecHarness.yieldSingle {
            Blocking.<String> get {
                assert false: "Should never be called"
            }
                .transform(transformer)
        }

        then:
        with(recovered) {
            !error
            value == "recover"
        }

        and:
        blockingLatch.countDown()
        with(acceptedFuture.get()) {
            !error
            value == "r"
            throwable == null
        }

        and:
        times.get() == 1
    }

    def buildBulkhead() {
        def config = BulkheadConfig.custom()
            .maxConcurrentCalls(1)
            .maxWaitDuration(Duration.ZERO)
            .build()
        Bulkhead.of("test", config)
    }
}
