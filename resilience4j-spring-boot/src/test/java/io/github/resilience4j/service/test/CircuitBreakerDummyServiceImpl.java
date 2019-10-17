package io.github.resilience4j.service.test;


import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Component;

import java.io.IOException;

@CircuitBreaker(name = DummyService.BACKEND)
@Component("circuitBreakerDummyService")
public class CircuitBreakerDummyServiceImpl implements DummyService {

    @Override
    public void doSomething(boolean throwBackendTrouble) throws IOException {
        if (throwBackendTrouble) {
            throw new IOException("Test Message");
        }
    }
}
