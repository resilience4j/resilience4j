package io.github.resilience4j.springboot.service.test.retry;


import java.io.IOException;
import java.util.concurrent.CompletionStage;

public interface RetryDummyService {

    String RETRY_BACKEND_A = "retryBackendA";
    String RETRY_BACKEND_B = "retryBackendB";

    void doSomething(boolean throwException) throws IOException;

    CompletionStage<String> doSomethingAsync(boolean throwException) throws IOException;
}
