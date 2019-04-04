/*
 * Copyright 2019 lespinsideg
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
package io.github.resilience4j;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.reactivex.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Component
public class RecoveryTestService {
    public static final String BACKEND = "backendA";

    @CircuitBreaker(name = RecoveryTestService.BACKEND, recovery = "testRecovery")
    public String circuitBreaker() {
        throw new RuntimeException("Test");
    }

    @CircuitBreaker(name = RecoveryTestService.BACKEND, recovery = "testRecovery")
    public CompletionStage<String> asyncCircuitBreaker() {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Test"));

        return future;
    }

    @CircuitBreaker(name = RecoveryTestService.BACKEND, recovery = "fluxTestRecovery")
    public Flux<String> circuitBreakerFlux() {
        return Flux.error(new RuntimeException("Test"));
    }

    @CircuitBreaker(name = RecoveryTestService.BACKEND, recovery = "monoTestRecovery")
    public Mono<String> circuitBreakerMono(String parameter) {
        return Mono.error(new RuntimeException("Test"));
    }

    @CircuitBreaker(name = RecoveryTestService.BACKEND, recovery = "observableTestRecovery")
    public Observable<String> circuitBreakerObservable() {
        return Observable.error(new RuntimeException("Test"));
    }

    @CircuitBreaker(name = RecoveryTestService.BACKEND, recovery = "singleTestRecovery")
    public Single<String> circuitBreakerSingle() {
        return Single.error(new RuntimeException("Test"));
    }

    @CircuitBreaker(name = RecoveryTestService.BACKEND, recovery = "completableTestRecovery")
    public Completable circuitBreakerCompletable() {
        return Completable.error(new RuntimeException("Test"));
    }

    @CircuitBreaker(name = RecoveryTestService.BACKEND, recovery = "maybeTestRecovery")
    public Maybe<String> circuitBreakerMaybe() {
        return Maybe.error(new RuntimeException("Test"));
    }

    @CircuitBreaker(name = RecoveryTestService.BACKEND, recovery = "flowableTestRecovery")
    public Flowable<String> circuitBreakerFlowable() {
        return Flowable.error(new RuntimeException("Test"));
    }

    @Bulkhead(name = RecoveryTestService.BACKEND, recovery = "testRecovery")
    public String bulkhead() {
        throw new RuntimeException("Test");
    }

    @RateLimiter(name = RecoveryTestService.BACKEND, recovery = "testRecovery")
    public String rateLimiter() {
        throw new RuntimeException("Test");
    }

    @Retry(name = RecoveryTestService.BACKEND, recovery = "testRecovery")
    public String retry() {
        throw new RuntimeException("Test");
    }

    @Retry(name = RecoveryTestService.BACKEND, recovery = "testRecovery")
    public CompletionStage<String> asyncRetry() {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Test"));

        return future;
    }

    public String testRecovery(Throwable throwable) {
        return "recovered";
    }

    public Flux<String> fluxTestRecovery(Throwable throwable) {
        return Flux.just("recovered");
    }

    public Mono<String> monoTestRecovery(String parameter, Throwable throwable) {
        return Mono.just(parameter);
    }

    public Observable<String> observableTestRecovery(Throwable throwable) {
        return Observable.just("recovered");
    }

    public Single<String> singleTestRecovery(Throwable throwable) {
        return Single.just("recovered");
    }

    public Completable completableTestRecovery(Throwable throwable) {
        return Completable.complete();
    }

    public Maybe<String> maybeTestRecovery(Throwable throwable) {
        return Maybe.just("recovered");
    }

    public Flowable<String> flowableTestRecovery(Throwable throwable) {
        return Flowable.just("recovered");
    }
}
