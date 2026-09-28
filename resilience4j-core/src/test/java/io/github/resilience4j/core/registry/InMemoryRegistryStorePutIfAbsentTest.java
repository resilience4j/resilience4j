/*
 * Copyright 2026 Wali Temuri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.core.registry;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

class InMemoryRegistryStorePutIfAbsentTest {

    @ParameterizedTest
    @CsvSource({
        "false, SUCCESS, 1", "false, SUCCESS, 2",
        "false, FAILURE, 1", "false, FAILURE, 2",
        "false, CANCELLATION, 1", "false, CANCELLATION, 2",
        "true, SUCCESS, 1", "true, SUCCESS, 2",
        "true, FAILURE, 1", "true, FAILURE, 2",
        "true, CANCELLATION, 1", "true, CANCELLATION, 2"
    })
    void shouldPreserveRegistrationsAfterConcurrentComputation(
        boolean virtualThreads, Outcome outcome, int contenders) throws Exception {
        InMemoryRegistryStore<String> store = new InMemoryRegistryStore<>();
        ThreadFactory threads = virtualThreads ? Thread.ofVirtual().factory()
            : Thread.ofPlatform().factory();
        CountDownLatch computationStarted = new CountDownLatch(1);
        CompletableFuture<String> computedValue = new CompletableFuture<>();
        CompletableFuture<String> computationResult = new CompletableFuture<>();
        List<Thread> workers = new ArrayList<>();
        List<CompletableFuture<String>> putResults = new ArrayList<>();
        boolean computationFails = outcome != Outcome.SUCCESS;
        RuntimeException failure = outcome == Outcome.CANCELLATION
            ? new CancellationException("initialization cancelled")
            : new IllegalStateException("initialization failed");
        Thread computation = threads.newThread(() -> {
            try {
                computationResult.complete(store.computeIfAbsent("key", key -> {
                    computationStarted.countDown();
                    return computedValue.join();
                }));
            } catch (Throwable throwable) {
                computationResult.completeExceptionally(throwable);
            }
        });
        workers.add(computation);

        try {
            computation.start();
            assertThat(computationStarted.await(5, TimeUnit.SECONDS)).isTrue();
            for (int i = 0; i < contenders; i++) {
                String candidate = "candidate-" + i;
                CompletableFuture<String> putResult = new CompletableFuture<>();
                putResults.add(putResult);
                Thread registration = threads.newThread(() -> {
                    try {
                        putResult.complete(store.putIfAbsent("key", candidate));
                    } catch (Throwable throwable) {
                        putResult.completeExceptionally(throwable);
                    }
                });
                workers.add(registration);
                registration.start();
                // The registration thread can only park while joining the pending computation.
                await().atMost(Duration.ofSeconds(5))
                    .until(() -> registration.getState() == Thread.State.WAITING);
            }

            if (computationFails) {
                computedValue.completeExceptionally(failure);
            } else {
                computedValue.complete("computed");
            }

            if (outcome == Outcome.CANCELLATION) {
                assertThatThrownBy(() -> computationResult.get(5, TimeUnit.SECONDS))
                    .isSameAs(failure);
            } else if (computationFails) {
                assertThatThrownBy(() -> computationResult.get(5, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasRootCause(failure);
            } else {
                assertThat(computationResult.get(5, TimeUnit.SECONDS)).isEqualTo("computed");
            }
            List<String> previousValues = new ArrayList<>();
            for (CompletableFuture<String> putResult : putResults) {
                previousValues.add(putResult.get(5, TimeUnit.SECONDS));
            }
            if (computationFails) {
                assertThat(previousValues.stream().filter(value -> value == null).count())
                    .isEqualTo(1);
                int winner = previousValues.indexOf(null);
                String registeredValue = "candidate-" + winner;
                assertThat(store.find("key")).hasValue(registeredValue);
                assertThat(store.values()).containsExactly(registeredValue);
                for (int i = 0; i < contenders; i++) {
                    if (i != winner) {
                        assertThat(previousValues.get(i)).isEqualTo(registeredValue);
                    }
                }
            } else {
                assertThat(previousValues).containsOnly("computed");
                assertThat(store.find("key")).hasValue("computed");
                assertThat(store.values()).containsExactly("computed");
            }
        } finally {
            computedValue.completeExceptionally(failure);
            for (Thread worker : workers) {
                worker.join(TimeUnit.SECONDS.toMillis(5));
                assertThat(worker.isAlive()).isFalse();
            }
        }
    }

    private enum Outcome {
        SUCCESS, FAILURE, CANCELLATION
    }
}
