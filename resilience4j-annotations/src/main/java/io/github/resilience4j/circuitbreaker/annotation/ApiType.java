package io.github.resilience4j.circuitbreaker.annotation;

/**
 * the API return type which will be used into the circuit breaker annotation
 *
 * @see io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
 */
public enum ApiType {

	DEFAULT, WEBFLUX
}
