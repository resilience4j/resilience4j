package io.github.resilience4j.service.test.bulkhead;

import java.util.concurrent.CompletableFuture;

public interface BulkheadDummyService {

    String BACKEND = "backendA";
    String BACKEND_C = "backendC";

    void doSomething();

    CompletableFuture<String> doSomethingAsync() throws InterruptedException;
}
