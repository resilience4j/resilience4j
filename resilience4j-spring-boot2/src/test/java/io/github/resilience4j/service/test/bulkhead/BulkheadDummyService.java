package io.github.resilience4j.service.test.bulkhead;

import java.util.concurrent.CompletableFuture;

public interface BulkheadDummyService {

    String BACKEND = "backendA";
    String BACKEND_C = "backendC";
    String BACKEND_D = "backendD";

    void doSomething();

    CompletableFuture<String> doSomethingAsync() throws InterruptedException;

    CompletableFuture<Object> doSomethingAsyncWithThreadLocal() throws InterruptedException;
}
