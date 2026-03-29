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
package io.github.resilience4j.metrics;

import com.codahale.metrics.MetricRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.test.AsyncHelloWorldService;
import io.github.resilience4j.test.HelloWorldException;
import io.github.resilience4j.test.HelloWorldService;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

public class RetryMetricsTest extends AbstractRetryMetricsTest {

    @Override
    protected Retry givenMetricRegistry(String prefix, MetricRegistry metricRegistry) {
        RetryRegistry retryRegistry = RetryRegistry
            .of(RetryConfig.custom().waitDuration(Duration.ofMillis(150)).build());
        Retry retry = retryRegistry.retry("testName");
        metricRegistry.registerAll(RetryMetrics.ofRetryRegistry(prefix, retryRegistry));

        return retry;
    }

    @Override
    protected Retry givenMetricRegistry(MetricRegistry metricRegistry) {
        RetryRegistry retryRegistry = RetryRegistry
            .of(RetryConfig.custom().waitDuration(Duration.ofMillis(150)).build());
        Retry retry = retryRegistry.retry("testName");
        metricRegistry.registerAll(RetryMetrics.ofRetryRegistry(retryRegistry));

        return retry;
    }

    // returns only success
    @Test
    void shouldReturnTotalNumberOfRequestsAs1ForSuccess() {
        HelloWorldService helloWorldService = mock(HelloWorldService.class);

        Retry retry = Retry.of("metrics", RetryConfig.<String>custom()
            .retryOnResult(String::isEmpty)
            .maxAttempts(5)
            .build());

        given(helloWorldService.returnHelloWorld()).willReturn("Success");
        String result = Retry.decorateSupplier(retry, helloWorldService::returnHelloWorld).get();
        assertThat(retry.getMetrics().getNumberOfTotalCalls()).isOne();
        assertThat(result).isEqualTo("Success");
    }

    @Test
    void shouldReturnTotalNumberOfRequestsAs1ForSuccessVoid() {
        HelloWorldService helloWorldService = mock(HelloWorldService.class);

        Retry retry = Retry.of("metrics", RetryConfig.custom()
                .maxAttempts(5)
                .build());


        retry.executeRunnable(helloWorldService::sayHelloWorld);

        assertThat(retry.getMetrics().getNumberOfTotalCalls()).isOne();
    }

    // returns fail twice and then success
    @Test
    void shouldReturnTotalNumberOfRequestsAs3ForFail() {
        HelloWorldService helloWorldService = mock(HelloWorldService.class);

        Retry retry = Retry.of("metrics", RetryConfig.<String>custom()
            .retryExceptions(Exception.class)
            .maxAttempts(5)
            .build());

        given(helloWorldService.returnHelloWorld())
            .willThrow(new HelloWorldException())
            .willThrow(new HelloWorldException())
            .willReturn("Success");

        String result = Retry.decorateSupplier(retry, helloWorldService::returnHelloWorld).get();

        assertThat(retry.getMetrics().getNumberOfTotalCalls()).isEqualTo(3);
        assertThat(result).isEqualTo("Success");
    }

    // throws only exception finally
    @Test
    void shouldReturnTotalNumberOfRequestsAs5OnlyFails() {

        HelloWorldService helloWorldService = mock(HelloWorldService.class);

        Retry retry = Retry.of("metrics", RetryConfig.<String>custom()
            .retryExceptions(Exception.class)
            .maxAttempts(5)
            .failAfterMaxAttempts(true)
            .build());

        given(helloWorldService.returnHelloWorld())
            .willThrow(new HelloWorldException());

        assertThatThrownBy(() -> Retry.decorateSupplier(retry, helloWorldService::returnHelloWorld).get())
            .isInstanceOf(HelloWorldException.class);

        assertThat(retry.getMetrics().getNumberOfTotalCalls()).isEqualTo(5);
    }

