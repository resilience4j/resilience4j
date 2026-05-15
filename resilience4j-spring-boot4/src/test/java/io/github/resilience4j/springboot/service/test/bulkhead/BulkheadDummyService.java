package io.github.resilience4j.springboot.service.test.bulkhead;

import java.util.concurrent.CompletableFuture;

public interface BulkheadDummyService {

    public String BACKEND = "backendA";
    public String BACKEND_C = "backendC";
    public String BACKEND_D = "backendD";
    public String BACKEND_E = "backendE";

    public void doSomething();

    CompletableFuture<String> doSomethingAsync() throws InterruptedException;

    CompletableFuture<Object> doSomethingAsyncWithThreadLocal() throws InterruptedException;
}
