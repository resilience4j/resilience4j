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

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.github.resilience4j.ratelimiter.annotation.RateLimiter
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
import java.util.function.Function

import static ratpack.groovy.test.embed.GroovyEmbeddedApp.ratpack

@Unroll
class RateLimiterSpec extends Specification {

    @AutoCleanup
    EmbeddedApp app

    @Delegate
    TestHttpClient client

    def "test no rate limiter registry installed, app still works"() {
        given:
        app = ratpack {
            bindings {
                module(Resilience4jModule)
                bind(Something)
            }
            handlers {
                get('promise') { Something something ->
                    something.rateLimiterPromise().then {
                        render it
                    }
                }
            }
        }
        client = testHttpClient(app)

        when:
        def actual = get('promise')

        then:
        actual.body.text == 'ratelimiter promise'
        actual.statusCode == 200
    }

    def "test rate limit a method via annotation - #path"() {
        given:
        RateLimiterRegistry registry = RateLimiterRegistry.of(buildConfig())
        app = ratpack {
            bindings {
                bindInstance(CircuitBreakerRegistry, CircuitBreakerRegistry.ofDefaults())
                bindInstance(RateLimiterRegistry, registry)
                bind(Something)
                module(Resilience4jModule)
            }
            handlers {
                get('promise') { Something something ->
                    something.rateLimiterPromise().then {
                        render it
                    }
                }
                get('flux') { Something something ->
                    something.rateLimiterFlux().subscribe {
                        render it
                    } {
                        response.status(500).send(it.toString())
                    }
                }
                get('mono') { Something something ->
                    something.rateLimiterMono().subscribe {
                        render it
                    } {
                        response.status(500).send(it.toString())
                    }
                }
                get('stage') { Something something ->
                    render something.rateLimiterStage().toCompletableFuture().get()
                }
                get('normal') { Something something ->
                    render something.rateLimiterNormal()
                }
            }
        }
        client = testHttpClient(app)

        when:
        def actual = null
        for (int i = 0; i <= 3; i++) {
            actual = get(path)
        }

        then:
        actual.body.text.contains('io.github.resilience4j.ratelimiter.RequestNotPermitted: RateLimiter \'test\' does not permit further calls')
        actual.statusCode == 500

        where:
        path      | rateLimiterName
        'promise' | 'test'
        'flux'    | 'test'
        'mono'    | 'test'
        'stage'   | 'test'
        'normal'  | 'test'
    }

    def "test rate limit a method via annotation with exception - #path"() {
        given:
        RateLimiterRegistry registry = RateLimiterRegistry.of(buildConfig())
        app = ratpack {
            bindings {
                bindInstance(CircuitBreakerRegistry, CircuitBreakerRegistry.ofDefaults())
                bindInstance(RateLimiterRegistry, registry)
                bind(Something)
                module(Resilience4jModule)
            }
            handlers {
                get('promise') { Something something ->
                    something.rateLimiterPromiseException().then {
                        render it
                    }
                }
                get('flux') { Something something ->
                    something.rateLimiterFluxException().subscribe {
                        render it
                    } {
                        response.status(500).send(it.toString())
                    }
                }
                get('mono') { Something something ->
                    something.rateLimiterMonoException().subscribe {
                        render it
                    } {
                        response.status(500).send(it.toString())
                    }
                }
                get('stage') { Something something ->
                    render something.rateLimiterStageException().toCompletableFuture().get()
                }
                get('normal') { Something something ->
                    render something.rateLimiterNormalException()
                }
            }
        }
        client = testHttpClient(app)

        when:
        def actual = null
        for (int i = 0; i <= 3; i++) {
            actual = get(path)
        }

        then:
        actual.body.text.contains('io.github.resilience4j.ratelimiter.RequestNotPermitted: RateLimiter \'test\' does not permit further calls')
        actual.statusCode == 500

        where:
        path      | rateLimiterName
        'promise' | 'test'
        'flux'    | 'test'
        'mono'    | 'test'
        'stage'   | 'test'
        'normal'  | 'test'
    }

    def "test rate limit a method via annotation with fallback - #path"() {
        given:
        RateLimiterRegistry registry = RateLimiterRegistry.of(buildConfig())
        app = ratpack {
            bindings {
                bindInstance(CircuitBreakerRegistry, CircuitBreakerRegistry.ofDefaults())
                bindInstance(RateLimiterRegistry, registry)
                bind(Something)
                module(Resilience4jModule)
            }
            handlers {
                get('promise') { Something something ->
                    something.rateLimiterPromiseFallback().then {
                        render it
                    }
                }
                get('flux') { Something something ->
                    something.rateLimiterFluxFallback().subscribe {
                        render it
                    }
                }
                get('mono') { Something something ->
                    something.rateLimiterMonoFallback().subscribe {
                        render it
                    }
                }
                get('stage') { Something something ->
                    render something.rateLimiterStageFallback().toCompletableFuture().get()
                }
                get('normal') { Something something ->
                    render something.rateLimiterNormalFallback()
                }
            }
        }
        client = testHttpClient(app)

        when:
        def actual = null
        for (int i = 0; i <= 3; i++) {
            actual = get(path)
        }

        then:
        actual.body.text.contains('recovered')
        actual.statusCode == 200

        where:
        path      | rateLimiterName
        'promise' | 'test'
        'flux'    | 'test'
        'mono'    | 'test'
        'stage'   | 'test'
        'normal'  | 'test'
    }

