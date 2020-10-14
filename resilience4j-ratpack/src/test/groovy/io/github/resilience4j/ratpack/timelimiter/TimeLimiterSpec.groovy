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

package io.github.resilience4j.ratpack.timelimiter

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.ratpack.Resilience4jModule
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import io.github.resilience4j.timelimiter.TimeLimiterRegistry
import io.github.resilience4j.timelimiter.annotation.TimeLimiter
import ratpack.exec.Promise
import ratpack.test.embed.EmbeddedApp
import ratpack.test.http.TestHttpClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import spock.lang.AutoCleanup
import spock.lang.Specification
import spock.lang.Unroll

import javax.inject.Singleton
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.function.Function

import static ratpack.groovy.test.embed.GroovyEmbeddedApp.ratpack

@Unroll
class TimeLimiterSpec extends Specification {

    @AutoCleanup
    EmbeddedApp app

    @Delegate
    TestHttpClient client

    def "test no time limiter registry installed, app still works"() {
        given:
        app = ratpack {
            bindings {
                module(Resilience4jModule)
                bind(Something)
            }
            handlers {
                get('promise') { Something something ->
                    something.timeLimiterPromise().then {
                        render it
                    }
                }
            }
        }
        client = testHttpClient(app)

        when:
        def actual = get('promise')

        then:
        actual.body.text == 'timeLimiter promise'
        actual.statusCode == 200
    }

    def "test time limit a method via annotation - #path"() {
        given:
        TimeLimiterRegistry registry = TimeLimiterRegistry.of(buildConfig())
        app = ratpack {
            bindings {
                bindInstance(CircuitBreakerRegistry, CircuitBreakerRegistry.ofDefaults())
                bindInstance(TimeLimiterRegistry, registry)
                bind(Something)
                module(Resilience4jModule)
            }
            handlers {
                get('promise') { Something something ->
                    something.timeLimiterPromise().then {
                        render it
                    }
                }
                get('flux') { Something something ->
                    Promise.async { down ->
                        something.timeLimiterFlux().subscribe {
                            down.success(it)
                        } {
                            down.error(it)
                        }
                    }.onError {
                        response.status(500).send(it.toString())
                    }.then {
                        render it
                    }
                }
                get('mono') { Something something ->
                    Promise.async { down ->
                        something.timeLimiterMono().subscribe {
                            down.success(it)
                        } {
                            down.error(it)
                        }
                    }.onError {
                        response.status(500).send(it.toString())
                    }.then {
                        render it
                    }
                }
                get('stage') { Something something ->
                    render something.timeLimiterStage().toCompletableFuture().get()
                }
                get('normal') { Something something ->
                    render something.timeLimiterNormal()
                }
            }
        }
        client = testHttpClient(app)

        when:
        def actual = get(path)

        then:
        if (path == 'normal') {
            assert actual.body.text.contains('java.lang.IllegalArgumentException')
        } else {
            assert actual.body.text.contains('java.util.concurrent.TimeoutException')
        }
        actual.statusCode == 500

        where:
        path      | timeLimiterName
        'promise' | 'test'
        'flux'    | 'test'
        'mono'    | 'test'
        'stage'   | 'test'
        'normal'  | 'test'
    }

    def "test time limit a method via annotation with exception - #path"() {
        given:
        TimeLimiterRegistry registry = TimeLimiterRegistry.of(buildConfig())
        app = ratpack {
            bindings {
                bindInstance(CircuitBreakerRegistry, CircuitBreakerRegistry.ofDefaults())
                bindInstance(TimeLimiterRegistry, registry)
                bind(Something)
                module(Resilience4jModule)
            }
            handlers {
                get('promise') { Something something ->
                    something.timeLimiterPromiseException().then {
                        render it
                    }
                }
                get('flux') { Something something ->
                    Promise.async { down ->
                        something.timeLimiterFluxException().subscribe {
                            down.success(it)
                        } {
                            down.error(it)
                        }
                    }.onError {
                        response.status(500).send(it.toString())
                    }.then {
                        render it
                    }
                }
                get('mono') { Something something ->
                    Promise.async { down ->
                        something.timeLimiterMonoException().subscribe {
                            down.success(it)
                        } {
                            down.error(it)
                        }
                    }.onError {
                        response.status(500).send(it.toString())
                    }.then {
                        render it
                    }
                }
                get('stage') { Something something ->
                    render something.timeLimiterStageException().toCompletableFuture().get()
                }
            }
        }
        client = testHttpClient(app)

        when:
        def actual = get(path)

        then:
        actual.body.text.contains('java.util.concurrent.TimeoutException')
        actual.statusCode == 500

        where:
        path      | timeLimiterName
        'promise' | 'test'
        'flux'    | 'test'
        'mono'    | 'test'
        'stage'   | 'test'
    }

