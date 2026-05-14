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
package io.github.resilience4j.timelimiter.internal;

import io.github.resilience4j.core.ExecutorServiceFactory;
import io.github.resilience4j.core.ThreadModeExtension;
import io.github.resilience4j.core.ThreadType;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

class TimeLimiterTest {

    private static final String TIME_LIMITER_NAME = "TestTimeLimiter";
    private static final Duration TIMEOUT = Duration.ofMillis(1000);

    private ScheduledExecutorService scheduler;

    @AfterEach
    void tearDown() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    @Test
    void shouldReturnCorrectTimeoutDuration() {
        Duration timeoutDuration = Duration.ofSeconds(1);
        TimeLimiter timeLimiter = TimeLimiter.of(timeoutDuration);
        assertThat(timeLimiter).isNotNull();
        assertThat(timeLimiter.getTimeLimiterConfig().getTimeoutDuration())
            .isEqualTo(timeoutDuration);
    }

    @Test
    void shouldThrowTimeoutExceptionAndInvokeCancel() throws Exception {
        Duration timeoutDuration = Duration.ofSeconds(1);
        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
            .timeoutDuration(timeoutDuration)
            .build();
        TimeLimiter timeLimiter = TimeLimiter.of(TIME_LIMITER_NAME, timeLimiterConfig);

        @SuppressWarnings("unchecked")
        Future<Integer> mockFuture = (Future<Integer>) mock(Future.class);

        Supplier<Future<Integer>> supplier = () -> mockFuture;
        given(mockFuture.get(timeoutDuration.toMillis(), TimeUnit.MILLISECONDS))
            .willThrow(new TimeoutException());

        Callable<Integer> decorated = TimeLimiter.decorateFutureSupplier(timeLimiter, supplier);

        assertThatThrownBy(decorated::call)
            .isInstanceOf(TimeoutException.class)
            .hasMessage(TimeLimiter.createdTimeoutExceptionWithName(TIME_LIMITER_NAME, null).getMessage());

        then(mockFuture).should().cancel(true);
    }

