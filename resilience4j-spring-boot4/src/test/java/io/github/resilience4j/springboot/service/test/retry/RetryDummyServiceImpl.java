package io.github.resilience4j.springboot.service.test.retry;


import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;


@Component
public class RetryDummyServiceImpl implements RetryDummyService {

    @Retry(name = RetryDummyService.RETRY_BACKEND_A)
    @Override
    public void doSomething(boolean throwBackendTrouble) throws IOException {
        if (throwBackendTrouble) {
            throw new IOException("Test Message");
        }
    }

    @Retry(name = RetryDummyService.RETRY_BACKEND_B)
    @Override
    public CompletionStage<String> doSomethingAsync(boolean throwException) throws IOException {
        if (throwException) {
            CompletableFuture<String> promise = new CompletableFuture<>();
            promise.completeExceptionally(new IOException("Test Message"));
            return promise;
        } else {
            return CompletableFuture.supplyAsync(() -> "test");
        }

    }
}
