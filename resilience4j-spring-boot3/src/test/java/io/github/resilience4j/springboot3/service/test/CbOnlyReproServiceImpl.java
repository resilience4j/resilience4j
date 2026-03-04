package io.github.resilience4j.springboot3.service.test;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Component;

import java.io.IOException;

@CircuitBreaker(name = DummyService.BACKEND)
@Component
public class CbOnlyReproServiceImpl implements CbOnlyReproService {

    @Override
    public void doSomething(boolean throwBackendTrouble) throws IOException {
        if (throwBackendTrouble) {
            throw new IOException("Test Message");
        }
    }
}
