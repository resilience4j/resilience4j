package io.github.resilience4j.circuitbreaker.test;


import org.springframework.stereotype.Component;

import java.io.IOException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@CircuitBreaker(backend = DummyService.BACKEND)
@Component
public class DummyServiceImpl implements DummyService {
    @Override
    public void doSomething(boolean throwBackendTrouble) throws IOException {
        if (throwBackendTrouble) {
            throw new IOException("Test Message");
        }
    }
}
