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

package io.github.resilience4j.ratpack.ratelimiter.monitoring.endpoint

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.resilience4j.common.ratelimiter.monitoring.endpoint.RateLimiterEventsEndpointResponse
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent
import io.github.resilience4j.ratpack.Resilience4jModule
import ratpack.http.client.HttpClient
import ratpack.test.embed.EmbeddedApp
import ratpack.test.exec.ExecHarness
import ratpack.test.http.TestHttpClient
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.TimeUnit

import static com.jayway.awaitility.Awaitility.await
import static ratpack.groovy.test.embed.GroovyEmbeddedApp.ratpack

class RateLimiterChainSpec extends Specification {

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
                    it.rateLimiter('test1') {
                        it.setLimitForPeriod(5).setLimitRefreshPeriod(Duration.ofNanos(1000000000)).setTimeoutDuration(Duration.ofSeconds(0))
                    }.rateLimiter('test2') {
                        it.setLimitForPeriod(5).setLimitRefreshPeriod(Duration.ofNanos(1000000000)).setTimeoutDuration(Duration.ofSeconds(0))
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
        def rateLimiterRegistry = app.server.registry.get().get(RateLimiterRegistry)

        then: "it works"
        actual.statusCode == 200
        actual.body.text == 'ok'
        rateLimiterRegistry.rateLimiter('test1').metrics.availablePermissions == 5
        rateLimiterRegistry.rateLimiter('test2').metrics.numberOfWaitingThreads == 0

        when: "we get all rate limiter events"
        ['test1', 'test2'].each {
            def r = rateLimiterRegistry.rateLimiter(it)
            (0..5).each {
                r.acquirePermission()
            }
        }
        actual = client.get('ratelimiter/events')
        def dto = mapper.readValue(actual.body.text, RateLimiterEventsEndpointResponse)

        then: "it works"
        dto.rateLimiterEvents.size() == 12
        dto.rateLimiterEvents.get(11).type == RateLimiterEvent.Type.FAILED_ACQUIRE

        when: "we get events for a single rate limiter"
        actual = client.get('ratelimiter/events/test1')
        dto = mapper.readValue(actual.body.text, RateLimiterEventsEndpointResponse)

        then: "it works"
        dto.rateLimiterEvents.size() == 6
        dto.rateLimiterEvents.get(5).type == RateLimiterEvent.Type.FAILED_ACQUIRE

        when: "we get events for a single rate limiter by type"
        actual = client.get('ratelimiter/events/test1/failed_acquire')
        dto = mapper.readValue(actual.body.text, RateLimiterEventsEndpointResponse)

        then: "it works"
        dto.rateLimiterEvents.size() == 1
        dto.rateLimiterEvents.get(0).type == RateLimiterEvent.Type.FAILED_ACQUIRE

        when: "we get events for a single circuit breaker by success type"
        actual = client.get('ratelimiter/events/test1/successful_acquire')
        dto = mapper.readValue(actual.body.text, RateLimiterEventsEndpointResponse)

        then: "it works"
        dto.rateLimiterEvents.size() == 5
        (0..4).each { i ->
            assert dto.rateLimiterEvents.get(i).type == RateLimiterEvent.Type.SUCCESSFUL_ACQUIRE
        }

        and:
        await().atMost(2, TimeUnit.SECONDS).until {
            ['test1', 'test2'].each {
                def r = rateLimiterRegistry.rateLimiter(it)
                assert r.metrics.availablePermissions == 5
            }
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
                    it.rateLimiter('test1') {
                        it.setLimitForPeriod(5).setLimitRefreshPeriod(Duration.ofNanos(1000000000)).setTimeoutDuration(Duration.ofMillis(0))
                    }.rateLimiter('test2') {
                        it.setLimitForPeriod(5).setLimitRefreshPeriod(Duration.ofNanos(1000000000)).setTimeoutDuration(Duration.ofMillis(0))
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

        when: "we get all rate limiter events"
        def rateLimiterRegistry = app.server.registry.get().get(RateLimiterRegistry)
        ['test1', 'test2'].each {
            def r = rateLimiterRegistry.rateLimiter(it)
            r.acquirePermission()
        }
        def actual = ExecHarness.yieldSingle {
            streamer.requestStream(new URI("http://$app.server.bindHost:$app.server.bindPort/ratelimiter/stream/events")) {
                it.get()
            }
        }

        then: "it works"
        "text/event-stream;charset=UTF-8" == actual.value.headers["Content-Type"]

        when: "we get circuit breaker events by name"
        actual = ExecHarness.yieldSingle {
            streamer.requestStream(new URI("http://$app.server.bindHost:$app.server.bindPort/ratelimiter/stream/events/test1")) {
                it.get()
            }
        }

        then: "it works"
        "text/event-stream;charset=UTF-8" == actual.value.headers["Content-Type"]

        when: "we get circuit breaker events by name and error"
        actual = ExecHarness.yieldSingle {
            streamer.requestStream(new URI("http://$app.server.bindHost:$app.server.bindPort/ratelimiter/stream/events/test1/error")) {
                it.get()
            }
        }

        then: "it works"
        "text/event-stream;charset=UTF-8" == actual.value.headers["Content-Type"]

        when: "we get circuit breaker events by name and success"
        actual = ExecHarness.yieldSingle {
            streamer.requestStream(new URI("http://$app.server.bindHost:$app.server.bindPort/ratelimiter/stream/events/test1/success")) {
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
                    it.rateLimiter('test1') {
                        it.setLimitForPeriod(5).setLimitRefreshPeriod(Duration.ofNanos(1000000000)).setTimeoutDuration(Duration.ofMillis(0))
                    }.rateLimiter('test2') {
                        it.setLimitForPeriod(5).setLimitRefreshPeriod(Duration.ofNanos(1000000000)).setTimeoutDuration(Duration.ofMillis(0))
                    }.endpoints {
                        it.rateLimiters {
                            it.enabled(false)
                        }
                    }
                }
            }
        }
        client = testHttpClient(app)
        app.server.start() // override lazy start

        when: "we get all rate limiter events"
        def rateLimiterRegistry = app.server.registry.get().get(RateLimiterRegistry)
        ['test1', 'test2'].each {
            def r = rateLimiterRegistry.rateLimiter(it)
            (0..5).each {
                r.acquirePermission()
            }
        }
        def actual = client.get('ratelimiter/events')

        then: "it fails"
        actual.statusCode == 404
    }

}
