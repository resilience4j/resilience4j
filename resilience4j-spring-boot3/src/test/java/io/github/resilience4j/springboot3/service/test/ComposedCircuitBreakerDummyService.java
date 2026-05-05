package io.github.resilience4j.springboot3.service.test;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Component
public class ComposedCircuitBreakerDummyService {

    public static final String BACKEND = "backendComposed";

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @CircuitBreaker(name = BACKEND)
    public @interface ComposedCircuitBreaker {

        @AliasFor(annotation = CircuitBreaker.class, attribute = "name")
        String name() default BACKEND;
    }

    @ComposedCircuitBreaker
    public void doSomething(boolean throwBackendTrouble) throws IOException {
        if (throwBackendTrouble) {
            throw new IOException("Test Message");
        }
    }
}
