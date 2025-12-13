package io.github.resilience4j.springboot.service.test;


import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.springboot.service.test.bulkhead.BulkheadDummyService;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.vavr.control.Try;
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
    @TimeLimiter(name = BACKEND)
    public CompletableFuture<String> doSomethingAsync(boolean throwBackendTrouble)
        throws IOException {
        if (throwBackendTrouble) {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(new IOException("Test Message"));
            return future;
        }
        return CompletableFuture.supplyAsync(() -> "Test result");
    }

    @Override
    @Bulkhead(name = BulkheadDummyService.BACKEND_D, type = Bulkhead.Type.THREADPOOL)
    @TimeLimiter(name = BACKEND_B)
    public CompletableFuture<String> longDoSomethingAsync() {
        Try.run(() -> Thread.sleep(2000));
        return CompletableFuture.completedFuture("Test result");
    }
}
