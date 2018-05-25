package io.github.resilience4j.service.test;


import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static io.github.resilience4j.service.test.DummyService.BACKEND_B;

@CircuitBreaker(backend = BACKEND_B)
@RateLimiter(name = BACKEND_B)
@Qualifier(BACKEND_B)
@Component
public class DummyServiceBImpl implements DummyService {
    @Override
    public void doSomething(boolean throwBackendTrouble) throws IOException {
        if (throwBackendTrouble) {
            throw new IOException("Test Message");
        }
    }
}
