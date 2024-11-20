package io.github.resilience4j.metrics;

import com.codahale.metrics.MetricRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.test.AsyncHelloWorldService;
import io.github.resilience4j.test.HelloWorldException;
import io.github.resilience4j.test.HelloWorldService;
import io.vavr.control.Try;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
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
    public void shouldReturnTotalNumberOfRequestsAs1ForSuccess() {
        HelloWorldService helloWorldService = mock(HelloWorldService.class);

        Retry retry = Retry.of("metrics", RetryConfig.<String>custom()
            .retryOnResult(String::isEmpty)
            .maxAttempts(5)
            .build());

        given(helloWorldService.returnHelloWorld()).willReturn("Success");
        String result = Retry.decorateSupplier(retry, helloWorldService::returnHelloWorld).get();
        assertThat(retry.getMetrics().getNumberOfTotalCalls()).isEqualTo(1);
        assertThat(result).isEqualTo("Success");
    }

    @Test
    public void shouldReturnTotalNumberOfRequestsAs1ForSuccessVoid() {
        HelloWorldService helloWorldService = mock(HelloWorldService.class);

        Retry retry = Retry.of("metrics", RetryConfig.custom()
                .maxAttempts(5)
                .build());


        retry.executeRunnable(helloWorldService::sayHelloWorld);

        assertThat(retry.getMetrics().getNumberOfTotalCalls()).isEqualTo(1);
    }

    // returns fail twice and then success
    @Test
    public void shouldReturnTotalNumberOfRequestsAs3ForFail() {
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
    public void shouldReturnTotalNumberOfRequestsAs5OnlyFails() {

        HelloWorldService helloWorldService = mock(HelloWorldService.class);

        Retry retry = Retry.of("metrics", RetryConfig.<String>custom()
            .retryExceptions(Exception.class)
            .maxAttempts(5)
            .failAfterMaxAttempts(true)
            .build());

        given(helloWorldService.returnHelloWorld())
            .willThrow(new HelloWorldException());

        Try<String> supplier = Try.ofSupplier(Retry.decorateSupplier(retry, helloWorldService::returnHelloWorld));

        assertThat(retry.getMetrics().getNumberOfTotalCalls()).isEqualTo(5);
        assertThat(supplier.isFailure()).isTrue();
    }

    // throws only checked exception finally
    @Test
    public void shouldReturnTotalNumberOfRequestsAs5OnlyFailsChecked() throws IOException {

        HelloWorldService helloWorldService = mock(HelloWorldService.class);

        Retry retry = Retry.of("metrics", RetryConfig.<String>custom()
            .retryExceptions(Exception.class)
            .maxAttempts(5)
            .failAfterMaxAttempts(true)
            .build());

        willThrow(new HelloWorldException()).given(helloWorldService).returnHelloWorldWithException();

        Callable<String> retryableCallable = Retry.decorateCallable(retry, helloWorldService::returnHelloWorldWithException);

        Try<Void> run = Try.run(retryableCallable::call);

        assertThat(retry.getMetrics().getNumberOfTotalCalls()).isEqualTo(5);
        assertThat(run.isFailure()).isTrue();
    }


    // returns async success
    @Test
    public void shouldReturnTotalNumberOfRequestsAs1ForSuccessAsync() {
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

        assertThat(retry.getMetrics().getNumberOfTotalCalls()).isEqualTo(1);
        assertThat(result).isEqualTo("Success");
    }

    // returns 1 failed and then 1 success async
    @Test
    public void shouldReturnTotalNumberOfRequestsAs3ForFailAsync() {
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
    public void shouldReturnTotalNumberOfRequestsAs5ForFailAsync() {
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
