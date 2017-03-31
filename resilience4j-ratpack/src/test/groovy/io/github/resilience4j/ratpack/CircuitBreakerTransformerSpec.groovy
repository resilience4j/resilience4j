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
package io.github.resilience4j.ratpack

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import ratpack.exec.Blocking
import ratpack.test.exec.ExecHarness
import spock.lang.Specification

import java.time.Duration

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

        when:
        sleep 1000
        r = ExecHarness.yieldSingle {
            Blocking.<String> get { "foo" }
                    .transform(transformer)
        }

        then:
        r.value == "foo"
        !r.error
        r.throwable == null
        breaker.state == CircuitBreaker.State.HALF_OPEN

        when:
        r = ExecHarness.yieldSingle {
            Blocking.<String> get { "foo" }
                    .transform(transformer)
        }

        then:
        r.value == "foo"
        !r.error
        r.throwable == null
        breaker.state == CircuitBreaker.State.CLOSED
    }

    def buildBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .ringBufferSizeInHalfOpenState(2)
                .ringBufferSizeInClosedState(2)
                .build()
        CircuitBreaker.of("test", config)
    }

}
