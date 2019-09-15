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

package io.github.resilience4j.ratpack.ratelimiter

import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.github.resilience4j.ratpack.Resilience4jModule
import io.github.resilience4j.ratpack.ratelimiter.RateLimiterHandler
import ratpack.handling.HandlerDecorator
import ratpack.test.embed.EmbeddedApp
import ratpack.test.http.TestHttpClient
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.time.Duration

import static ratpack.groovy.test.embed.GroovyEmbeddedApp.ratpack

class RateLimiterHandlerSpec extends Specification {

    @AutoCleanup
    EmbeddedApp app

    @Delegate
    TestHttpClient client

    def "test rate limit all requests"() {
        given:
        RateLimiterRegistry registry = RateLimiterRegistry.of(buildConfig())
        app = ratpack {
            bindings {
                bindInstance(RateLimiterRegistry, registry)
                module(Resilience4jModule)
                HandlerDecorator
            }
            handlers {
                all(new RateLimiterHandler(registry, 'test'))
                get {
                    render 'success'
                }
                get('a') {
                    render 'success'
                }
            }
        }
        client = testHttpClient(app)
        def actual = null

        when:
        (0..10).each {
            actual = client.get()
            if (it < 10) {
                assert actual.statusCode == 200
            } else {
                assert actual.statusCode == 500
            }
        }

        then:
        actual.body.text.contains('io.github.resilience4j.ratelimiter.RequestNotPermitted: RateLimiter \'test\' does not permit further calls')
        actual.statusCode == 500

        when:
        actual = client.get('a')

        then:
        actual.body.text.contains('io.github.resilience4j.ratelimiter.RequestNotPermitted: RateLimiter \'test\' does not permit further calls')
        actual.statusCode == 500
    }

    def "test rate limit some requests"() {
        given:
        RateLimiterRegistry registry = RateLimiterRegistry.of(buildConfig())
        app = ratpack {
            bindings {
                bindInstance(RateLimiterRegistry, registry)
                module(Resilience4jModule)
            }
            handlers {
                get(new RateLimiterHandler(registry, 'test'))
                get {
                    render 'success'
                }
                get('a') {
                    render 'success'
                }
            }
        }
        client = testHttpClient(app)
        def actual = null

        when:
        (0..10).each {
            actual = client.get()
            if (it < 10) {
                assert actual.statusCode == 200
            } else {
                assert actual.statusCode == 500
            }
        }

        then:
        actual.body.text.contains('io.github.resilience4j.ratelimiter.RequestNotPermitted: RateLimiter \'test\' does not permit further calls')
        actual.statusCode == 500

        when:
        actual = client.get('a')

        then:
        actual.body.text  == 'success'
        actual.statusCode == 200
    }

    // 10 events / 10 s
    def buildConfig() {
        RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(10))
                .limitForPeriod(10)
                .timeoutDuration(Duration.ofMillis(20))
                .build()
    }

}
