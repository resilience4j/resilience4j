package io.github.resilience4j.spring6;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Component
@ComposedClassLevelCircuitBreakerService.ComposedCircuitBreakerClassLevel
public class ComposedClassLevelCircuitBreakerService {

    private static final String BACKEND = TestDummyService.BACKEND;

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @CircuitBreaker(name = BACKEND, fallbackMethod = "recovery")
    public @interface ComposedCircuitBreakerClassLevel {
        @AliasFor(annotation = CircuitBreaker.class, attribute = "name")
        String name() default BACKEND;

        @AliasFor(annotation = CircuitBreaker.class, attribute = "fallbackMethod")
        String fallbackMethod() default "recovery";
    }

    public String sync() {
        throw new RuntimeException("Test");
    }

    public String recovery(RuntimeException throwable) {
        return "recovered";
    }
}
