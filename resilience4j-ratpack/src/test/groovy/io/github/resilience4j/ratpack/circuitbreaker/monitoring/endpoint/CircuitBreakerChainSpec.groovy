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

package io.github.resilience4j.ratpack.circuitbreaker.monitoring.endpoint

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerEventsEndpointResponse
import io.github.resilience4j.ratpack.Resilience4jModule
import io.github.resilience4j.ratpack.circuitbreaker.monitoring.endpoint.states.CircuitBreakerStatesEndpointResponse
import ratpack.http.client.HttpClient
import ratpack.test.embed.EmbeddedApp
import ratpack.test.exec.ExecHarness
import ratpack.test.http.TestHttpClient
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.TimeUnit

import static ratpack.groovy.test.embed.GroovyEmbeddedApp.ratpack

class CircuitBreakerChainSpec extends Specification {

    @AutoCleanup
    EmbeddedApp app

    @Delegate
    TestHttpClient client

    HttpClient streamer = HttpClient.of { it.poolSize(8) }

    def mapper = new ObjectMapper()

    def "test states"() {
        given: "a ratpack app"
        app = ratpack {
            serverConfig {
                development(false)
            }
            bindings {
                module(Resilience4jModule) {
                    it.circuitBreaker('test1') {
                        it.setFailureRateThreshold(75).setWaitDurationInOpenState(Duration.ofMillis(5000))
                    }.circuitBreaker('test2') {
                        it.setFailureRateThreshold(25).setWaitDurationInOpenState(Duration.ofMillis(5000))
                    }
                }
            }
            handlers {
                get {
                    render 'ok'
                }
            }
        }
        client = testHttpClient(app)
        app.server.start() // override lazy start

        and: "some circuit breaker events"
        def circuitBreakerRegistry = app.server.registry.get().get(CircuitBreakerRegistry)
        ['test1', 'test2'].each {
            def c = circuitBreakerRegistry.circuitBreaker(it)
            c.onSuccess(1000, TimeUnit.NANOSECONDS)
            c.onError(1000, TimeUnit.NANOSECONDS, new Exception("meh"))
        }

        when: "we do a sanity check"
        def actual = client.get()

        then: "it works"
        actual.statusCode == 200
        actual.body.text == 'ok'
        circuitBreakerRegistry.circuitBreaker('test1').metrics.numberOfBufferedCalls == 2
        circuitBreakerRegistry.circuitBreaker('test2').metrics.numberOfBufferedCalls == 2

        when: "we get all circuit breaker states"
        actual = client.get('circuitbreaker/states')
        def dto = mapper.readValue(actual.body.text, CircuitBreakerStatesEndpointResponse)

        then: "it works"
        dto.circuitBreakerStates.size() == 2
        dto.circuitBreakerStates.each {
            assert it.metrics.numberOfBufferedCalls == 2
        }

        when: "we get state for just the test1 circuit"
        actual = client.get('circuitbreaker/states/test1')
        dto = mapper.readValue(actual.body.text, CircuitBreakerStatesEndpointResponse)

        then: "we retrieved the test1 circuit"
        dto.circuitBreakerStates.size() == 1
        dto.circuitBreakerStates.get(0).metrics.numberOfBufferedCalls == 2
    }

    def "test events"() {
        given: "an app"
        app = ratpack {
            serverConfig {
                development(false)
            }
            bindings {
                module(Resilience4jModule) {
                    it.circuitBreaker('test1') {
                        it.setFailureRateThreshold(75).setWaitDurationInOpenState(Duration.ofMillis(5000))
                    }.circuitBreaker('test2') {
                        it.setFailureRateThreshold(25).setWaitDurationInOpenState(Duration.ofMillis(5000))
                    }
                }
            }
            handlers {
                get {
                    render 'ok'
                }
            }
        }
        client = testHttpClient(app)
        app.server.start() // override lazy start

        and: "some circuit breaker events"
        def circuitBreakerRegistry = app.server.registry.get().get(CircuitBreakerRegistry)
        ['test1', 'test2'].each {
            def c = circuitBreakerRegistry.circuitBreaker(it)
            c.onSuccess(1000, TimeUnit.NANOSECONDS)
            c.onError(1000, TimeUnit.NANOSECONDS, new Exception("meh"))
        }

        when: "we do a sanity check"
        def actual = client.get()

        then: "it works"
        actual.statusCode == 200
        actual.body.text == 'ok'
        circuitBreakerRegistry.circuitBreaker('test1').metrics.numberOfBufferedCalls == 2
        circuitBreakerRegistry.circuitBreaker('test2').metrics.numberOfBufferedCalls == 2

        when: "we get all circuit breaker events"
        actual = client.get('circuitbreaker/events')
        def dto = mapper.readValue(actual.body.text, CircuitBreakerEventsEndpointResponse)

        then: "it works"
        dto.circuitBreakerEvents.size() == 4

        when: "we get events for a single circuit breaker"
        actual = client.get('circuitbreaker/events/test1')
        dto = mapper.readValue(actual.body.text, CircuitBreakerEventsEndpointResponse)

        then: "it works"
        dto.circuitBreakerEvents.size() == 2

        when: "we get events for a single circuit breaker by error type"
        actual = client.get('circuitbreaker/events/test1/error')
        dto = mapper.readValue(actual.body.text, CircuitBreakerEventsEndpointResponse)

        then: "it works"
        dto.circuitBreakerEvents.size() == 1

        when: "we get events for a single circuit breaker by success type"
        actual = client.get('circuitbreaker/events/test1/success')
        dto = mapper.readValue(actual.body.text, CircuitBreakerEventsEndpointResponse)

        then: "it works"
        dto.circuitBreakerEvents.size() == 1
    }

