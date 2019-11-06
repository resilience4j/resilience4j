package io.github.resilience4j.service.test;


import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public interface DummyService {

    String BACKEND = "backendA";

    void doSomething(boolean throwException) throws IOException;

    CompletableFuture<String> doSomethingAsync(boolean throwException) throws IOException;
}
