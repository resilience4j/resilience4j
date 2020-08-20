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

package io.github.resilience4j.ratpack.retry.monitoring.endpoint

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.resilience4j.common.retry.monitoring.endpoint.RetryEventsEndpointResponse
import io.github.resilience4j.ratpack.Resilience4jModule
import io.github.resilience4j.retry.RetryRegistry
import io.github.resilience4j.retry.VavrRetry
import io.github.resilience4j.retry.event.RetryEvent
import io.vavr.CheckedFunction0
import io.vavr.control.Try
import ratpack.http.client.HttpClient
import ratpack.test.embed.EmbeddedApp
import ratpack.test.exec.ExecHarness
import ratpack.test.http.TestHttpClient
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.time.Duration

import static ratpack.groovy.test.embed.GroovyEmbeddedApp.ratpack

class RetryChainSpec extends Specification {

    @AutoCleanup
    EmbeddedApp app

    @Delegate
    TestHttpClient client

    HttpClient streamer = HttpClient.of { it.poolSize(8) }

    def mapper = new ObjectMapper()

    def "test events"() {
        given: "an app"
        app = ratpack {
            serverConfig {
                development(false)
            }
            bindings {
                module(Resilience4jModule) {
                    it.retry('test1') {
                        it.setMaxRetryAttempts(3).setWaitDuration(Duration.ofMillis(100))
                    }.retry('test2') {
                        it.setMaxRetryAttempts(3).setWaitDuration(Duration.ofMillis(100))
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

        when: "we do a sanity check"
        def actual = client.get()
        def retryRegistry = app.server.registry.get().get(RetryRegistry)

        then: "it works"
        actual.statusCode == 200
        actual.body.text == 'ok'
        ['test1', 'test2'].collect { retryRegistry.retry(it).metrics }.each {
            assert it.numberOfFailedCallsWithoutRetryAttempt == 0
            assert it.numberOfFailedCallsWithRetryAttempt == 0
            assert it.numberOfSuccessfulCallsWithoutRetryAttempt == 0
            assert it.numberOfSuccessfulCallsWithRetryAttempt == 0
        }

        when: "we get all retry events"
        ['test1', 'test2'].each {
            def r = retryRegistry.retry(it)
            Try.of(VavrRetry.decorateCheckedSupplier(r, {
                throw new Exception('derek olk'); 'unreachable'
            } as CheckedFunction0<String>)).recover { "recovered" }.get()
        }
        actual = client.get('retry/events')
        def dto = mapper.readValue(actual.body.text, RetryEventsEndpointResponse)

        then: "it works"
        dto.retryEvents.size() == 6
        dto.retryEvents.get(5).type == RetryEvent.Type.ERROR
        ['test1', 'test2'].collect { retryRegistry.retry(it).metrics }.each {
            assert it.numberOfFailedCallsWithoutRetryAttempt == 0
            assert it.numberOfFailedCallsWithRetryAttempt == 1
            assert it.numberOfSuccessfulCallsWithoutRetryAttempt == 0
            assert it.numberOfSuccessfulCallsWithRetryAttempt == 0
        }

        when: "we get events for a single retry"
        actual = client.get('retry/events/test1')
        dto = mapper.readValue(actual.body.text, RetryEventsEndpointResponse)

        then: "it works"
        dto.retryEvents.size() == 3
        dto.retryEvents.get(2).type == RetryEvent.Type.ERROR

        when: "we get events for a single retry by type error"
        actual = client.get('retry/events/test1/error')
        dto = mapper.readValue(actual.body.text, RetryEventsEndpointResponse)

        then: "it works"
        dto.retryEvents.size() == 1
        dto.retryEvents.get(0).type == RetryEvent.Type.ERROR

        when: "we get events for a single retry by type retry"
        actual = client.get('retry/events/test1/retry')
        dto = mapper.readValue(actual.body.text, RetryEventsEndpointResponse)

        then: "it works"
        dto.retryEvents.size() == 2
        dto.retryEvents.get(0).type == RetryEvent.Type.RETRY
    }

    def "test stream events"() {
        given: "an app"
        app = ratpack {
            serverConfig {
                development(false)
            }
            bindings {
                module(Resilience4jModule) {
                    it.retry('test1') {
                        it.setMaxRetryAttempts(3).setWaitDuration(Duration.ofMillis(100))
                    }.retry('test2') {
                        it.setMaxRetryAttempts(3).setWaitDuration(Duration.ofMillis(100))
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

        when: "we get all retry events"
        def retryRegistry = app.server.registry.get().get(RetryRegistry)
        ['test1', 'test2'].each {
            def r = retryRegistry.retry(it)
            Try.of(VavrRetry.decorateCheckedSupplier(r, {
                throw new Exception('derek olk'); 'unreachable'
            } as CheckedFunction0<String>)).recover { "recovered" }.get()
        }
        def actual = ExecHarness.yieldSingle {
            streamer.requestStream(new URI("http://$app.server.bindHost:$app.server.bindPort/retry/stream/events")) {
                it.get()
            }
        }

        then: "it works"
        "text/event-stream;charset=UTF-8" == actual.value.headers["Content-Type"]

        when: "we get circuit breaker events by name"
        actual = ExecHarness.yieldSingle {
            streamer.requestStream(new URI("http://$app.server.bindHost:$app.server.bindPort/retry/stream/events/test1")) {
                it.get()
            }
        }

        then: "it works"
        "text/event-stream;charset=UTF-8" == actual.value.headers["Content-Type"]

        when: "we get circuit breaker events by name and error"
        actual = ExecHarness.yieldSingle {
            streamer.requestStream(new URI("http://$app.server.bindHost:$app.server.bindPort/retry/stream/events/test1/error")) {
                it.get()
            }
        }

        then: "it works"
        "text/event-stream;charset=UTF-8" == actual.value.headers["Content-Type"]

        when: "we get circuit breaker events by name and success"
        actual = ExecHarness.yieldSingle {
            streamer.requestStream(new URI("http://$app.server.bindHost:$app.server.bindPort/retry/stream/events/test1/success")) {
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
                    it.retry('test1') {
                        it.setMaxRetryAttempts(3).setWaitDuration(Duration.ofMillis(100))
                    }.retry('test2') {
                        it.setMaxRetryAttempts(3).setWaitDuration(Duration.ofMillis(100))
                    }.endpoints {
                        it.retries {
                            it.enabled(false)
                        }
                    }
                }
            }
        }
        client = testHttpClient(app)
        app.server.start() // override lazy start

        when: "we get all retry events"
        def retryRegistry = app.server.registry.get().get(RetryRegistry)
        ['test1', 'test2'].each {
            def r = retryRegistry.retry(it)
            Try.of(VavrRetry.decorateCheckedSupplier(r, {
                throw new Exception('derek olk'); 'unreachable'
            } as CheckedFunction0<String>)).recover { "recovered" }.get()
        }
        def actual = client.get('retry/events')

        then: "it fails"
        actual.statusCode == 404
    }

}
