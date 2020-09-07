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

package io.github.resilience4j.ratpack.timelimiter.monitoring.endpoint

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.resilience4j.common.timelimiter.monitoring.endpoint.TimeLimiterEventsEndpointResponse
import io.github.resilience4j.ratpack.Resilience4jModule
import io.github.resilience4j.timelimiter.TimeLimiterRegistry
import io.github.resilience4j.timelimiter.event.TimeLimiterEvent
import ratpack.http.client.HttpClient
import ratpack.test.embed.EmbeddedApp
import ratpack.test.exec.ExecHarness
import ratpack.test.http.TestHttpClient
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.CompletableFuture

import static ratpack.groovy.test.embed.GroovyEmbeddedApp.ratpack

class TimeLimiterChainSpec extends Specification {

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
                    it.timeLimiter('test1') {
                        it.setTimeoutDuration(Duration.ofMillis(600))
                    }.timeLimiter('test2') {
                        it.setTimeoutDuration(Duration.ofMillis(400))
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
        def timeLimiterRegistry = app.server.registry.get().get(TimeLimiterRegistry)

        when: "we do a sanity check"
        def actual = client.get()

        then: "it works"
        actual.statusCode == 200

        when: "we get all time limiter events"
        ['test1', 'test2'].each {
            def t = timeLimiterRegistry.timeLimiter(it)
            (0..5).each {
                swallow {
                    t.executeFutureSupplier {
                        CompletableFuture.supplyAsync { sleep 500; "complete" }
                    }
                }
            }
        }
        actual = client.get('timelimiter/events')
        def dto = mapper.readValue(actual.body.text, TimeLimiterEventsEndpointResponse)
        println actual.body.text
        then: "it works"
        dto.timeLimiterEvents.size() == 12
        dto.timeLimiterEvents.get(11).type == TimeLimiterEvent.Type.TIMEOUT

        when: "we get events for a single time limiter"
        actual = client.get('timelimiter/events/test1')
        dto = mapper.readValue(actual.body.text, TimeLimiterEventsEndpointResponse)

        then: "it works"
        dto.timeLimiterEvents.size() == 6
        dto.timeLimiterEvents.get(5).type == TimeLimiterEvent.Type.SUCCESS

        when: "we get events for a single time limiter by type"
        actual = client.get('timelimiter/events/test1/success')
        dto = mapper.readValue(actual.body.text, TimeLimiterEventsEndpointResponse)

        then: "it works"
        dto.timeLimiterEvents.size() == 6
        dto.timeLimiterEvents.get(0).type == TimeLimiterEvent.Type.SUCCESS

        when: "we get events for a single time limiter by timeout type"
        actual = client.get('timelimiter/events/test2/timeout')
        dto = mapper.readValue(actual.body.text, TimeLimiterEventsEndpointResponse)

        then: "it works"
        dto.timeLimiterEvents.size() == 6
        (0..5).each { i ->
            assert dto.timeLimiterEvents.get(i).type == TimeLimiterEvent.Type.TIMEOUT
        }
    }

    def "test stream events"() {
        given: "an app"
        app = ratpack {
            serverConfig {
                development(false)
            }
            bindings {
                module(Resilience4jModule) {
                    it.timeLimiter('test1') {
                        it.setTimeoutDuration(Duration.ofMillis(600))
                    }.timeLimiter('test2') {
                        it.setTimeoutDuration(Duration.ofMillis(400))
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
        def timeLimiterRegistry = app.server.registry.get().get(TimeLimiterRegistry)

        when: "we get all time limiter events"
        ['test1', 'test2'].each {
            def t = timeLimiterRegistry.timeLimiter(it)
            swallow {
                t.executeFutureSupplier {
                    CompletableFuture.supplyAsync { sleep 500; "complete" }
                }
            }
        }
        def actual = ExecHarness.yieldSingle {
            streamer.requestStream(new URI("http://$app.server.bindHost:$app.server.bindPort/timelimiter/stream/events")) {
                it.get()
            }
        }

        then: "it works"
        "text/event-stream;charset=UTF-8" == actual.value.headers["Content-Type"]

        when: "we get time limiter events by name"
        actual = ExecHarness.yieldSingle {
            streamer.requestStream(new URI("http://$app.server.bindHost:$app.server.bindPort/timelimiter/stream/events/test1")) {
                it.get()
            }
        }

        then: "it works"
        "text/event-stream;charset=UTF-8" == actual.value.headers["Content-Type"]

        when: "we get time limiter events by name and error"
        actual = ExecHarness.yieldSingle {
            streamer.requestStream(new URI("http://$app.server.bindHost:$app.server.bindPort/timelimiter/stream/events/test1/error")) {
                it.get()
            }
        }

        then: "it works"
        "text/event-stream;charset=UTF-8" == actual.value.headers["Content-Type"]

        when: "we get time limiter events by name and success"
        actual = ExecHarness.yieldSingle {
            streamer.requestStream(new URI("http://$app.server.bindHost:$app.server.bindPort/timelimiter/stream/events/test1/success")) {
                it.get()
            }
        }

        then: "it works"
        "text/event-stream;charset=UTF-8" == actual.value.headers["Content-Type"]

        when: "we get time limiter events by name and timeout"
        actual = ExecHarness.yieldSingle {
            streamer.requestStream(new URI("http://$app.server.bindHost:$app.server.bindPort/timelimiter/stream/events/test1/timeout")) {
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
                    it.timeLimiter('test1') {
                        it.setTimeoutDuration(Duration.ofMillis(400))
                    }.timeLimiter('test2') {
                        it.setTimeoutDuration(Duration.ofMillis(600))
                    }.endpoints {
                        it.timeLimiters {
                            it.enabled(false)
                        }
                    }
                }
            }
        }
        client = testHttpClient(app)
        app.server.start() // override lazy start
        def timeLimiterRegistry = app.server.registry.get().get(TimeLimiterRegistry)

        when: "we get all time limiter events"
        ['test1', 'test2'].each {
            def t = timeLimiterRegistry.timeLimiter(it)
            (0..5).each {
                swallow {
                    t.executeFutureSupplier {
                        CompletableFuture.supplyAsync { sleep 500; "complete" }
                    }
                }
            }
        }
        def actual = client.get('timelimiter/events')

        then: "it fails"
        actual.statusCode == 404
    }

    private Object swallow(Closure<Object> closure) {
        try {
            return closure()
        } catch (Throwable t) {
            return "swallowed"
        }
    }

}
