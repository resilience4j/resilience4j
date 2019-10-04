/*
 * Copyright 2017 Dan Maas
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
package io.github.resilience4j.ratpack.circuitbreaker

import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import ratpack.exec.Blocking
import ratpack.exec.Promise
import ratpack.test.exec.ExecHarness
import spock.lang.Specification

import java.time.Duration
import java.util.function.Predicate

class CircuitBreakerTransformerSpec extends Specification {

    def "can circuit break promise to open state"() {
        given:
        CircuitBreaker breaker = buildBreaker()
        CircuitBreakerTransformer<String> transformer = CircuitBreakerTransformer.of(breaker)
        Exception e = new Exception("puke")

        when:
        def r = ExecHarness.yieldSingle {
            Blocking.<String> get { throw e }
                    .transform(transformer)
        }

        then:
        r.value == null
        r.error
        r.throwable == e
        breaker.state == CircuitBreaker.State.CLOSED

        when:
        r = ExecHarness.yieldSingle {
            Blocking.<String> get { throw e }
                    .transform(transformer)
        }

        then:
        r.value == null
        r.error
        r.throwable == e
        breaker.state == CircuitBreaker.State.OPEN
    }

    def "can circuit break promise to open state with recovery"() {
        given:
        CircuitBreaker breaker = buildBreaker()
        CircuitBreakerTransformer<String> transformer = CircuitBreakerTransformer.of(breaker).recover { t -> "bar" }
        Exception e = new Exception("puke")

        when:
        def r = ExecHarness.yieldSingle {
            Blocking.<String> get { throw e }
                    .transform(transformer)
        }

        then:
        r.value == "bar"
        !r.error
        r.throwable == null
        breaker.state == CircuitBreaker.State.CLOSED

        when:
        r = ExecHarness.yieldSingle {
            Blocking.<String> get { throw e }
                    .transform(transformer)
        }

        then:
        r.value == "bar"
        !r.error
        r.throwable == null
        breaker.state == CircuitBreaker.State.OPEN
    }

    def "can circuit break promise to from closed to open, then half open, then closed"() {
        given:
        CircuitBreaker breaker = buildBreaker()
        CircuitBreakerTransformer<String> circuitTransformer = CircuitBreakerTransformer.of(breaker)
        Exception e = new Exception("puke")

        when: "the first request and retry is a failure"
        def r = ExecHarness.yieldSingle {
            Blocking.<String> get { throw e }
                    .transform(circuitTransformer)

        }

        then: "circuit is still closed since buffer hasn't been filled (1 call)"
        r.value == null
        r.error
        r.throwable == e
        breaker.state == CircuitBreaker.State.CLOSED

        when: "the request and the retry fails again"
        r = ExecHarness.yieldSingle {
            Blocking.<String> get { throw e }
                    .transform(circuitTransformer)
        }

        then: "the circuit open since the buffer has at least 2 calls and the failure threshold was met"
        r.value == null
        r.error
        r.throwable == e
        breaker.state == CircuitBreaker.State.OPEN

        when: "a call is made after the wait duration that is a success"
        sleep 1000
        r = ExecHarness.yieldSingle {
            Blocking.<String> get { "foo" }
                    .transform(circuitTransformer)
        }

        then: "the circuit is now in the HALF_OPEN state"
        r.value == "foo"
        !r.error
        r.throwable == null
        breaker.state == CircuitBreaker.State.HALF_OPEN

        and: "do a call where the Upstream promise handles the error, thus calling complete on the circuit"

        when: "error on half open, retry swallows exception thrown"
        r = ExecHarness.yieldSingle {
            Blocking.<String> get { throw e }.onError { exception -> Promise.value("not foo") }
                    .transform(circuitTransformer)
        }

        then: "the test call was ran and returned our expected error"
        r.value == null
        !r.error
        r.throwable == null
        breaker.state == CircuitBreaker.State.HALF_OPEN
        breaker.metrics

        when: "error on half open, retry swallows exception thrown"
        r = ExecHarness.yieldSingle {
            Blocking.<String> get { throw e }.onError { exception -> Promise.value("not foo") }
                    .transform(circuitTransformer)
        }

        then: "the test call was ran and returned our expected error"
        r.value == null
        !r.error
        r.throwable == null
        breaker.state == CircuitBreaker.State.HALF_OPEN
        breaker.metrics

        and: "the circuit transformer should not count this effectively canceled promises as attempts made while HALF_OPEN"

        when: "trying the circuit that returns a succcess"
        r = ExecHarness.yieldSingle {
            Blocking.<String> get { "foo" }
                    .transform(circuitTransformer)
        }

        then: "the call was allowed and transitioned the circuit to CLOSED"
        r.value == "foo"
        !r.error
        r.throwable == null
        breaker.state == CircuitBreaker.State.CLOSED
    }

    def "test that only specific exceptions are recorded as errors"() {
        given:
        CircuitBreaker breaker = buildBreaker(new MyPredicate())
        CircuitBreakerTransformer<String> transformer = CircuitBreakerTransformer.of(breaker)
        Exception e1 = new DummyException1("puke")
        Exception e2 = new DummyException2("puke")

        when:
        def r = null
        (1..10).forEach {
            r = ExecHarness.yieldSingle {
                Blocking.<String> get { throw e1 }
                        .transform(transformer)
            }
        }

        then:
        r.value == null
        r.error
        r.throwable == e1
        breaker.state == CircuitBreaker.State.CLOSED

        when:
        (1..10).forEach {
            r = ExecHarness.yieldSingle {
                Blocking.<String> get { throw e2 }
                        .transform(transformer)
            }
        }

        then:
        r.value == null
        r.error
        r.throwable instanceof CallNotPermittedException
        breaker.state == CircuitBreaker.State.OPEN
    }

    def buildBreaker(Predicate<Throwable> predicate) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .permittedNumberOfCallsInHalfOpenState(2)
                .slidingWindowSize(2)
                .recordException(predicate)
                .build()
        CircuitBreaker.of("test", config)
    }

    def buildBreaker() {
        return buildBreaker(null)
    }

    static class MyPredicate implements Predicate<Throwable> {

        @Override
        boolean test(Throwable throwable) {
            return !(throwable instanceof DummyException1)
        }
    }

    static class DummyException1 extends Exception {
        DummyException1(String message) {
            super(message)
        }
    }

    static class DummyException2 extends Exception {
        DummyException2(String message) {
            super(message)
        }
    }

}