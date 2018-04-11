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

package io.github.resilience4j.ratpack.retry

import io.github.resilience4j.ratpack.Resilience4jModule
import io.github.resilience4j.ratpack.recovery.RecoveryFunction
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import ratpack.exec.Promise
import ratpack.test.embed.EmbeddedApp
import ratpack.test.http.TestHttpClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import spock.lang.AutoCleanup
import spock.lang.Specification
import spock.lang.Unroll

import javax.inject.Inject
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.function.Function

import static ratpack.groovy.test.embed.GroovyEmbeddedApp.ratpack

@Unroll
class RetrySpec extends Specification {

    @AutoCleanup
    EmbeddedApp app

    @Delegate
    TestHttpClient client

    AtomicInteger times = new AtomicInteger(0)

    def "test no retry registry installed, app still works"() {
        given:
        app = ratpack {
            bindings {
                module(Resilience4jModule)
                bindInstance(Something, new Something(times))
            }
            handlers {
                get('promise') { Something something ->
                    something.retryPromise().then {
                        render it
                    }
                }
            }
        }
        client = testHttpClient(app)

        when:
        def actual = get('promise')

        then:
        actual.body.text == 'retry promise'
    }

    def "test retry a method via annotation with fallback"() {
        given:
        RetryRegistry registry = RetryRegistry.of(buildConfig())
        app = ratpack {
            bindings {
                bindInstance(RetryRegistry, registry)
                bindInstance(AtomicInteger, times)
                bind(Something)
                module(Resilience4jModule)
            }
            handlers {
                get('promise') { Something something ->
                    something.retryPromise().then {
                        render it
                    }
                }
                get('promiseBad') { Something something ->
                    something.retryPromiseBad().then {
                        render it
                    }
                }
                get('promiseRecover') { Something something ->
                    something.retryPromiseRecovery().then {
                        render it
                    }
                }
                get('Flux') { Something something ->
                    something.retryFlux().subscribe {
                        render it[0]
                    }
                }
                get('FluxBad') { Something something ->
                    something.retryFluxBad().subscribe {
                        render it
                    }
                }
                get('FluxRecover') { Something something ->
                    something.retryFluxRecovery().subscribe {
                        render it
                    }
                }
                get('Mono') { Something something ->
                    something.retryMono().subscribe({
                        render it
                    } as Consumer<String>)
                }
                get('MonoBad') { Something something ->
                    something.retryMonoBad().subscribe({
                        render it
                    } as Consumer<Void>)
                }
                get('MonoRecover') { Something something ->
                    something.retryMonoRecovery().subscribe({
                        render it
                    } as Consumer<Void>)
                }
                get('stage') { Something something ->
                    render something.retryStage().toCompletableFuture().get()
                }
                get('stageBad') { Something something ->
                    render something.retryStageBad().toCompletableFuture().get()
                }
                get('stageRecover') { Something something ->
                    render something.retryStageRecover().toCompletableFuture().get()
                }
                get('normal') { Something something ->
                    render something.retryNormal()
                }
                get('normalBad') { Something something ->
                    render something.retryNormalBad()
                }
                get('normalRecover') { Something something ->
                    render something.retryNormalRecover()
                }
            }
        }
        client = testHttpClient(app)
        registry.retry(retryName)

        when:
        times.set(0)
        def actual = get(path)

        then:
        actual.body.text == expectedText
        actual.statusCode == 200
        times.get() == 1

        when:
        times.set(0)
        actual = get(badPath)

        then:
        actual.statusCode == badStatus
        times.get() == 3

        when:
        times.set(0)
        actual = get(recoverPath)

        then:
        actual.body.text == "recovered"
        actual.statusCode == 200
        times.get() == 3

        where:
        path         | badPath         | recoverPath         | retryName | expectedText       | badStatus
        'promise'    | 'promiseBad'    | 'promiseRecover'    | 'test'    | 'retry promise'    | 500
        'Flux'       | 'FluxBad'       | 'FluxRecover'       | 'test'    | 'retry Flux'       | 500
        'Mono'       | 'MonoBad'       | 'MonoRecover'       | 'test'    | 'retry Mono'       | 500
        'stage'      | 'stageBad'      | 'stageRecover'      | 'test'    | 'retry stage'      | 500
        'normal'     | 'normalBad'     | 'normalRecover'     | 'test'    | 'retry normal'     | 500
    }