    // throws only checked exception finally
    @Test
    void shouldReturnTotalNumberOfRequestsAs5OnlyFailsChecked() throws Exception {

        HelloWorldService helloWorldService = mock(HelloWorldService.class);

        Retry retry = Retry.of("metrics", RetryConfig.<String>custom()
            .retryExceptions(Exception.class)
            .maxAttempts(5)
            .failAfterMaxAttempts(true)
            .build());

        willThrow(new HelloWorldException()).given(helloWorldService).returnHelloWorldWithException();

        Callable<String> retryableCallable = Retry.decorateCallable(retry, helloWorldService::returnHelloWorldWithException);

        assertThatThrownBy(retryableCallable::call)
            .isInstanceOf(HelloWorldException.class);

        assertThat(retry.getMetrics().getNumberOfTotalCalls()).isEqualTo(5);
    }


    // returns async success
    @Test
    void shouldReturnTotalNumberOfRequestsAs1ForSuccessAsync() {
        AsyncHelloWorldService helloWorldService = mock(AsyncHelloWorldService.class);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        given(helloWorldService.returnHelloWorld())
            .willReturn(completedFuture("Success"));

        Retry retry = Retry.of("metrics", RetryConfig.<String>custom()
            .retryExceptions(Exception.class)
            .maxAttempts(5)
            .build());

        Supplier<CompletionStage<String>> supplier = Retry.decorateCompletionStage(retry, scheduler, helloWorldService::returnHelloWorld);

        String result = awaitResult(supplier.get(), 5);

        assertThat(retry.getMetrics().getNumberOfTotalCalls()).isOne();
        assertThat(result).isEqualTo("Success");
    }

    // returns 1 failed and then 1 success async
    @Test
    void shouldReturnTotalNumberOfRequestsAs3ForFailAsync() {
        AsyncHelloWorldService helloWorldService = mock(AsyncHelloWorldService.class);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        CompletableFuture<String> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new HelloWorldException());
        given(helloWorldService.returnHelloWorld())
            .willReturn(failedFuture)
            .willReturn(completedFuture("Success"));

        Retry retry = Retry.of("metrics", RetryConfig.<String>custom()
            .retryExceptions(Exception.class)
            .maxAttempts(5)
            .failAfterMaxAttempts(true)
            .build());

        Supplier<CompletionStage<String>> supplier = Retry.decorateCompletionStage(retry, scheduler, helloWorldService::returnHelloWorld);

        String result = awaitResult(supplier.get(), 5);

        assertThat(retry.getMetrics().getNumberOfTotalCalls()).isEqualTo(2);
        assertThat(result).isEqualTo("Success");
    }

    // throws only exception async
    @Test
    void shouldReturnTotalNumberOfRequestsAs5ForFailAsync() {
        AsyncHelloWorldService helloWorldService = mock(AsyncHelloWorldService.class);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        CompletableFuture<String> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new HelloWorldException());
        given(helloWorldService.returnHelloWorld()).willReturn(failedFuture);

        Retry retry = Retry.of("metrics", RetryConfig.<String>custom()
            .retryExceptions(Exception.class)
            .maxAttempts(5)
            .failAfterMaxAttempts(true)
            .build());

        Supplier<CompletionStage<String>> supplier = Retry.decorateCompletionStage(retry, scheduler, helloWorldService::returnHelloWorld);

        assertThat(supplier.get())
            .failsWithin(5, TimeUnit.SECONDS)
            .withThrowableOfType(ExecutionException.class)
            .havingCause();

        assertThat(retry.getMetrics().getNumberOfTotalCalls()).isEqualTo(5);
    }

    public static <T> T awaitResult(CompletionStage<T> completionStage, long timeoutSeconds) {
        try {
            return completionStage.toCompletableFuture().get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException e) {
            throw new AssertionError(e);
        } catch (ExecutionException e) {
            throw new RuntimeExecutionException(e.getCause());
        }
    }

    private static class RuntimeExecutionException extends RuntimeException {

        RuntimeExecutionException(Throwable cause) {
            super(cause);
        }
    }
}
