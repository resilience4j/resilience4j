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

package io.github.resilience4j.ratpack.circuitbreaker


import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.github.resilience4j.ratpack.Resilience4jModule
import ratpack.exec.Promise
import ratpack.test.embed.EmbeddedApp
import ratpack.test.http.TestHttpClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import spock.lang.AutoCleanup
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.function.Consumer
import java.util.function.Function

import static ratpack.groovy.test.embed.GroovyEmbeddedApp.ratpack

@Unroll
class CircuitBreakerSpec extends Specification {

    @AutoCleanup
    EmbeddedApp app

    @Delegate
    TestHttpClient client

    def "test no circuit breaker registry installed, app still works"() {
        given:
        app = ratpack {
            bindings {
                module(Resilience4jModule)
                bind(Something)
            }
            handlers {
                get('promise') { Something something ->
                    something.breakerPromise().then {
                        render it
                    }
                }
            }
        }
        client = testHttpClient(app)

        when:
        def actual = get('promise')

        then:
        actual.body.text == 'breaker promise'
    }

    def "test circuit break a method via annotation with fallback - #path"() {
        given:
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(buildConfig())
        app = ratpack {
            bindings {
                bindInstance(CircuitBreakerRegistry, registry)
                bindInstance(RateLimiterRegistry, RateLimiterRegistry.of(RateLimiterConfig.ofDefaults()))
                bind(Something)
                module(Resilience4jModule)
            }
            handlers {
                get('promise') { Something something ->
                    something.breakerPromise().then {
                        render it
                    }
                }
                get('promiseBad') { Something something ->
                    something.breakerPromiseBad().then {
                        render it
                    }
                }
                get('promiseFallback') { Something something ->
                    something.breakerPromiseFallback().then {
                        render it
                    }
                }
                get('promiseFallbackParams') { Something something ->
                    something.breakerPromiseFallbackParams().then {
                        render it
                    }
                }
                get('stage') { Something something ->
                    render something.breakerStage().toCompletableFuture().get()
                }
                get('stageBad') { Something something ->
                    render something.breakerStageBad().toCompletableFuture().get()
                }
                get('stageFallback') { Something something ->
                    render something.breakerStageFallback().toCompletableFuture().get()
                }
                get('flow') { Something something ->
                    something.breakerFlow().subscribe {
                        render it
                    }
                }
                get('flowBad') { Something something ->
                    something.breakerFlowBad().subscribe {
                        render it
                    }
                }
                get('flowFallback') { Something something ->
                    something.breakerFlowFallback().subscribe {
                        render it
                    }
                }
                get('mono') { Something something ->
                    something.breakerMono().subscribe({
                        render it
                    } as Consumer<String>)
                }
                get('monoBad') { Something something ->
                    something.breakerMonoBad().subscribe({
                        render it
                    } as Consumer<Void>)
                }
                get('monoFallback') { Something something ->
                    something.breakerMonoFallback().subscribe({
                        render it
                    } as Consumer<Void>)
                }
                get('normal') { Something something ->
                    render something.breakerNormal()
                }
                get('normalBad') { Something something ->
                    render something.breakerNormalBad()
                }
                get('normalFallback') { Something something ->
                    render something.breakerNormalFallback()
                }
            }
        }
        client = testHttpClient(app)
        def breaker = registry.circuitBreaker(breakerName)

        when:
        def actual = get(path)

        then:
        actual.body.text == expectedText
        actual.statusCode == 200
        breaker.tryAcquirePermission()
        breaker.state == io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED

        when:
        get(badPath)
        actual = get(badPath)

        then:
        actual.statusCode == 500
        !breaker.tryAcquirePermission()
        breaker.state == io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN

        when:
        get(fallbackPath)
        actual = get(fallbackPath)

        then:
        actual.body.text == "recovered"
        actual.statusCode == 200
        !breaker.tryAcquirePermission()
        breaker.state == io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN

        where:
        path      | badPath      | fallbackPath      | breakerName | expectedText
        'promise' | 'promiseBad' | 'promiseFallback' | 'test'      | 'breaker promise'
        'stage'   | 'stageBad'   | 'stageFallback'   | 'test'      | 'breaker stage'
        'flow'    | 'flowBad'    | 'flowFallback'    | 'test'      | 'breaker flow'
        'mono'    | 'monoBad'    | 'monoFallback'    | 'test'      | 'breaker mono'
        'normal'  | 'normalBad'  | 'normalFallback'  | 'test'      | 'breaker normal'
    }

