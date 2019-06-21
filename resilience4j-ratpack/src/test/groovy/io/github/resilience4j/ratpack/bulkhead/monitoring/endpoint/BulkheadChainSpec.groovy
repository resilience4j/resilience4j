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
package io.github.resilience4j.ratpack.bulkhead.monitoring.endpoint

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.resilience4j.bulkhead.BulkheadRegistry
import io.github.resilience4j.common.bulkhead.monitoring.endpoint.BulkheadEventsEndpointResponse
import io.github.resilience4j.ratpack.Resilience4jModule
import ratpack.http.client.HttpClient
import ratpack.test.embed.EmbeddedApp
import ratpack.test.exec.ExecHarness
import ratpack.test.http.TestHttpClient
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.time.Duration

import static ratpack.groovy.test.embed.GroovyEmbeddedApp.ratpack

class BulkheadChainSpec extends Specification {
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
                    it.bulkhead('test1') {
                        it.setMaxConcurrentCalls(10).setMaxWaitDuration(Duration.ofMillis(0))
                    }.bulkhead('test2') {
                        it.setMaxConcurrentCalls(20).setMaxWaitDuration(Duration.ofMillis(0))
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

        and: "some bulkhead events"
        def bulkheadRegistry = app.server.registry.get().get(BulkheadRegistry)
        ['test1', 'test2'].each {
            bulkheadRegistry.bulkhead(it).with {
                tryAcquirePermission()
                tryAcquirePermission()
                onComplete()
            }
        }

        when: "we do a sanity check"
        def actual = client.get()

        then: "it works"
        actual.statusCode == 200
        actual.body.text == 'ok'
        bulkheadRegistry.bulkhead('test1').metrics.availableConcurrentCalls == 9 // 10 - 1
        bulkheadRegistry.bulkhead('test2').metrics.availableConcurrentCalls == 19 // 20 - 1

        when: "we get all bulkhead events"
        actual = client.get('bulkhead/events')
        def dto = mapper.readValue(actual.body.text, BulkheadEventsEndpointResponse)

        then: "it works"
        dto.bulkheadEvents.size() == 6

        when: "we get events for a single bulkhead"
        actual = client.get('bulkhead/events/test1')
        dto = mapper.readValue(actual.body.text, BulkheadEventsEndpointResponse)

        then: "it works"
        dto.bulkheadEvents.size() == 3

        when: "we get events for a single bulkhead by call finished type"
        actual = client.get('bulkhead/events/test1/call_finished')
        dto = mapper.readValue(actual.body.text, BulkheadEventsEndpointResponse)

        then: "it works"
        dto.bulkheadEvents.size() == 1

        when: "we get events for a single bulkhead by call permitted type"
        actual = client.get('bulkhead/events/test1/call_permitted')
        dto = mapper.readValue(actual.body.text, BulkheadEventsEndpointResponse)

        then: "it works"
        dto.bulkheadEvents.size() == 2

        when: "we get events for a single bulkhead by call rejected type"
        actual = client.get('bulkhead/events/test1/call_rejected')
        dto = mapper.readValue(actual.body.text, BulkheadEventsEndpointResponse)

        then: "it works"
        dto.bulkheadEvents.size() == 0
    }

    def "test stream events"() {
        given: "an app"
        app = ratpack {
            serverConfig {
                development(false)
            }
            bindings {
                module(Resilience4jModule) {
                    it.bulkhead('test1') {
                        it.setMaxConcurrentCalls(10).setMaxWaitDuration(Duration.ofMillis(9))
                    }.bulkhead('test2') {
                        it.setMaxConcurrentCalls(10).setMaxWaitDuration(Duration.ofMillis(0))
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

        when: "we get all bulkhead events"
        def bulkheadRegistry = app.server.registry.get().get(BulkheadRegistry)
        ['test1', 'test2'].each {
            bulkheadRegistry.bulkhead(it).with {
                tryAcquirePermission()
                tryAcquirePermission()
                onComplete()
            }
        }
        def actual = ExecHarness.yieldSingle {
            streamer.requestStream(new URI("http://$app.server.bindHost:$app.server.bindPort/bulkhead/stream/events")) {
                it.get()
            }
        }

        then: "it works"
        "text/event-stream;charset=UTF-8" == actual.value.headers["Content-Type"]

        when: "we get bulkhead events by name"
        actual = ExecHarness.yieldSingle {
            streamer.requestStream(new URI("http://$app.server.bindHost:$app.server.bindPort/bulkhead/stream/events/test1")) {
                it.get()
            }
        }

        then: "it works"
        "text/event-stream;charset=UTF-8" == actual.value.headers["Content-Type"]

        when: "we get bulkhead events by name and error"
        actual = ExecHarness.yieldSingle {
            streamer.requestStream(new URI("http://$app.server.bindHost:$app.server.bindPort/bulkhead/stream/events/test1/error")) {
                it.get()
            }
        }

        then: "it works"
        "text/event-stream;charset=UTF-8" == actual.value.headers["Content-Type"]

        when: "we get bulkhead events by name and success"
        actual = ExecHarness.yieldSingle {
            streamer.requestStream(new URI("http://$app.server.bindHost:$app.server.bindPort/bulkhead/stream/events/test1/success")) {
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
                    it.bulkhead('test1') {
                        it.setMaxConcurrentCalls(10).setMaxWaitDuration(Duration.ofMillis(0))
                    }.bulkhead('test2') {
                        it.setMaxConcurrentCalls(20).setMaxWaitDuration(Duration.ofMillis(0))
                    }.endpoints {
                        it.bulkheads {
                            it.enabled(false)
                        }
                    }
                }
            }
        }
        client = testHttpClient(app)
        app.server.start() // override lazy start

        and: "some bulkhead events"
        def bulkheadRegistry = app.server.registry.get().get(BulkheadRegistry)
        ['test1', 'test2'].each {
            bulkheadRegistry.bulkhead(it).with {
                tryAcquirePermission()
                tryAcquirePermission()
                onComplete()
            }
        }

        when: "we get all bulkhead events"
        def actual = client.get('bulkhead/events')

        then: "it fails"
        actual.statusCode == 404
    }
}