    def "test time limit a method via annotation with fallback - #path"() {
        given:
        TimeLimiterRegistry registry = TimeLimiterRegistry.of(buildConfig())
        app = ratpack {
            bindings {
                bindInstance(CircuitBreakerRegistry, CircuitBreakerRegistry.ofDefaults())
                bindInstance(TimeLimiterRegistry, registry)
                bind(Something)
                module(Resilience4jModule)
            }
            handlers {
                get('promise') { Something something ->
                    something.timeLimiterPromiseFallback().then {
                        render it
                    }
                }
                get('flux') { Something something ->
                    Promise.async { down ->
                        something.timeLimiterFluxFallback().subscribe {
                            down.success(it)
                        } {
                            down.error(it)
                        }
                    }.onError {
                        response.status(500).send(it.toString())
                    }.then {
                        render it
                    }
                }
                get('mono') { Something something ->
                    Promise.async { down ->
                        something.timeLimiterMonoFallback().subscribe {
                            down.success(it)
                        } {
                            down.error(it)
                        }
                    }.onError {
                        response.status(500).send(it.toString())
                    }.then {
                        render it
                    }
                }
                get('stage') { Something something ->
                    render something.timeLimiterStageFallback().toCompletableFuture().get()
                }
            }
        }
        client = testHttpClient(app)

        when:
        def actual = get(path)

        then:
        actual.body.text.contains('recovered')
        actual.statusCode == 200

        where:
        path      | timeLimiterName
        'promise' | 'test'
        'flux'    | 'test'
        'mono'    | 'test'
        'stage'   | 'test'
    }

    def "test time limit a method via annotation with async fallback - #path"() {
        given:
        TimeLimiterRegistry registry = TimeLimiterRegistry.of(buildConfig())
        app = ratpack {
            bindings {
                bindInstance(CircuitBreakerRegistry, CircuitBreakerRegistry.ofDefaults())
                bindInstance(TimeLimiterRegistry, registry)
                bind(Something)
                module(Resilience4jModule)
            }
            handlers {
                get('promise') { Something something ->
                    something.timeLimiterPromiseFallbackPromise().then {
                        render it
                    }
                }
                get('flux') { Something something ->
                    Promise.async { down ->
                        something.timeLimiterFluxFallbackFlux().subscribe {
                            down.success(it)
                        } {
                            down.error(it)
                        }
                    }.onError {
                        response.status(500).send(it.toString())
                    }.then {
                        render it
                    }
                }
                get('mono') { Something something ->
                    Promise.async { down ->
                        something.timeLimiterMonoFallbackMono().subscribe {
                            down.success(it)
                        } {
                            down.error(it)
                        }
                    }.onError {
                        response.status(500).send(it.toString())
                    }.then {
                        render it
                    }
                }
                get('stage') { Something something ->
                    render something.timeLimiterStageFallbackStage().toCompletableFuture().get()
                }
            }
        }
        client = testHttpClient(app) {
            it.readTimeout(Duration.ofMillis(5000))
        }

        when:
        def actual = get(path)

        then:
        actual.body.text.contains('recovered')
        actual.statusCode == 200

        where:
        path      | timeLimiterName
        'promise' | 'test'
        'flux'    | 'test'
        'mono'    | 'test'
        'stage'   | 'test'
    }

