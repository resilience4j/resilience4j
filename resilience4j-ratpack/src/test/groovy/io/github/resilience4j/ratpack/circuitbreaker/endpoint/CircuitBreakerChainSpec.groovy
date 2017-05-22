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

package io.github.resilience4j.ratpack.circuitbreaker.endpoint

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.ratpack.Resilience4jModule
import ratpack.test.embed.EmbeddedApp
import ratpack.test.http.TestHttpClient
import spock.lang.AutoCleanup
import spock.lang.Specification

import static ratpack.groovy.test.embed.GroovyEmbeddedApp.ratpack

class CircuitBreakerChainSpec extends Specification {

    @AutoCleanup
    EmbeddedApp app

    @Delegate
    TestHttpClient client

    def "test events"() {
        given:
        def circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults()
        app = ratpack {
            serverConfig {
                development(false)
            }
            bindings {
                bindInstance(CircuitBreakerRegistry, circuitBreakerRegistry)
                module(Resilience4jModule) {
                    it.circuitBreaker('test1') {
                        it.failureRateThreshold(75).waitIntervalInMillis(5000)
                    }.circuitBreaker('test2') {
                        it.failureRateThreshold(25).waitIntervalInMillis(5000)
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
        app.server.start() // disable lazy start

        and:
        ['test1', 'test2'].each {
            def c = circuitBreakerRegistry.circuitBreaker(it)
            c.onSuccess(1000)
            c.onError(1000, new Exception("meh"))
        }

        when:
        def actual = client.get()

        then:
        actual.statusCode == 200
        actual.body.text == 'ok'

        when:
        actual = client.get('circuitbreaker/events')

        then:
        circuitBreakerRegistry.circuitBreaker('test1').metrics.numberOfBufferedCalls == 2
        println actual.body.text

    }

}
