package io.github.resilience4j.springboot.service.test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public interface DummyService {

    public String BACKEND = "backendA";
    public String BACKEND_B = "backendB";

    public void doSomething(boolean throwException) throws IOException;

    CompletableFuture<String> longDoSomethingAsync() throws InterruptedException;

    CompletableFuture<String> doSomethingAsync(boolean throwException) throws IOException;
}