    // 500ms timeout
    def buildConfig() {
        TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofMillis(500l))
            .build()
    }

    @Singleton
    static class Something {

        long delay = 600l

        @TimeLimiter(name = "test")
        Promise<String> timeLimiterPromise() {
            Promise.value("timeLimiter promise").defer(Duration.ofMillis(delay))
        }

        @TimeLimiter(name = "test")
        Flux<String> timeLimiterFlux() {
            Flux.just("timeLimiter Flux").delaySubscription(Duration.ofMillis(delay))
        }

        @TimeLimiter(name = "test")
        Mono<String> timeLimiterMono() {
            Mono.just("timeLimiter Mono").delaySubscription(Duration.ofMillis(delay))
        }

        @TimeLimiter(name = "test")
        CompletionStage<String> timeLimiterStage() {
            CompletableFuture.supplyAsync {
                sleep(delay)
                'timeLimiter stage'
            }
        }

        @TimeLimiter(name = "test")
        String timeLimiterNormal() {
            sleep(delay)
            "timeLimiter normal"
        }

        @TimeLimiter(name = "test")
        Promise<String> timeLimiterPromiseException() {
            Promise.<String> async { down ->
                Thread.start {
                    sleep(delay)
                    down.error(new Exception("timeLimiter promise exception"))
                }
            }
        }

        @TimeLimiter(name = "test")
        Flux<Void> timeLimiterFluxException() {
            Flux.just("timeLimiter Flux")
                .map({ throw new Exception("bad") } as Function<String, Void>)
                .delaySubscription(Duration.ofMillis(delay))
        }

        @TimeLimiter(name = "test")
        Mono<Void> timeLimiterMonoException() {
            Mono.just("timeLimiter Mono")
                .map({ throw new Exception("bad") } as Function<String, Void>)
                .delaySubscription(Duration.ofMillis(delay))
        }

        @TimeLimiter(name = "test")
        CompletionStage<Void> timeLimiterStageException() {
            CompletableFuture.supplyAsync {
                sleep(delay)
                throw new Exception('timeLimiter stage exception')
            }
        }

        @TimeLimiter(name = "test", fallbackMethod = "fallback")
        Promise<String> timeLimiterPromiseFallback() {
            Promise.<String> async { down ->
                Thread.start {
                    sleep(delay)
                    down.error(new Exception("timeLimiter promise exception"))
                }
            }
        }

        @TimeLimiter(name = "test", fallbackMethod = "fallback")
        Flux<String> timeLimiterFluxFallback() {
            Flux.just("timeLimiter Flux")
                .map({
                    throw new Exception("bad")
                } as Function<String, String>)
                .delaySubscription(Duration.ofMillis(delay))
        }

        @TimeLimiter(name = "test", fallbackMethod = "fallback")
        Mono<String> timeLimiterMonoFallback() {
            Mono.just("timeLimiter Mono")
                .map({
                    throw new Exception("bad")
                } as Function<String, String>)
                .delaySubscription(Duration.ofMillis(delay))
        }

        @TimeLimiter(name = "test", fallbackMethod = "fallback")
        CompletionStage<String> timeLimiterStageFallback() {
            CompletableFuture.<String> supplyAsync {
                sleep(delay)
                throw new Exception('timeLimiter stage exception')
            }
        }

        @TimeLimiter(name = "test", fallbackMethod = "fallbackPromise")
        Promise<String> timeLimiterPromiseFallbackPromise() {
            Promise.<String> async {
                Thread.start {
                    sleep(delay)
                    it.error(new Exception("timeLimiter promise exception"))
                }
            }
        }

        @TimeLimiter(name = "test", fallbackMethod = "fallbackFlux")
        Flux<String> timeLimiterFluxFallbackFlux() {
            Flux.just("timeLimiter Flux")
                .map({
                    throw new Exception("bad")
                } as Function<String, String>)
                .delaySubscription(Duration.ofMillis(delay))
        }

        @TimeLimiter(name = "test", fallbackMethod = "fallbackMono")
        Mono<String> timeLimiterMonoFallbackMono() {
            Mono.just("timeLimiter Mono")
                .map({
                    throw new Exception("bad")
                } as Function<String, String>)
                .delaySubscription(Duration.ofMillis(delay))
        }

        @TimeLimiter(name = "test", fallbackMethod = "fallbackStage")
        CompletionStage<String> timeLimiterStageFallbackStage() {
            CompletableFuture.<String> supplyAsync {
                sleep(delay)
                throw new Exception('timeLimiter stage exception')
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
