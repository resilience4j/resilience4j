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

import static ratpack.groovy.test.embed.GroovyEmbeddedApp.ratpack

// TODO add docs
class Resilience4jModuleSpec extends Specification {

    @AutoCleanup
    EmbeddedApp app

    @Delegate
    TestHttpClient client

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
        registry.gauges.size() == 8
        registry.gauges.keySet() == ['resilience4j.circuitbreaker.test.buffered', 'resilience4j.circuitbreaker.test.buffered_max', 'resilience4j.circuitbreaker.test.failed', 'resilience4j.circuitbreaker.test.not_permitted', 'resilience4j.circuitbreaker.test.successful', 'resilience4j.ratelimiter.test.available_permissions', 'resilience4j.ratelimiter.test.number_of_waiting_threads', 'resilience4j.retry.test.retry_max_ratio'].toSet()
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
        families == ['resilience4j_circuitbreaker_calls', 'resilience4j_circuitbreaker_states', 'resilience4j_ratelimiter'].sort()
    }

    static class Something {

        @Timed(name = "test", absolute = true)
        @CircuitBreaker(name = "test")
        String name() {
            "dan"
        }

    }

}