    @Test
    void shouldThrowTimeoutExceptionWithCompletionStage() throws Exception {
        Duration timeoutDuration = Duration.ofMillis(300);
        TimeLimiter timeLimiter = TimeLimiter.of(timeoutDuration);
        scheduler = Executors.newScheduledThreadPool(1);

        Supplier<CompletionStage<Integer>> supplier = () -> CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // nothing
            }
            return 0;
        });

        CompletionStage<Integer> decorated = TimeLimiter
            .decorateCompletionStage(timeLimiter, scheduler, supplier).get();

        assertThatThrownBy(() -> decorated.toCompletableFuture().get())
            .isInstanceOf(ExecutionException.class)
            .hasCauseExactlyInstanceOf(TimeoutException.class);
    }

    @Test
    void shouldThrowTimeoutExceptionAndNotInvokeCancel() throws Exception {
        Duration timeoutDuration = Duration.ofSeconds(1);
        TimeLimiter timeLimiter = TimeLimiter
            .of(TimeLimiterConfig.custom().timeoutDuration(timeoutDuration)
                .cancelRunningFuture(false).build());

        @SuppressWarnings("unchecked")
        Future<Integer> mockFuture = (Future<Integer>) mock(Future.class);

        Supplier<Future<Integer>> supplier = () -> mockFuture;
        given(mockFuture.get(timeoutDuration.toMillis(), TimeUnit.MILLISECONDS))
            .willThrow(new TimeoutException());

        Callable<Integer> decorated = TimeLimiter.decorateFutureSupplier(timeLimiter, supplier);

        assertThatThrownBy(decorated::call)
            .isInstanceOf(TimeoutException.class);

        then(mockFuture).should(never()).cancel(true);
    }

    @Test
    void shouldReturnResult() throws Exception {
        Duration timeoutDuration = Duration.ofSeconds(1);
        TimeLimiter timeLimiter = TimeLimiter.of(timeoutDuration);

        @SuppressWarnings("unchecked")
        Future<Integer> mockFuture = (Future<Integer>) mock(Future.class);

        Supplier<Future<Integer>> supplier = () -> mockFuture;
        given(mockFuture.get(timeoutDuration.toMillis(), TimeUnit.MILLISECONDS)).willReturn(42);

        int result = timeLimiter.executeFutureSupplier(supplier);
        assertThat(result).isEqualTo(42);

        int result2 = timeLimiter.decorateFutureSupplier(supplier).call();
        assertThat(result2).isEqualTo(42);
    }

    @Test
    void shouldReturnResultWithCompletionStage() throws Exception {
        Duration timeoutDuration = Duration.ofSeconds(1);
        TimeLimiter timeLimiter = TimeLimiter.of(timeoutDuration);
        scheduler = Executors.newScheduledThreadPool(1);

        Supplier<CompletionStage<Integer>> supplier = () -> CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // nothing
            }
            return 42;
        });

        int result = timeLimiter.executeCompletionStage(scheduler, supplier).toCompletableFuture()
            .get();
        assertThat(result).isEqualTo(42);

        int result2 = timeLimiter.decorateCompletionStage(scheduler, supplier).get()
            .toCompletableFuture().get();
        assertThat(result2).isEqualTo(42);
    }

    @Test
    void unwrapExecutionException() {
        TimeLimiter timeLimiter = TimeLimiter.ofDefaults();
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Supplier<Future<Integer>> supplier = () -> executorService.submit(() -> {
            throw new RuntimeException();
        });
        Callable<Integer> decorated = TimeLimiter.decorateFutureSupplier(timeLimiter, supplier);

        assertThatThrownBy(decorated::call)
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldSetGivenName() {
        TimeLimiter timeLimiter = TimeLimiter.ofDefaults("TEST");
        assertThat(timeLimiter.getName()).isEqualTo("TEST");
    }

    @TestTemplate
    @ExtendWith(ThreadModeExtension.class)
    void shouldUseCorrectThreadTypeForScheduler(ThreadType threadType) throws Exception {
        boolean isVirtual = threadType == ThreadType.VIRTUAL;
        scheduler = ExecutorServiceFactory.newSingleThreadScheduledExecutor("timelimiter-test-" + threadType);

        TimeLimiter timeLimiter = TimeLimiter.of(TimeLimiterConfig.custom()
            .timeoutDuration(TIMEOUT)
            .build());

        AtomicBoolean ranOnExpectedThreadType = new AtomicBoolean(false);
        ExecutorService taskExecutor = isVirtual ?
            Executors.newVirtualThreadPerTaskExecutor() :
            Executors.newSingleThreadExecutor();

        try {
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(50);
                    boolean expectedThreadType = isVirtual ?
                        Thread.currentThread().isVirtual() :
                        !Thread.currentThread().isVirtual();
                    ranOnExpectedThreadType.set(expectedThreadType);
                    return true;
                } catch (InterruptedException e) {
                    return false;
                }
            }, taskExecutor);

            Supplier<CompletionStage<Boolean>> decoratedSupplier = timeLimiter.decorateCompletionStage(
                scheduler, () -> future);

            Boolean result = decoratedSupplier.get().toCompletableFuture().get(2, TimeUnit.SECONDS);
            assertThat(result).isTrue();

            CompletableFuture<Boolean> threadTypeFuture = new CompletableFuture<>();
            scheduler.execute(() -> {
                boolean isExpectedType = isVirtual ?
                    Thread.currentThread().isVirtual() :
                    !Thread.currentThread().isVirtual();
                threadTypeFuture.complete(isExpectedType);
            });

            Boolean usedExpectedThreadType = threadTypeFuture.get(1, TimeUnit.SECONDS);
            assertThat(usedExpectedThreadType)
                .as("TimeLimiter's scheduler should use " + threadType + " threads")
                .isTrue();
            assertThat(ranOnExpectedThreadType.get())
                .as("CompletableFuture execution should run on " + threadType + " thread")
                .isTrue();
        } finally {
            taskExecutor.shutdownNow();
        }
    }

    @TestTemplate
    @ExtendWith(ThreadModeExtension.class)
    void shouldTimeoutAndCancelOnCorrectThreadType(ThreadType threadType) throws Exception {
        boolean isVirtual = threadType == ThreadType.VIRTUAL;
        CountDownLatch interruptedLatch = new CountDownLatch(1);

        ExecutorService executor = isVirtual ?
            Executors.newVirtualThreadPerTaskExecutor() :
            Executors.newSingleThreadExecutor();

        try {
            TimeLimiter timeLimiter = TimeLimiter.of(TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(50))
                .cancelRunningFuture(true)
                .build());

            Callable<String> longRunningTask = () -> {
                try {
                    Thread.sleep(10000);
                    return "Task completed";
                } catch (InterruptedException e) {
                    interruptedLatch.countDown();
                    throw e;
                }
            };

            Future<String> future = executor.submit(longRunningTask);

            assertThatThrownBy(() -> timeLimiter.decorateFutureSupplier(() -> future).call())
                .isInstanceOf(TimeoutException.class);

            boolean wasInterrupted = interruptedLatch.await(500, TimeUnit.MILLISECONDS);
            assertThat(wasInterrupted)
                .as("Task should have been interrupted due to cancellation")
                .isTrue();
            assertThat(future.isCancelled())
                .as("Future should have been cancelled")
                .isTrue();
        } finally {
            executor.shutdownNow();
        }
    }
}
