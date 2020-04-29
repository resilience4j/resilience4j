package io.github.resilience4j.micronaut

import io.micronaut.context.annotation.Executable

import java.util.concurrent.CompletableFuture

public abstract class TestDummyService {
    CompletableFuture<String> asyncError() {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Test"));
        return future
    }

    String syncError() {
        throw new RuntimeException("Test");
    }

    @Executable
    CompletableFuture<String> completionStageRecovery() {
        return CompletableFuture.supplyAsync({ -> 'recovered' });
    }

    @Executable
    String syncRecovery() {
        return "recovered"
    }
}
