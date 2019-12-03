package io.github.resilience4j.circuitbreaker.autoconfigure;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.vavr.Tuple2;

public interface Customizer<T> {

    Tuple2<String, CircuitBreakerConfig.Builder> customize(CircuitBreakerConfig.Builder builder);
}
