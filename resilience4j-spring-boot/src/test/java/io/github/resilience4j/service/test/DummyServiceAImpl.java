package io.github.resilience4j.service.test;


import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static io.github.resilience4j.service.test.DummyService.BACKEND_A;

@CircuitBreaker(backend = BACKEND_A)
@RateLimiter(name = BACKEND_A)
@Qualifier(BACKEND_A)

@Component
public class DummyServiceAImpl implements DummyService {
    @Override
    public void doSomething(boolean throwBackendTrouble) throws IOException {
        if (throwBackendTrouble) {
            throw new IOException("Test Message");
        }
    }
}
