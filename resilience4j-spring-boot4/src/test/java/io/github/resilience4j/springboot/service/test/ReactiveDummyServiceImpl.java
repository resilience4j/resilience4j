/*
 * Copyright 2025 Mahmoud Romeh, Artur Havliukovskyi
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
package io.github.resilience4j.springboot.service.test;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.reactivex.*;
import org.assertj.core.util.Arrays;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;

/**
 * reactive test service for Reactor and RxJava API type for the circuit breaker annotation AOP
 * processing
 */
@CircuitBreaker(name = ReactiveDummyService.BACKEND)
@RateLimiter(name = ReactiveDummyService.BACKEND)
@Component
public class ReactiveDummyServiceImpl implements ReactiveDummyService {

    @Override
    public Flux<String> doSomethingFlux(boolean throwException) throws IOException {

        if (throwException) {
            return Flux.error(new IllegalArgumentException("FailedFlux"));
        }

        return Flux.fromArray(Arrays.array("test", "test2"));
    }

    @Override
    public Mono<String> doSomethingMono(boolean throwException) throws IOException {
        if (throwException) {
            return Mono.error(new IllegalArgumentException("Failed"));
        }

        return Mono.just("testMono");
    }

    @Override
    public Flowable<String> doSomethingFlowable(boolean throwException) throws IOException {
        if (throwException) {
            return Flowable.error(new IllegalArgumentException("Failed"));
        }
        return Flowable.just("testMaybe");
    }

    @Override
    public Maybe<String> doSomethingMaybe(boolean throwException) throws IOException {
        if (throwException) {
            return Maybe.error(new IllegalArgumentException("Failed"));
        }
        return Maybe.just("testMaybe");
    }

    @Override
    public Single<String> doSomethingSingle(boolean throwException) throws IOException {
        if (throwException) {
            return Single.error(new IllegalArgumentException("Failed"));
        }
        return Single.just("testSingle");
    }

    @Override
    public Completable doSomethingCompletable(boolean throwException) throws IOException {
        if (throwException) {
            return Completable.error(new IllegalArgumentException("Failed"));
        }
        return Completable.complete();
    }

    @Override
    public Observable<String> doSomethingObservable(boolean throwException) throws IOException {
        if (throwException) {
            return Observable.error(new IllegalArgumentException("Failed"));
        }
        return Observable.just("testObservable");
    }

}