    def "test stream events"() {
        given: "an app"
        app = ratpack {
            serverConfig {
                development(false)
            }
            bindings {
                module(Resilience4jModule) {
                    it.circuitBreaker('test1') {
                        it.setFailureRateThreshold(75).setWaitDurationInOpenState(Duration.ofMillis(5000))
                    }.circuitBreaker('test2') {
                        it.setFailureRateThreshold(25).setWaitDurationInOpenState(Duration.ofMillis(5000))
                    }
                }
            }
            handlers {
                get {
                    render 'ok'
                }
            }
        }
        app.server.start() // override lazy start

        when: "we get all circuit breaker events"
        def circuitBreakerRegistry = app.server.registry.get().get(CircuitBreakerRegistry)
        ['test1', 'test2'].each {
            def c = circuitBreakerRegistry.circuitBreaker(it)
            c.onSuccess(1000, TimeUnit.NANOSECONDS)
            c.onError(1000, TimeUnit.NANOSECONDS, new Exception("meh"))
        }
        def actual = ExecHarness.yieldSingle {
            streamer.requestStream(new URI("http://$app.server.bindHost:$app.server.bindPort/circuitbreaker/stream/events")) {
                it.get()
            }
        }

        then: "it works"
        "text/event-stream;charset=UTF-8" == actual.value.headers["Content-Type"]

        when: "we get circuit breaker events by name"
        actual = ExecHarness.yieldSingle {
            streamer.requestStream(new URI("http://$app.server.bindHost:$app.server.bindPort/circuitbreaker/stream/events/test1")) {
                it.get()
            }
        }

        then: "it works"
        "text/event-stream;charset=UTF-8" == actual.value.headers["Content-Type"]

        when: "we get circuit breaker events by name and error"
        actual = ExecHarness.yieldSingle {
            streamer.requestStream(new URI("http://$app.server.bindHost:$app.server.bindPort/circuitbreaker/stream/events/test1/error")) {
                it.get()
            }
        }

        then: "it works"
        "text/event-stream;charset=UTF-8" == actual.value.headers["Content-Type"]

        when: "we get circuit breaker events by name and success"
        actual = ExecHarness.yieldSingle {
            streamer.requestStream(new URI("http://$app.server.bindHost:$app.server.bindPort/circuitbreaker/stream/events/test1/success")) {
                it.get()
            }
        }

        then: "it works"
        "text/event-stream;charset=UTF-8" == actual.value.headers["Content-Type"]

        when: "we get circuit breaker hystrix events by name and success"
        actual = ExecHarness.yieldSingle {
            streamer.requestStream(new URI("http://$app.server.bindHost:$app.server.bindPort/circuitbreaker/hystrixStream/events/test1/success")) {
                it.get()
            }
        }

        then: "it works"
        "text/event-stream;charset=UTF-8" == actual.value.headers["Content-Type"]

        when: "we get circuit breaker hystrix events by name"
        actual = ExecHarness.yieldSingle {
            streamer.requestStream(new URI("http://$app.server.bindHost:$app.server.bindPort/circuitbreaker/hystrixStream/events/test1")) {
                it.get()
            }
        }

        then: "it works"
        "text/event-stream;charset=UTF-8" == actual.value.headers["Content-Type"]


        when: "we get all circuit breaker hystrix events"
        actual = ExecHarness.yieldSingle {
            streamer.requestStream(new URI("http://$app.server.bindHost:$app.server.bindPort/circuitbreaker/hystrixStream/events/test1")) {
                it.get()
            }
        }

        then: "it works"
        "text/event-stream;charset=UTF-8" == actual.value.headers["Content-Type"]

        when: "we get circuit breaker hystrix events by name and error"
        actual = ExecHarness.yieldSingle {
            streamer.requestStream(new URI("http://$app.server.bindHost:$app.server.bindPort/circuitbreaker/hystrixStream/events/test1/error")) {
                it.get()
            }
        }

        then: "it works"
        "text/event-stream;charset=UTF-8" == actual.value.headers["Content-Type"]

        when: "we get circuit breaker hystrix events by name and success"
        actual = ExecHarness.yieldSingle {
            streamer.requestStream(new URI("http://$app.server.bindHost:$app.server.bindPort/circuitbreaker/hystrixStream/events/test1/success")) {
                it.get()
            }
        }

        then: "it works"
        "text/event-stream;charset=UTF-8" == actual.value.headers["Content-Type"]
    }

    def "test disabled"() {
        given: "an app"
        app = ratpack {
            serverConfig {
                development(false)
            }
            bindings {
                module(Resilience4jModule) {
                    it.circuitBreaker('test1') {
                        it.setFailureRateThreshold(75).setWaitDurationInOpenState(Duration.ofMillis(5000))
                    }.circuitBreaker('test2') {
                        it.setFailureRateThreshold(25).setWaitDurationInOpenState(Duration.ofMillis(5000))
                    }.endpoints {
                        it.circuitBreakers {
                            it.enabled(false)
                        }
                    }
                }
            }
        }
        client = testHttpClient(app)
        app.server.start() // override lazy start

        and: "some circuit breaker events"
        def circuitBreakerRegistry = app.server.registry.get().get(CircuitBreakerRegistry)
        ['test1', 'test2'].each {
            def c = circuitBreakerRegistry.circuitBreaker(it)
            c.onSuccess(1000, TimeUnit.NANOSECONDS)
            c.onError(1000, TimeUnit.NANOSECONDS, new Exception("meh"))
        }

        when: "we get all circuit breaker events"
        def actual = client.get('circuitbreaker/events')

        then: "it fails"
        actual.statusCode == 404
    }

}