    def buildConfig() {
        RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .build()
    }

    static class Something {

        AtomicInteger times

        @Inject
        Something(AtomicInteger times) {
            this.times = times
        }

        @Retry(name = "test")
        Promise<String> retryPromise() {
            Promise.async {
                times.getAndIncrement()
                it.success("retry promise")
            }
        }

        @Retry(name = "test")
        Promise<String> retryPromiseBad() {
            Promise.async {
                times.getAndIncrement()
                it.error(new Exception("retry promise bad"))
            }
        }

        @Retry(name = "test", recovery = MyRecoveryFunction)
        Promise<String> retryPromiseRecovery() {
            Promise.async {
                times.getAndIncrement()
                it.error(new Exception("retry promise bad"))
            }
        }

        @Retry(name = "test")
        Flux<String> retryFlux() {
            Mono.fromCallable {
                times.getAndIncrement()
                "retry Flux"
            }.flux()
        }

        @Retry(name = "test")
        Flux<Void> retryFluxBad() {
            Mono.fromCallable {
                times.getAndIncrement()
                "retry Flux"
            }.map({
                throw new Exception("retry Flux bad")
            } as Function<String, Void>).flux()
        }

        @Retry(name = "test", recovery = MyRecoveryFunction)
        Flux<Void> retryFluxRecovery() {
            Mono.fromCallable {
                times.getAndIncrement()
                "retry Flux"
            }.map({
                throw new Exception("retry Flux bad")
            } as Function<String, Void>).flux()
        }

        @Retry(name = "test")
        Mono<String> retryMono() {
            Mono.fromCallable {
                times.getAndIncrement()
                "retry Mono"
            }
        }

        @Retry(name = "test")
        Mono<Void> retryMonoBad() {
            Mono.fromCallable {
                times.getAndIncrement()
                "retry Mono"
            }.map({
                throw new Exception("retry Mono bad")
            } as Function<String, Void>)
        }

        @Retry(name = "test", recovery = MyRecoveryFunction)
        Mono<Void> retryMonoRecovery() {
            Mono.fromCallable {
                times.getAndIncrement()
                "retry Mono"
            }.map({
                throw new Exception("retry Mono bad")
            } as Function<String, Void>)
        }

        @Retry(name = "test")
        CompletionStage<String> retryStage() {
            CompletableFuture.supplyAsync {
                times.getAndIncrement()
                'retry stage'
            }
        }

        @Retry(name = "test")
        CompletionStage<Void> retryStageBad() throws Exception {
            CompletableFuture.supplyAsync {
                times.getAndIncrement()
                throw new RuntimeException("bad")
            }
        }

        @Retry(name = "test", recovery = MyRecoveryFunction)
        CompletionStage<Void> retryStageRecover() throws Exception {
            CompletableFuture.supplyAsync {
                times.getAndIncrement()
                throw new RuntimeException("bad")
            }
        }

        @Retry(name = "test")
        String retryNormal() {
            times.getAndIncrement()
            "retry normal"
        }

        @Retry(name = "test")
        String retryNormalBad() {
            times.getAndIncrement()
            throw new Exception("bad")
        }

        @Retry(name = "test", recovery = MyRecoveryFunction)
        String retryNormalRecover() {
            times.getAndIncrement()
            throw new Exception("bad")
        }
    }

    static class MyRecoveryFunction implements RecoveryFunction<String> {
        @Override
        String apply(Throwable t) throws Exception {
            "recovered"
        }
    }

}