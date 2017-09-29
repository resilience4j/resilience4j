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

import com.codahale.metrics.SharedMetricRegistries
import com.codahale.metrics.annotation.Timed
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.github.resilience4j.ratpack.circuitbreaker.CircuitBreaker
import io.github.resilience4j.retry.RetryRegistry
import io.prometheus.client.CollectorRegistry
import ratpack.dropwizard.metrics.DropwizardMetricsModule
import ratpack.test.embed.EmbeddedApp
import ratpack.test.http.TestHttpClient
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.time.Duration

import static ratpack.groovy.test.embed.GroovyEmbeddedApp.ratpack

class Resilience4jModuleSpec extends Specification {

    @AutoCleanup
    EmbeddedApp app

    @Delegate
    TestHttpClient client

    def "test circuit breakers"() {
        given:
        def circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults()
        app = ratpack {
            serverConfig {
                development(false)
            }
            bindings {
                bindInstance(CircuitBreakerRegistry, circuitBreakerRegistry)
                module(Resilience4jModule) {
                    it.circuitBreaker('test') {
                        it.defaults(true)
                    }.circuitBreaker('test2') {
                        it.failureRateThreshold(50)
                                .waitIntervalInMillis(5000)
                                .ringBufferSizeInClosedState(200)
                                .ringBufferSizeInHalfOpenState(20)
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

        when:
        def actual = client.get()

        then:
        actual.statusCode == 200
        actual.body.text == 'ok'

        and:
        circuitBreakerRegistry.allCircuitBreakers.size() == 2
        def test1 = circuitBreakerRegistry.circuitBreaker('test1')
        test1.name == 'test1'
        test1.circuitBreakerConfig.with {
            assert ringBufferSizeInClosedState == 100
            assert ringBufferSizeInHalfOpenState == 10
            assert waitDurationInOpenState == Duration.ofMinutes(1)
            assert failureRateThreshold == 50
            it
        }
        def test2 = circuitBreakerRegistry.circuitBreaker('test2')
        test2.name == 'test2'
        test2.circuitBreakerConfig.with {
            assert ringBufferSizeInClosedState == 200
            assert ringBufferSizeInHalfOpenState == 20
            assert waitDurationInOpenState == Duration.ofMillis(5000)
            assert failureRateThreshold == 50
            it
        }
    }

    def "test no circuit breakers"() {
        given:
        def circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults()
        app = ratpack {
            serverConfig {
                development(false)
            }
            bindings {
                bindInstance(CircuitBreakerRegistry, circuitBreakerRegistry)
                module(Resilience4jModule)
            }
            handlers {
                get {
                    render 'ok'
                }
            }
        }
        client = testHttpClient(app)

        when:
        def actual = client.get()

        then:
        actual.statusCode == 200
        actual.body.text == 'ok'

        and:
        circuitBreakerRegistry.allCircuitBreakers.size() == 0
    }

    def "test circuit breakers from yaml"() {
        given:
        def circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults()
        app = ratpack {
            serverConfig {
                development(false)
                yaml(getClass().classLoader.getResource('application.yml'))
                require("/resilience4j", Resilience4jConfig)
            }
            bindings {
                bindInstance(CircuitBreakerRegistry, circuitBreakerRegistry)
                module(Resilience4jModule)
            }
            handlers {
                get {
                    render 'ok'
                }
            }
        }
        client = testHttpClient(app)

        when:
        def actual = client.get()

        then:
        actual.statusCode == 200
        actual.body.text == 'ok'

        and:
        circuitBreakerRegistry.allCircuitBreakers.size() == 2
        def test1 = circuitBreakerRegistry.circuitBreaker('test1')
        test1.name == 'test1'
        test1.circuitBreakerConfig.with {
            assert ringBufferSizeInClosedState == 100
            assert ringBufferSizeInHalfOpenState == 10
            assert waitDurationInOpenState == Duration.ofMinutes(1)
            assert failureRateThreshold == 50
            it
        }
        def test2 = circuitBreakerRegistry.circuitBreaker('test2')
        test2.name == 'test2'
        test2.circuitBreakerConfig.with {
            assert ringBufferSizeInClosedState == 200
            assert ringBufferSizeInHalfOpenState == 20
            assert waitDurationInOpenState == Duration.ofMillis(5000)
            assert failureRateThreshold == 50
            it
        }
    }

    def "test rate limiters"() {
        given:
        def rateLimiterRegistry = RateLimiterRegistry.ofDefaults()
        app = ratpack {
            serverConfig {
                development(false)
            }
            bindings {
                bindInstance(RateLimiterRegistry, rateLimiterRegistry)
                module(Resilience4jModule) {
                    it.rateLimiter('test') {
                        it.defaults(true)
                    }.rateLimiter('test2') {
                        it.limitForPeriod(100)
                                .limitRefreshPeriodInNanos(900)
                                .timeoutInMillis(10)
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

        when:
        def actual = client.get()

        then:
        actual.statusCode == 200
        actual.body.text == 'ok'

        and:
        rateLimiterRegistry.allRateLimiters.size() == 2
        def test1 = rateLimiterRegistry.rateLimiter('test1')
        test1.name == 'test1'
        test1.rateLimiterConfig.with {
            assert limitForPeriod == 50
            assert limitRefreshPeriod == Duration.ofNanos(500)
            assert timeoutDuration == Duration.ofSeconds(5)
            it
        }
        def test2 = rateLimiterRegistry.rateLimiter('test2')
        test2.name == 'test2'
        test2.rateLimiterConfig.with {
            assert limitForPeriod == 100
            assert limitRefreshPeriod == Duration.ofNanos(900)
            assert timeoutDuration == Duration.ofMillis(10)
            it
        }
    }

    def "test no rate limiters"() {
        given:
        def rateLimiterRegistry = RateLimiterRegistry.ofDefaults()
        app = ratpack {
            serverConfig {
                development(false)
            }
            bindings {
                bindInstance(RateLimiterRegistry, rateLimiterRegistry)
                module(Resilience4jModule)
            }
            handlers {
                get {
                    render 'ok'
                }
            }
        }
        client = testHttpClient(app)

        when:
        def actual = client.get()

        then:
        actual.statusCode == 200
        actual.body.text == 'ok'

        and:
        rateLimiterRegistry.allRateLimiters.size() == 0
    }

    def "test rate limiters from yaml"() {
        given:
        def rateLimiterRegistry = RateLimiterRegistry.ofDefaults()
        app = ratpack {
            serverConfig {
                development(false)
                yaml(getClass().classLoader.getResource('application.yml'))
                require("/resilience4j", Resilience4jConfig)
            }
            bindings {
                bindInstance(RateLimiterRegistry, rateLimiterRegistry)
                module(Resilience4jModule)
            }
            handlers {
                get {
                    render 'ok'
                }
            }
        }
        client = testHttpClient(app)

        when:
        def actual = client.get()

        then:
        actual.statusCode == 200
        actual.body.text == 'ok'

        and:
        rateLimiterRegistry.allRateLimiters.size() == 2
        def test1 = rateLimiterRegistry.rateLimiter('test1')
        test1.name == 'test1'
        test1.rateLimiterConfig.with {
            assert limitForPeriod == 50
            assert limitRefreshPeriod == Duration.ofNanos(500)
            assert timeoutDuration == Duration.ofSeconds(5)
            it
        }
        def test2 = rateLimiterRegistry.rateLimiter('test2')
        test2.name == 'test2'
        test2.rateLimiterConfig.with {
            assert limitForPeriod == 100
            assert limitRefreshPeriod == Duration.ofNanos(900)
            assert timeoutDuration == Duration.ofMillis(10)
            it
        }
    }

    def "test retries"() {
        given:
        def retryRegistry = RetryRegistry.ofDefaults()
        app = ratpack {
            serverConfig {
                development(false)
            }
            bindings {
                bindInstance(RetryRegistry, retryRegistry)
                module(Resilience4jModule) {
                    it.retry('test') {
                        it.defaults(true)
                    }.retry('test2') {
                        it.maxAttempts(3)
                                .waitDurationInMillis(1000)
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

        when:
        def actual = client.get()

        then:
        actual.statusCode == 200
        actual.body.text == 'ok'

        and:
        retryRegistry.allRetries.size() == 2
        def test1 = retryRegistry.retry('test1')
        test1.name == 'test1'
        test1.retryConfig.with {
            assert maxAttempts == 3
            it
        }
        def test2 = retryRegistry.retry('test2')
        test2.name == 'test2'
        test2.retryConfig.with {
            assert maxAttempts == 3
            it
        }
    }

    def "test no retries"() {
        given:
        def retryRegistry = RetryRegistry.ofDefaults()
        app = ratpack {
            serverConfig {
                development(false)
            }
            bindings {
                bindInstance(RetryRegistry, retryRegistry)
                module(Resilience4jModule)
            }
            handlers {
                get {
                    render 'ok'
                }
            }
        }
        client = testHttpClient(app)

        when:
        def actual = client.get()

        then:
        actual.statusCode == 200
        actual.body.text == 'ok'

        and:
        retryRegistry.allRetries.size() == 0
    }

    def "test retries from yaml"() {
        given:
        def retryRegistry = RetryRegistry.ofDefaults()
        app = ratpack {
            serverConfig {
                development(false)
                yaml(getClass().classLoader.getResource('application.yml'))
                require("/resilience4j", Resilience4jConfig)
            }
            bindings {
                bindInstance(RetryRegistry, retryRegistry)
                module(Resilience4jModule)
            }
            handlers {
                get {
                    render 'ok'
                }
            }
        }
        client = testHttpClient(app)

        when:
        def actual = client.get()

        then:
        actual.statusCode == 200
        actual.body.text == 'ok'

        and:
        retryRegistry.allRetries.size() == 2
        def test1 = retryRegistry.retry('test1')
        test1.name == 'test1'
        test1.retryConfig.with {
            assert maxAttempts == 3
            it
        }
        def test2 = retryRegistry.retry('test2')
        test2.name == 'test2'
        test2.retryConfig.with {
            assert maxAttempts == 3
            it
        }
    }

    def "test dropwizard metrics"() {
        given:
        def circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults()
        circuitBreakerRegistry.circuitBreaker('test')
        def rateLimiterRegistry = RateLimiterRegistry.ofDefaults()
        rateLimiterRegistry.rateLimiter('test')
        def retryRegistry = RetryRegistry.ofDefaults()
        retryRegistry.retry('test')
        app = ratpack {
            serverConfig {
                development(false)
            }
            bindings {
                bindInstance(CircuitBreakerRegistry, circuitBreakerRegistry)
                bindInstance(RateLimiterRegistry, rateLimiterRegistry)
                bindInstance(RetryRegistry, retryRegistry)
                module(Resilience4jModule) {
                    it.metrics(true)
                }
                module(DropwizardMetricsModule) {
                    it.blockingTimingMetrics(false)
                    it.requestTimingMetrics(false)
                }
                bind(Something)
            }
            handlers {
                get { Something something ->
                    render something.name()
                }
            }
        }
        client = testHttpClient(app)

        when:
        client.get()
        client.get()
        def actual = client.get()

        then:
        actual.statusCode == 200
        actual.body.text == "dan"

        when:
        def registry = SharedMetricRegistries.getOrCreate(DropwizardMetricsModule.RATPACK_METRIC_REGISTRY)
        def timer = registry.timer("test")

        then:
        registry.timers.size() == 1
        timer != null
        timer.count == 3

        and:
        registry.gauges.size() == 13
        registry.gauges.keySet() == ['resilience4j.circuitbreaker.test.current_state',
                                     'resilience4j.circuitbreaker.test.is_open',
                                     'resilience4j.circuitbreaker.test.buffered',
                                     'resilience4j.circuitbreaker.test.buffered_max',
                                     'resilience4j.circuitbreaker.test.failed',
                                     'resilience4j.circuitbreaker.test.not_permitted',
                                     'resilience4j.circuitbreaker.test.successful',
                                     'resilience4j.ratelimiter.test.available_permissions',
                                     'resilience4j.ratelimiter.test.number_of_waiting_threads',
                                     'resilience4j.retry.test.successful_calls_without_retry',
                                     'resilience4j.retry.test.successful_calls_with_retry',
                                     'resilience4j.retry.test.failed_calls_without_retry',
                                     'resilience4j.retry.test.failed_calls_with_retry'].toSet()
    }

    def "test prometheus"() {
        given:
        def circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults()
        circuitBreakerRegistry.circuitBreaker('test')
        def rateLimiterRegistry = RateLimiterRegistry.ofDefaults()
        rateLimiterRegistry.rateLimiter('test')
        def retryRegistry = RetryRegistry.ofDefaults()
        retryRegistry.retry('test')
        CollectorRegistry collectorRegistry = new CollectorRegistry()
        app = ratpack {
            serverConfig {
                development(false)
            }
            bindings {
                bindInstance(CircuitBreakerRegistry, circuitBreakerRegistry)
                bindInstance(RateLimiterRegistry, rateLimiterRegistry)
                bindInstance(RetryRegistry, retryRegistry)
                bindInstance(CollectorRegistry, collectorRegistry)
                module(Resilience4jModule) {
                    it.prometheus(true)
                }
                bind(Something)
            }
            handlers {
                get { Something something ->
                    render something.name()
                }
            }
        }
        client = testHttpClient(app)

        when:
        client.get()
        client.get()
        def actual = client.get()

        then:
        actual.statusCode == 200
        actual.body.text == "dan"

        when:
        def families = collectorRegistry.metricFamilySamples().collect { it.name }.sort()

        then:
        families == ['resilience4j_circuitbreaker_calls',
                     'resilience4j_circuitbreaker_states',
                     'resilience4j_ratelimiter'].sort()
    }

    static class Something {

        @Timed(name = "test", absolute = true)
        @CircuitBreaker(name = "test")
        String name() {
            "dan"
        }

    }

}