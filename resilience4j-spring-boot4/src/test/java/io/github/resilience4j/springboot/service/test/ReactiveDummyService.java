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


import io.reactivex.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;

/**
 * reactive web service test using reactor and RxJava 2 types
 */
public interface ReactiveDummyService {

    String BACKEND = "backendB";

    Flux<String> doSomethingFlux(boolean throwException) throws IOException;

    Mono<String> doSomethingMono(boolean throwException) throws IOException;

    Flowable<String> doSomethingFlowable(boolean throwException) throws IOException;

    Maybe<String> doSomethingMaybe(boolean throwException) throws IOException;

    Single<String> doSomethingSingle(boolean throwException) throws IOException;

    Completable doSomethingCompletable(boolean throwException) throws IOException;

    Observable<String> doSomethingObservable(boolean throwException) throws IOException;
}
