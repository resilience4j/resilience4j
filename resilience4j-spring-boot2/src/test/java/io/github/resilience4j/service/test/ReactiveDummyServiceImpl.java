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
