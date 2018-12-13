package io.github.resilience4j.service.test;


import java.io.IOException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive web service
 */
public interface ReactiveDummyService {
	String BACKEND = "backendB";

	Flux<String> doSomethingFlux(boolean throwException) throws IOException;

	Mono<String> doSomethingMono(boolean throwException) throws IOException;
}
