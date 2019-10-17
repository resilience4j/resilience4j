package io.github.resilience4j.service.test;


import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@CircuitBreaker(name = DummyService.BACKEND)
@RateLimiter(name = DummyService.BACKEND)
@Component
public class DummyServiceImpl implements DummyService {

    @Override
    public void doSomething(boolean throwBackendTrouble) throws IOException {
        if (throwBackendTrouble) {
            throw new IOException("Test Message");
        }
    }

    @Override
    public CompletableFuture<String> doSomethingAsync(boolean throwBackendTrouble)
        throws IOException {
        if (throwBackendTrouble) {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(new IOException("Test Message"));
            return future;
        }
        return CompletableFuture.supplyAsync(() -> "Test result");
    }
}
