package io.github.resilience4j.service.test;

import org.springframework.stereotype.Component;

import io.github.resilience4j.circuitbreaker.IgnoredException;
import io.github.resilience4j.circuitbreaker.RecordedException;
import io.github.resilience4j.circuitbreaker.UnusedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Component
public class AnnotatedConfigDummyService {
    public static final String BACKEND = "backendC";
    public static final String PROPERTY_OVERRIDE_BACKEND = "backendD";

    @CircuitBreaker(name = BACKEND, ignoreExceptions = IgnoredException.class, recordExceptions = RecordedException.class)
    public void doSomething() {
    }

    @CircuitBreaker(name = PROPERTY_OVERRIDE_BACKEND, ignoreExceptions = UnusedException.class, recordExceptions = UnusedException.class)
    public void doSomethingElse() {
    }
}
