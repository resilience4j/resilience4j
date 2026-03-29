/*
 *
 * Copyright 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package io.github.resilience4j.core;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import static io.github.resilience4j.core.CompletionStageUtils.recover;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompletionStageUtilsTest {

    @Test
    void shouldReturnResult() throws Exception {
        CompletableFuture<String> future = CompletableFuture.completedFuture("result");

        String result = recover(future, (e) -> "fallback").toCompletableFuture()
            .get(1, TimeUnit.SECONDS);

        assertThat(result).isEqualTo("result");
    }

    @Test
    void shouldRecoverTimeoutException() throws Exception {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new TimeoutException());

        String result = recover(future, (e) -> "fallback").toCompletableFuture()
            .get(1, TimeUnit.SECONDS);

        assertThat(result).isEqualTo("fallback");
    }

    @Test
    void shouldReturnExceptionFromRecoveryMethod() {
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
    void shouldReturnResult2() throws Exception {
        CompletableFuture<String> future = CompletableFuture.completedFuture("result");

        String result = recover(future, TimeoutException.class, (e) -> "fallback").toCompletableFuture()
            .get(1, TimeUnit.SECONDS);

        assertThat(result).isEqualTo("result");
    }

    @Test
    void shouldThrowRuntimeException() {
        RuntimeException exception = new RuntimeException("blub");

        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(exception);

        assertThatThrownBy(() -> recover(future, TimeoutException.class, (e) -> "fallback").toCompletableFuture()
            .get(1, TimeUnit.SECONDS))
            .hasCause(exception);
    }

    @Test
    void shouldRecoverTimeoutException2() throws Exception {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new TimeoutException());

        String result = recover(future, TimeoutException.class, (e) -> "fallback").toCompletableFuture()
            .get(1, TimeUnit.SECONDS);

        assertThat(result).isEqualTo("fallback");
    }

    @Test
    void shouldRecoverFromSpecificExceptions()
        throws Exception {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new TimeoutException());

        String result = recover(future, asList(TimeoutException.class, IOException.class), (e) -> "fallback").toCompletableFuture()
            .get(1, TimeUnit.SECONDS);

        assertThat(result).isEqualTo("fallback");
    }

    @Test
    void shouldRecoverSupplierFromSpecificResult()
        throws Exception {
        CompletableFuture<String> future = CompletableFuture.completedFuture("Wrong Result");

        String result = recover(future, (r) -> r.equals("Wrong Result"), (r) -> "Bla").toCompletableFuture()
            .get(1, TimeUnit.SECONDS);

        assertThat(result).isEqualTo("Bla");
    }

}
