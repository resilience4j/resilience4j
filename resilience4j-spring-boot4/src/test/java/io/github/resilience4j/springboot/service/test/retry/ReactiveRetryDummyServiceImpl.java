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

import io.github.resilience4j.retry.annotation.Retry;
import io.reactivex.Flowable;
import org.assertj.core.util.Arrays;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * reactive test service for Reactor and RxJava2 retry
 */
@Retry(name = ReactiveRetryDummyService.BACKEND_C)
@Component
public class ReactiveRetryDummyServiceImpl implements ReactiveRetryDummyService {

    @Override
    public Flux<String> doSomethingFlux(boolean throwException) {

        if (throwException) {
            return Flux.error(new IllegalArgumentException("FailedFlux"));
        }

        return Flux.fromArray(Arrays.array("test", "test2"));
    }

    @Override
    public Flowable<String> doSomethingFlowable(boolean throwException) {
        if (throwException) {
            return Flowable.error(new IllegalArgumentException("Failed"));
        }
        return Flowable.just("testMaybe");
    }
}
