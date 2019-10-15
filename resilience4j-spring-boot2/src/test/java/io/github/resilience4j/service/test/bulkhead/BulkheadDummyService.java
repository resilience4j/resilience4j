package io.github.resilience4j.service.test.bulkhead;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public interface BulkheadDummyService {
    String BACKEND = "backendA";
    String BACKEND_C = "backendC";

    void doSomething();

    CompletableFuture<String> doSomethingAsync() throws InterruptedException;

    Future<String> doSomethingAsyncWithFuture() throws InterruptedException;
}