    def "test circuit break a method via annotation with fallback params"() {
        given:
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(buildConfig())
        app = ratpack {
            bindings {
                bindInstance(CircuitBreakerRegistry, registry)
                bindInstance(RateLimiterRegistry, RateLimiterRegistry.of(RateLimiterConfig.ofDefaults()))
                bind(Something)
                module(Resilience4jModule)
            }
            handlers {
                get('promiseFallbackParams') { Something something ->
                    something.breakerPromiseFallbackParams().then {
                        render it
                    }
                }
            }
        }
        client = testHttpClient(app)
        def breaker = registry.circuitBreaker('test')

        when:
        get('promiseFallbackParams')
        def actual = get('promiseFallbackParams')

        then:
        actual.body.text == "recovered"
        actual.statusCode == 200
        !breaker.tryAcquirePermission()
        breaker.state == io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN
    }

    def "test circuit break a method via annotation with fallback params returning promise"() {
        given:
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(buildConfig())
        app = ratpack {
            bindings {
                bindInstance(CircuitBreakerRegistry, registry)
                bindInstance(RateLimiterRegistry, RateLimiterRegistry.of(RateLimiterConfig.ofDefaults()))
                bind(Something)
                module(Resilience4jModule)
            }
            handlers {
                get('promiseFallbackParamsPromise') { Something something ->
                    something.breakerPromiseFallbackParamsPromise().then {
                        render it
                    }
                }
            }
        }
        client = testHttpClient(app)
        def breaker = registry.circuitBreaker('test')

        when:
        get('promiseFallbackParamsPromise')
        def actual = get('promiseFallbackParamsPromise')

        then:
        actual.body.text == "recovered"
        actual.statusCode == 200
        !breaker.tryAcquirePermission()
        breaker.state == io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN
    }

    def "test circuit break a method via annotation with fallback params returning CompletionStage"() {
        given:
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(buildConfig())
        app = ratpack {
            bindings {
                bindInstance(CircuitBreakerRegistry, registry)
                bindInstance(RateLimiterRegistry, RateLimiterRegistry.of(RateLimiterConfig.ofDefaults()))
                bind(Something)
                module(Resilience4jModule)
            }
            handlers {
                get('promiseFallbackParamsStage') { Something something ->
                    render something.breakerStageFallbackStage().toCompletableFuture().get()
                }
            }
        }
        client = testHttpClient(app)
        def breaker = registry.circuitBreaker('test')

        when:
        get('promiseFallbackParamsStage')
        def actual = get('promiseFallbackParamsStage')

        then:
        actual.body.text == "recovered"
        actual.statusCode == 200
        !breaker.tryAcquirePermission()
        breaker.state == io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN
    }

    def "test circuit break a method via annotation with fallback params returning Flux"() {
        given:
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(buildConfig())
        app = ratpack {
            bindings {
                bindInstance(CircuitBreakerRegistry, registry)
                bindInstance(RateLimiterRegistry, RateLimiterRegistry.of(RateLimiterConfig.ofDefaults()))
                bind(Something)
                module(Resilience4jModule)
            }
            handlers {
                get('promiseFallbackParamsFlow') { Something something ->
                    something.breakerFlowFallbackFlow("q").subscribe {
                        render it
                    }
                }
            }
        }
        client = testHttpClient(app)
        def breaker = registry.circuitBreaker('test')

        when:
        get('promiseFallbackParamsFlow')
        def actual = get('promiseFallbackParamsFlow')

        then:
        actual.body.text == "recovered"
        actual.statusCode == 200
        !breaker.tryAcquirePermission()
        breaker.state == io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN
    }

    def "test circuit break a method via annotation with fallback params returning Mono"() {
        given:
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(buildConfig())
        app = ratpack {
            bindings {
                bindInstance(CircuitBreakerRegistry, registry)
                bindInstance(RateLimiterRegistry, RateLimiterRegistry.of(RateLimiterConfig.ofDefaults()))
                bind(Something)
                module(Resilience4jModule)
            }
            handlers {
                get('promiseFallbackParamsMono') { Something something ->
                    something.breakerMonoFallbackMono("q").subscribe {
                        render it
                    }
                }
            }
        }
        client = testHttpClient(app)
        def breaker = registry.circuitBreaker('test')

        when:
        get('promiseFallbackParamsMono')
        def actual = get('promiseFallbackParamsMono')

        then:
        actual.body.text == "recovered"
        actual.statusCode == 200
        !breaker.tryAcquirePermission()
        breaker.state == io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN
    }

    def buildConfig() {
        CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .permittedNumberOfCallsInHalfOpenState(2)
                .slidingWindowSize(2)
                .build()
    }

