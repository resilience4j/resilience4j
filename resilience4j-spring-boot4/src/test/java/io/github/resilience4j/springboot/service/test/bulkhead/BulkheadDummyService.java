package io.github.resilience4j.springboot.service.test.bulkhead;

import java.util.concurrent.CompletableFuture;

public interface BulkheadDummyService {

    String BACKEND = "backendA";
    String BACKEND_C = "backendC";
    String BACKEND_D = "backendD";
    String BACKEND_E = "backendE";

    void doSomething();

    CompletableFuture<String> doSomethingAsync() throws InterruptedException;

    CompletableFuture<Object> doSomethingAsyncWithThreadLocal() throws InterruptedException;
}
