package io.github.resilience4j.service.test;


import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.stereotype.Component;

import java.io.IOException;

@RateLimiter(name = DummyService.BACKEND)
@Component(value = "rateLimiterDummyService")
public class RateLimiterDummyServiceImpl implements DummyService {

    @Override
    public void doSomething(boolean throwBackendTrouble) throws IOException {
        if (throwBackendTrouble) {
            throw new IOException("Test Message");
        }
    }
}
