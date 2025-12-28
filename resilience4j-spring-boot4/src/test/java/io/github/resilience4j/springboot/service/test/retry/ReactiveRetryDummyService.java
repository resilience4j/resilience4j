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
package io.github.resilience4j.springboot.service.test.retry;


import io.reactivex.Flowable;
import reactor.core.publisher.Flux;

/**
 * reactive web service test using reactor types and RxJava2 for Retry
 */
public interface ReactiveRetryDummyService {

    String BACKEND_C = "retryBackendC";

    Flux<String> doSomethingFlux(boolean throwException);

    Flowable<String> doSomethingFlowable(boolean throwException);
}
