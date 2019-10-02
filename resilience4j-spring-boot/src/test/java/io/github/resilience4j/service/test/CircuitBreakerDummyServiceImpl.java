package io.github.resilience4j.service.test;


import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.io.IOException;
import org.springframework.stereotype.Component;

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
