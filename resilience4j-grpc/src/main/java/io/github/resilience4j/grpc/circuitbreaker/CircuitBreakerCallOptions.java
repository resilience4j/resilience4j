package io.github.resilience4j.grpc.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.grpc.CallOptions;
import io.grpc.Status;

import java.util.function.Predicate;

public class CircuitBreakerCallOptions {

    public static CallOptions.Key<CircuitBreaker> CIRCUIT_BREAKER = CallOptions.Key
            .create("resilience4j-circuitbreaker");

    public static CallOptions.Key<Predicate<Status>> SUCCESS_STATUS = CallOptions.Key
            .createWithDefault("resilience4j-circuitbreaker-success-status", Status::isOk);

    private CircuitBreakerCallOptions() {}
}