    static class Something {

        @CircuitBreaker(name = "test")
        Promise<String> breakerPromise() {
            Promise.<String> async {
                it.success("breaker promise")
            }
        }

        @CircuitBreaker(name = "test")
        Promise<String> breakerPromiseBad() {
            Promise.<String> async {
                it.error(new Exception("breaker promise bad"))
            }
        }

        @CircuitBreaker(name = "test", fallbackMethod = "fallback")
        Promise<String> breakerPromiseFallback() {
            Promise.<String> async {
                it.error(new Exception("breaker promise bad"))
            }
        }

        @CircuitBreaker(name = "test", fallbackMethod = "fallbackParams")
        Promise<String> breakerPromiseFallbackParams(String s) {
            Promise.<String> async {
                it.error(new Exception("$s breaker promise bad"))
            }
        }

        @CircuitBreaker(name = "test", fallbackMethod = "fallbackParamsPromise")
        Promise<String> breakerPromiseFallbackParamsPromise(String s) {
            Promise.<String> async {
                it.error(new Exception("$s breaker promise bad"))
            }
        }

        @CircuitBreaker(name = "test")
        CompletionStage<String> breakerStage() {
            CompletableFuture.supplyAsync { 'breaker stage' }
        }

        @CircuitBreaker(name = "test")
        CompletionStage<Void> breakerStageBad() {
            CompletableFuture.supplyAsync { throw new RuntimeException("bad") }
        }

        @CircuitBreaker(name = "test", fallbackMethod = "fallbackParams")
        CompletionStage<String> breakerStageFallback(String s) {
            CompletableFuture.supplyAsync { throw new RuntimeException("$s bad") }
        }

        @CircuitBreaker(name = "test", fallbackMethod = "fallbackParamsStage")
        CompletionStage<Void> breakerStageFallbackStage() {
            CompletableFuture.supplyAsync { throw new RuntimeException("bad") }
        }

        @CircuitBreaker(name = "test")
        Flux<String> breakerFlow() {
            Flux.just("breaker flow")
        }

        @CircuitBreaker(name = "test")
        Flux<Void> breakerFlowBad() {
            Flux.just("breaker flow").map({ throw new Exception("bad") } as Function<String, Void>)
        }

        @CircuitBreaker(name = "test", fallbackMethod = "fallback")
        Flux<Void> breakerFlowFallback() {
            Flux.just("breaker flow").map({ throw new Exception("bad") } as Function<String, Void>)
        }

        @CircuitBreaker(name = "test", fallbackMethod = "fallbackParamsFlux")
        Flux<String> breakerFlowFallbackFlow(String s) {
            Flux.just("breaker flow").map({ throw new Exception("$s bad") } as Function<String, String>)
        }

        @CircuitBreaker(name = "test")
        Mono<String> breakerMono() {
            Mono.just("breaker mono")
        }

        @CircuitBreaker(name = "test")
        Mono<Void> breakerMonoBad() {
            Mono.just("breaker mono").map({ throw new Exception("bad") } as Function<String, Void>)
        }

        @CircuitBreaker(name = "test", fallbackMethod = "fallback")
        Mono<Void> breakerMonoFallback() {
            Mono.just("breaker mono").map({ throw new Exception("bad") } as Function<String, Void>)
        }

        @CircuitBreaker(name = "test", fallbackMethod = "fallbackParamsMono")
        Mono<String> breakerMonoFallbackMono(String s) {
            Mono.just("breaker flow").map({ throw new Exception("$s bad") } as Function<String, String>)
        }

        @CircuitBreaker(name = "test")
        String breakerNormal() {
            "breaker normal"
        }

        @CircuitBreaker(name = "test")
        String breakerNormalBad() {
            throw new Exception("bad")
        }

        @CircuitBreaker(name = "test", fallbackMethod = "fallback")
        String breakerNormalFallback() {
            throw new Exception("bad")
        }

        String fallback(Throwable throwable) {
            "recovered"
        }

        String fallbackParams(String s, Throwable throwable) {
            "recovered"
        }

        Promise<String> fallbackParamsPromise(String s, Throwable throwable) {
            Promise.value("recovered")
        }

        CompletionStage<String> fallbackParamsStage(Throwable throwable) {
            def future = new CompletableFuture<String>()
            future.complete("recovered")
            return future
        }

        Flux<String> fallbackParamsFlux(String s, Throwable throwable) {
            Flux.just("recovered")
        }

        Mono<String> fallbackParamsMono(String s, Throwable throwable) {
            Mono.just("recovered")
        }
    }

}