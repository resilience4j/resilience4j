package io.github.resilience4j.core;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CompletionStageUtils {

    @Test
    public void shouldReturnResult() throws Exception {
        CompletableFuture<String> future = CompletableFuture.completedFuture("result");

        String result = recover(future, (e) -> "fallback").toCompletableFuture()
            .get(1, TimeUnit.SECONDS);

        assertThat(result).isEqualTo("result");
    }

    @Test
    public void shouldRecoverException() throws Exception {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException());

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


    public static <T> CompletionStage<T> recover(CompletionStage<T> completionStage, Function<Throwable, T> exceptionHandler){
        CompletableFuture<T> promise = new CompletableFuture<>();
        completionStage.whenComplete((result, throwable) -> {
            if (throwable != null) {
                try {
                    promise.complete(exceptionHandler.apply(throwable));
                } catch (Throwable fallbackThrowable) {
                    promise.completeExceptionally(fallbackThrowable);
                }
            } else {
                promise.complete(result);
            }
        });
        return promise;
    }

}
