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
import io.github.resilience4j.bulkhead.BulkheadRegistry
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
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
        app = ratpack {
            serverConfig {
                development(false)
            }
            bindings {
                module(Resilience4jModule) {
                    it.circuitBreaker('test1')
                        .circuitBreaker('test2') {
                            it.setFailureRateThreshold(50)
                                .setWaitDurationInOpenState(Duration.ofMillis(5000))
                                .setSlidingWindowSize(200)
                                .setPermittedNumberOfCallsInHalfOpenState(20)
                                .setFailureRateThreshold(60)
                                .setAutomaticTransitionFromOpenToHalfOpenEnabled(true)
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
        def circuitBreakerRegistry = app.server.registry.get().get(CircuitBreakerRegistry)
        circuitBreakerRegistry.allCircuitBreakers.size() == 2
        def test1 = circuitBreakerRegistry.circuitBreaker('test1')
        test1.name == 'test1'
        test1.circuitBreakerConfig.with {
            assert slidingWindowSize == 100
            assert permittedNumberOfCallsInHalfOpenState == 10
            assert waitIntervalFunctionInOpenState.apply(1) == 60_000
            assert failureRateThreshold == 50
            assert !automaticTransitionFromOpenToHalfOpenEnabled
            it
        }
        def test2 = circuitBreakerRegistry.circuitBreaker('test2')
        test2.name == 'test2'
        test2.circuitBreakerConfig.with {
            assert slidingWindowSize == 200
            assert permittedNumberOfCallsInHalfOpenState == 20
            assert waitIntervalFunctionInOpenState.apply(1) == 5_000
            assert failureRateThreshold == 60
            assert automaticTransitionFromOpenToHalfOpenEnabled
            assert recordExceptionPredicate.test(new Exception())
            it
        }
    }

    def "test no circuit breakers"() {
        given:
        app = ratpack {
            serverConfig {
                development(false)
            }
            bindings {
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
        def circuitBreakerRegistry = app.server.registry.get().get(CircuitBreakerRegistry)

        then:
        actual.statusCode == 200
        actual.body.text == 'ok'

        and:
        circuitBreakerRegistry.allCircuitBreakers.size() == 0
    }

    def "test circuit breakers from yaml"() {
        given:
        app = ratpack {
            serverConfig {
                development(false)
                yaml(getClass().classLoader.getResource('application.yml'))
                require("/resilience4j", Resilience4jConfig)
            }
            bindings {
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
        def circuitBreakerRegistry = app.server.registry.get().get(CircuitBreakerRegistry)
        circuitBreakerRegistry.allCircuitBreakers.size() == 2
        def test1 = circuitBreakerRegistry.circuitBreaker('test1')
        test1.name == 'test1'
        test1.circuitBreakerConfig.with {
            assert slidingWindowSize == 100
            assert permittedNumberOfCallsInHalfOpenState == 20
            assert waitIntervalFunctionInOpenState.apply(1) == 1_000
            assert failureRateThreshold == 60
            assert automaticTransitionFromOpenToHalfOpenEnabled
            assert recordExceptionPredicate.test(new DummyException1("test"))
            assert recordExceptionPredicate.test(new DummyException2("test"))
            it
        }
        def test2 = circuitBreakerRegistry.circuitBreaker('test2')
        test2.name == 'test2'
        test2.circuitBreakerConfig.with {
            assert slidingWindowSize == 200
            assert permittedNumberOfCallsInHalfOpenState == 20
            assert waitIntervalFunctionInOpenState.apply(1) == 5_000
            assert failureRateThreshold == 60
            assert automaticTransitionFromOpenToHalfOpenEnabled
            assert recordExceptionPredicate.test(new DummyException1("test"))
            assert !recordExceptionPredicate.test(new DummyException2("test"))
            it
        }
        // test default
        def test3 = circuitBreakerRegistry.circuitBreaker('test3', 'default')
        circuitBreakerRegistry.allCircuitBreakers.size() == 3
        test3.name == 'test3'
        test3.circuitBreakerConfig.with {
            assert slidingWindowSize == 200
            assert permittedNumberOfCallsInHalfOpenState == 20
            assert waitIntervalFunctionInOpenState.apply(1) == 1_000
            assert failureRateThreshold == 60
            assert automaticTransitionFromOpenToHalfOpenEnabled
            assert recordExceptionPredicate.test(new DummyException1("test"))
            assert recordExceptionPredicate.test(new DummyException2("test"))
            it
        }
    }

    def "test rate limiters"() {
        given:
        app = ratpack {
            serverConfig {
                development(false)
            }
            bindings {
                module(Resilience4jModule) {
                    it.rateLimiter('test')
                        .rateLimiter('test2') {
                            it.setLimitForPeriod(100)
                                .setLimitRefreshPeriod(Duration.ofNanos(900))
                                .setTimeoutDuration(Duration.ofMillis(10))
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
        def rateLimiterRegistry = app.server.registry.get().get(RateLimiterRegistry)
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
        app = ratpack {
            serverConfig {
                development(false)
            }
            bindings {
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
        def rateLimiterRegistry = app.server.registry.get().get(RateLimiterRegistry)

        then:
        actual.statusCode == 200
        actual.body.text == 'ok'

        and:
        rateLimiterRegistry.allRateLimiters.size() == 0
    }

    def "test rate limiters from yaml"() {
        given:
        app = ratpack {
            serverConfig {
                development(false)
                yaml(getClass().classLoader.getResource('application.yml'))
                require("/resilience4j", Resilience4jConfig)
            }
            bindings {
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
        def rateLimiterRegistry = app.server.registry.get().get(RateLimiterRegistry)
        rateLimiterRegistry.allRateLimiters.size() == 2
        def test1 = rateLimiterRegistry.rateLimiter('test1')
        test1.name == 'test1'
        test1.rateLimiterConfig.with {
            assert limitForPeriod == 150
            assert limitRefreshPeriod == Duration.ofNanos(900)
            assert timeoutDuration == Duration.ofMillis(10)
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
        // test default
        def test3 = rateLimiterRegistry.rateLimiter('test3')
        test3.name == 'test3'
        test3.rateLimiterConfig.with {
            assert limitForPeriod == 100
            assert limitRefreshPeriod == Duration.ofNanos(900)
            assert timeoutDuration == Duration.ofMillis(10)
            it
        }
    }

    def "test retries"() {
        given:
        app = ratpack {
            serverConfig {
                development(false)
            }
            bindings {
                module(Resilience4jModule) {
                    it.retry('test')
                        .retry('test2') {
                            it.setMaxAttempts(3)
                                .setWaitDuration(Duration.ofMillis(1000))
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
        def retryRegistry = app.server.registry.get().get(RetryRegistry)
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
        app = ratpack {
            serverConfig {
                development(false)
            }
            bindings {
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
        def retryRegistry = app.server.registry.get().get(RetryRegistry)

        then:
        actual.statusCode == 200
        actual.body.text == 'ok'

        and:
        retryRegistry.allRetries.size() == 0
    }

    def "test retries from yaml"() {
        given:
        app = ratpack {
            serverConfig {
                development(false)
                yaml(getClass().classLoader.getResource('application.yml'))
                require("/resilience4j", Resilience4jConfig)
            }
            bindings {
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
        def retryRegistry = app.server.registry.get().get(RetryRegistry)
        retryRegistry.allRetries.size() == 2
        def test1 = retryRegistry.retry('test1')
        test1.name == 'test1'
        test1.retryConfig.with {
            assert maxAttempts == 4
            it
        }
        def test2 = retryRegistry.retry('test2')
        test2.name == 'test2'
        test2.retryConfig.with {
            assert maxAttempts == 3
            it
        }
        // test default
        def test3 = retryRegistry.retry('test3')
        test3.name == 'test3'
        test3.retryConfig.with {
            assert maxAttempts == 3
            it
        }
    }

    def "test bulkheads"() {
        given:
        app = ratpack {
            serverConfig {
                development(false)
            }
            bindings {
                module(Resilience4jModule) {
                    it.bulkhead('test')
                        .bulkhead('test2') {
                            it.setMaxConcurrentCalls(100)
                                .setMaxWaitDuration(Duration.ofMillis(1000))
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
        def bulkheadRegistry = app.server.registry.get().get(BulkheadRegistry)
        bulkheadRegistry.allBulkheads.size() == 2
        def test1 = bulkheadRegistry.bulkhead('test1')
        test1.name == 'test1'
        test1.bulkheadConfig.with {
            assert maxConcurrentCalls == 25
            assert maxWaitDuration.toMillis() == 0
            it
        }
        def test2 = bulkheadRegistry.bulkhead('test2')
        test2.name == 'test2'
        test2.bulkheadConfig.with {
            assert maxConcurrentCalls == 100
            assert maxWaitDuration.toMillis() == 1000
            it
        }
    }

    def "test no bulkheads"() {
        given:
        app = ratpack {
            serverConfig {
                development(false)
            }
            bindings {
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
        def bulkheadRegistry = app.server.registry.get().get(BulkheadRegistry)

        then:
        actual.statusCode == 200
        actual.body.text == 'ok'

        and:
        bulkheadRegistry.allBulkheads.size() == 0
    }

    def "test bulkheads from yaml"() {
        given:
        app = ratpack {
            serverConfig {
                development(false)
                yaml(getClass().classLoader.getResource('application.yml'))
                require("/resilience4j", Resilience4jConfig)
            }
            bindings {
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
        def bulkheadRegistry = app.server.registry.get().get(BulkheadRegistry)
        bulkheadRegistry.allBulkheads.size() == 2
        def test1 = bulkheadRegistry.bulkhead('test1')
        test1.name == 'test1'
        test1.bulkheadConfig.with {
            assert maxConcurrentCalls == 50
            assert maxWaitDuration.toMillis() == 750
            it
        }
        def test2 = bulkheadRegistry.bulkhead('test2')
        test2.name == 'test2'
        test2.bulkheadConfig.with {
            assert maxConcurrentCalls == 100
            assert maxWaitDuration.toMillis() == 1000
            it
        }
        // test default
        def test3 = bulkheadRegistry.bulkhead('test3')
        test3.name == 'test3'
        test3.bulkheadConfig.with {
            assert maxConcurrentCalls == 50
            assert maxWaitDuration.toMillis() == 500
            it
        }
    }

    def "test threadpool bulkheads from yaml"() {
        given:
        app = ratpack {
            serverConfig {
                development(false)
                yaml(getClass().classLoader.getResource('application.yml'))
                require("/resilience4j", Resilience4jConfig)
            }
            bindings {
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
        def bulkheadRegistry = app.server.registry.get().get(ThreadPoolBulkheadRegistry)
        bulkheadRegistry.allBulkheads.size() == 2
        def test1 = bulkheadRegistry.bulkhead('test1')
        test1.name == 'test1'
        test1.bulkheadConfig.with {
            assert maxThreadPoolSize == 4
            assert coreThreadPoolSize == 2
            assert queueCapacity == 2
            assert keepAliveDuration.toMillis() == 1000
            it
        }
        def test2 = bulkheadRegistry.bulkhead('test2')
        test2.name == 'test2'
        test2.bulkheadConfig.with {
            assert maxThreadPoolSize == 1
            assert coreThreadPoolSize == 1
            assert queueCapacity == 1
            assert keepAliveDuration.toMillis() == 1000
            it
        }
        // test default
        def test3 = bulkheadRegistry.bulkhead('test3')
        test3.name == 'test3'
        test3.bulkheadConfig.with {
            assert maxThreadPoolSize == 4
            assert coreThreadPoolSize == 2
            assert queueCapacity == 2
            assert keepAliveDuration.toMillis() == 1000
            it
        }
    }

    def "test shared configs are added to each type registry"() {
        given:
        app = ratpack {
            serverConfig {
                development(false)
                yaml(getClass().classLoader.getResource('application.yml'))
                require("/resilience4j", Resilience4jConfig)
            }
            bindings {
                module(Resilience4jModule)
            }
            handlers {
                get {
                    render "OK"
                }
            }
        }
        client = testHttpClient(app)

        when:
        def actual = client.get()

        then:
        actual.statusCode == 200
        actual.body.text == "OK"

        when:
        def circuitBreakerRegistry = app.server.registry.get().get(CircuitBreakerRegistry)
        def rateLimiterRegistry = app.server.registry.get().get(RateLimiterRegistry)
        def retryRegistry = app.server.registry.get().get(RetryRegistry)
        def bulkheadRegistry = app.server.registry.get().get(BulkheadRegistry)
        def threadPoolBulkheadRegistry = app.server.registry.get().get(ThreadPoolBulkheadRegistry)
        def circuitBreakerConfig = circuitBreakerRegistry.getConfiguration('shared')
        def rateLimiterConfig = rateLimiterRegistry.getConfiguration('shared')
        def retryConfig = retryRegistry.getConfiguration('shared')
        def bulkheadConfig = bulkheadRegistry.getConfiguration('shared')
        def threadPoolBulkheadConfig = threadPoolBulkheadRegistry.getConfiguration('shared')
        def circuitBreakerConfig2 = circuitBreakerRegistry.getConfiguration('shared2')
        def rateLimiterConfig2 = rateLimiterRegistry.getConfiguration('shared2')
        def retryConfig2 = retryRegistry.getConfiguration('shared2')
        def bulkheadConfig2 = bulkheadRegistry.getConfiguration('shared2')
        def threadPoolBulkheadConfig2 = threadPoolBulkheadRegistry.getConfiguration('shared2')

        then:
        circuitBreakerConfig.present
        rateLimiterConfig.present
        retryConfig.present
        bulkheadConfig.present
        threadPoolBulkheadConfig.present
        !circuitBreakerConfig2.present
        !rateLimiterConfig2.present
        !retryConfig2.present
        !bulkheadConfig2.present
        !threadPoolBulkheadConfig2.present
    }

    def "test dropwizard metrics"() {
        given:
        app = ratpack {
            serverConfig {
                development(false)
            }
            bindings {
                module(Resilience4jModule) {
                    it.metrics(true)
                    it.circuitBreaker('test')
                    it.rateLimiter('test')
                    it.retry('test')
                    it.bulkhead('test')
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
        registry.gauges.size() == 18
        registry.gauges.keySet() == ['resilience4j.circuitbreaker.test.state',
                                     'resilience4j.circuitbreaker.test.buffered',
                                     'resilience4j.circuitbreaker.test.failed',
                                     'resilience4j.circuitbreaker.test.slow',
                                     'resilience4j.circuitbreaker.test.slow_successful',
                                     'resilience4j.circuitbreaker.test.slow_failed',
                                     'resilience4j.circuitbreaker.test.not_permitted',
                                     'resilience4j.circuitbreaker.test.successful',
                                     'resilience4j.circuitbreaker.test.failure_rate',
                                     'resilience4j.circuitbreaker.test.slow_call_rate',
                                     'resilience4j.ratelimiter.test.available_permissions',
                                     'resilience4j.ratelimiter.test.number_of_waiting_threads',
                                     'resilience4j.retry.test.successful_calls_without_retry',
                                     'resilience4j.retry.test.successful_calls_with_retry',
                                     'resilience4j.retry.test.failed_calls_without_retry',
                                     'resilience4j.retry.test.failed_calls_with_retry',
                                     'resilience4j.bulkhead.test.available_concurrent_calls',
                                     'resilience4j.bulkhead.test.max_allowed_concurrent_calls'].toSet()
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
        families == ['resilience4j_bulkhead_available_concurrent_calls',
                     'resilience4j_bulkhead_max_allowed_concurrent_calls',
                     'resilience4j_circuitbreaker_slow_call_rate',
                     'resilience4j_circuitbreaker_slow_calls',
                     'resilience4j_circuitbreaker_buffered_calls',
                     'resilience4j_circuitbreaker_calls',
                     'resilience4j_circuitbreaker_state',
                     'resilience4j_circuitbreaker_failure_rate',
                     'resilience4j_ratelimiter_available_permissions',
                     'resilience4j_ratelimiter_waiting_threads',
                     'resilience4j_retry_calls',
                     'resilience4j_thread_pool_bulkhead_available_queue_capacity',
                     'resilience4j_thread_pool_bulkhead_current_thread_pool_size'].sort()
    }

    static class Something {

        @Timed(name = "test", absolute = true)
        @CircuitBreaker(name = "test")
        String name() {
            "dan"
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
