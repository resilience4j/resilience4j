package io.github.resilience4j.core;

import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import static io.github.resilience4j.core.CompletionStageUtils.recover;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CompletionStageUtilsTest {

    @Test
    public void shouldReturnResult() throws Exception {
        CompletableFuture<String> future = CompletableFuture.completedFuture("result");

        String result = recover(future, (e) -> "fallback").toCompletableFuture()
            .get(1, TimeUnit.SECONDS);

        assertThat(result).isEqualTo("result");
    }

    @Test
    public void shouldRecoverTimeoutException() throws Exception {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new TimeoutException());

        String result = recover(future, (e) -> "fallback").toCompletableFuture()
            .get(1, TimeUnit.SECONDS);

        assertThat(result).isEqualTo("fallback");
    }

    @Test
    public void shouldReturnExceptionFromRecoveryMethod() {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("bla"));

        RuntimeException exception = new RuntimeException("blub");

        Function<Throwable, String> fallback = (e) -> {
            throw exception;
        };

        assertThatThrownBy(() -> recover(future, fallback).toCompletableFuture()
            .get(1, TimeUnit.SECONDS)).hasCause(exception);
    }

    @Test
    public void shouldReturnResult2() throws Exception {
        CompletableFuture<String> future = CompletableFuture.completedFuture("result");

        String result = recover(future, TimeoutException.class, (e) -> "fallback").toCompletableFuture()
            .get(1, TimeUnit.SECONDS);

        assertThat(result).isEqualTo("result");
    }

    @Test
    public void shouldThrowRuntimeException() {
        RuntimeException exception = new RuntimeException("blub");

        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(exception);

        assertThatThrownBy(() -> recover(future, TimeoutException.class, (e) -> "fallback").toCompletableFuture()
            .get(1, TimeUnit.SECONDS))
            .hasCause(exception);
    }

    @Test
    public void shouldRecoverTimeoutException2() throws Exception {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new TimeoutException());

        String result = recover(future, TimeoutException.class, (e) -> "fallback").toCompletableFuture()
            .get(1, TimeUnit.SECONDS);

        assertThat(result).isEqualTo("fallback");
    }

    @Test
    public void shouldRecoverFromSpecificExceptions()
        throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new TimeoutException());

        String result = recover(future, asList(TimeoutException.class, IOException.class), (e) -> "fallback").toCompletableFuture()
            .get(1, TimeUnit.SECONDS);

        assertThat(result).isEqualTo("fallback");
    }

    @Test
    public void shouldRecoverSupplierFromSpecificResult()
        throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<String> future = CompletableFuture.completedFuture("Wrong Result");

        String result = recover(future, (r) -> r.equals("Wrong Result"), (r) -> "Bla").toCompletableFuture()
            .get(1, TimeUnit.SECONDS);

        assertThat(result).isEqualTo("Bla");
    }

}
