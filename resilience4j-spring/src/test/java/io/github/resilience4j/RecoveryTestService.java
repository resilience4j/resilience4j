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
import io.github.resilience4j.circuitbreaker.annotation.ApiType;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.recovery.RecoveryFunction;
import io.github.resilience4j.retry.annotation.AsyncRetry;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Component
public class RecoveryTestService {
    public static final String BACKEND = "backendA";

    @CircuitBreaker(name = RecoveryTestService.BACKEND, recovery = TestRecovery.class)
    public String circuitBreaker() {
        throw new RuntimeException("Test");
    }

    @CircuitBreaker(name = RecoveryTestService.BACKEND, type = ApiType.WEBFLUX, recovery = FluxTestRecovery.class)
    public Flux<String> circuitBreakerFlux() {
        return Flux.error(new RuntimeException("Test"));
    }

    @Bulkhead(name = RecoveryTestService.BACKEND, recovery = TestRecovery.class)
    public String bulkhead() {
        throw new RuntimeException("Test");
    }

    @RateLimiter(name = RecoveryTestService.BACKEND, recovery = TestRecovery.class)
    public String rateLimiter() {
        throw new RuntimeException("Test");
    }

    @Retry(name = RecoveryTestService.BACKEND, recovery = TestRecovery.class)
    public String retry() {
        throw new RuntimeException("Test");
    }

    @AsyncRetry(name = RecoveryTestService.BACKEND, recovery = AsyncTestRecovery.class)
    public CompletionStage<String> asyncRetry() {
        throw new RuntimeException("Test");
    }

    public static class TestRecovery implements RecoveryFunction<String> {
        @Override
        public String apply(Throwable throwable) throws Throwable {
            return "recovered";
        }
    }

    public static class AsyncTestRecovery implements RecoveryFunction<CompletionStage<String>> {
        @Override
        public CompletionStage<String> apply(Throwable throwable) throws Throwable {
            return CompletableFuture.supplyAsync(() -> "recovered");
        }
    }

    public static class FluxTestRecovery implements RecoveryFunction<Flux<String>> {
        @Override
        public Flux<String> apply(Throwable throwable) throws Throwable {
            return Flux.just("recovered");
        }
    }
}
