/*
 * Copyright 2019 Mahmoud Romeh
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
package io.github.resilience4j.service.test;

import java.io.IOException;

import org.assertj.core.util.Arrays;
import org.springframework.stereotype.Component;

import io.github.resilience4j.circuitbreaker.annotation.ApiType;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * reactive test service for web flux API type for the circuit brealer annotation AOP processing
 */
@CircuitBreaker(name = ReactiveDummyService.BACKEND, type = ApiType.WEBFLUX)
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
			return Mono.error(new IllegalArgumentException("FailedFlux"));
		}

		return Mono.just("testMono");
	}

}