    def "test rate limit a method via annotation with async fallback - #path"() {
        given:
        RateLimiterRegistry registry = RateLimiterRegistry.of(buildConfig())
        app = ratpack {
            bindings {
                bindInstance(CircuitBreakerRegistry, CircuitBreakerRegistry.ofDefaults())
                bindInstance(RateLimiterRegistry, registry)
                bind(Something)
                module(Resilience4jModule)
            }
            handlers {
                get('promise') { Something something ->
                    something.rateLimiterPromiseFallbackPromise().then {
                        render it
                    }
                }
                get('flux') { Something something ->
                    something.rateLimiterFluxFallbackFlux().subscribe {
                        render it
                    }
                }
                get('mono') { Something something ->
                    something.rateLimiterMonoFallbackMono().subscribe {
                        render it
                    }
                }
                get('stage') { Something something ->
                    render something.rateLimiterStageFallbackStage().toCompletableFuture().get()
                }
            }
        }
        client = testHttpClient(app)

        when:
        def actual = null
        for (int i = 0; i <= 3; i++) {
            actual = get(path)
        }

        then:
        actual.body.text.contains('recovered')
        actual.statusCode == 200

        where:
        path      | rateLimiterName
        'promise' | 'test'
        'flux'    | 'test'
        'mono'    | 'test'
        'stage'   | 'test'
    }

    // 10 events / 1 minute
    def buildConfig() {
        RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofSeconds(60))
            .limitForPeriod(3)
            .timeoutDuration(Duration.ofMillis(20))
            .build()
    }

    static class Something {

        @RateLimiter(name = "test")
        Promise<String> rateLimiterPromise() {
            Promise.<String> async {
                it.success("ratelimiter promise")
            }
        }

        @RateLimiter(name = "test")
        Flux<String> rateLimiterFlux() {
            Flux.just("ratelimiter Flux")
        }

        @RateLimiter(name = "test")
        Mono<String> rateLimiterMono() {
            Mono.just("ratelimiter Mono")
        }

        @RateLimiter(name = "test")
        CompletionStage<String> rateLimiterStage() {
            CompletableFuture.supplyAsync { 'ratelimiter stage' }
        }

        @RateLimiter(name = "test")
        String rateLimiterNormal() {
            "ratelimiter normal"
        }

        @RateLimiter(name = "test")
        Promise<String> rateLimiterPromiseException() {
            Promise.<String> async {
                it.error(new Exception("ratelimiter promise exception"))
            }
        }

        @RateLimiter(name = "test")
        Flux<Void> rateLimiterFluxException() {
            Flux.just("ratelimiter Flux").map({
                throw new Exception("bad")
            } as Function<String, Void>)
        }

        @RateLimiter(name = "test")
        Mono<Void> rateLimiterMonoException() {
            Mono.just("ratelimiter Mono").map({
                throw new Exception("bad")
            } as Function<String, Void>)
        }

        @RateLimiter(name = "test")
        CompletionStage<Void> rateLimiterStageException() {
            CompletableFuture.supplyAsync { throw new Exception('ratelimiter stage exception') }
        }

        @RateLimiter(name = "test")
        String rateLimiterNormalException() {
            throw new Exception("ratelimiter normal exception")
        }

        @RateLimiter(name = "test", fallbackMethod = "fallback")
        Promise<String> rateLimiterPromiseFallback() {
            Promise.<String> async {
                it.error(new Exception("ratelimiter promise exception"))
            }
        }

        @RateLimiter(name = "test", fallbackMethod = "fallback")
        Flux<String> rateLimiterFluxFallback() {
            Flux.just("ratelimiter Flux").map({
                throw new Exception("bad")
            } as Function<String, String>)
        }

        @RateLimiter(name = "test", fallbackMethod = "fallback")
        Mono<String> rateLimiterMonoFallback() {
            Mono.just("ratelimiter Mono").map({
                throw new Exception("bad")
            } as Function<String, String>)
        }

        @RateLimiter(name = "test", fallbackMethod = "fallback")
        CompletionStage<String> rateLimiterStageFallback() {
            CompletableFuture.<String> supplyAsync {
                throw new Exception('ratelimiter stage exception')
            }
        }

        @RateLimiter(name = "test", fallbackMethod = "fallback")
        String rateLimiterNormalFallback() {
            throw new Exception("ratelimiter normal exception")
        }

        @RateLimiter(name = "test", fallbackMethod = "fallbackPromise")
        Promise<String> rateLimiterPromiseFallbackPromise() {
            Promise.<String> async {
                it.error(new Exception("ratelimiter promise exception"))
            }
        }

        @RateLimiter(name = "test", fallbackMethod = "fallbackFlux")
        Flux<String> rateLimiterFluxFallbackFlux() {
            Flux.just("ratelimiter Flux").map({
                throw new Exception("bad")
            } as Function<String, String>)
        }

        @RateLimiter(name = "test", fallbackMethod = "fallbackMono")
        Mono<String> rateLimiterMonoFallbackMono() {
            Mono.just("ratelimiter Mono").map({
                throw new Exception("bad")
            } as Function<String, String>)
        }

        @RateLimiter(name = "test", fallbackMethod = "fallbackStage")
        CompletionStage<String> rateLimiterStageFallbackStage() {
            CompletableFuture.<String> supplyAsync {
                throw new Exception('ratelimiter stage exception')
            }
        }

        Promise<String> fallbackPromise(Throwable throwable) {
            Promise.value("recovered")
        }

        CompletionStage<String> fallbackStage(Throwable throwable) {
            def future = new CompletableFuture<String>()
            future.complete("recovered")
            return future
        }

        Flux<String> fallbackFlux(Throwable throwable) {
            Flux.just("recovered")
        }

        Mono<String> fallbackMono(Throwable throwable) {
            Mono.just("recovered")
        }

        String fallback(Throwable t) throws Exception {
            "recovered"
        }
    }

}