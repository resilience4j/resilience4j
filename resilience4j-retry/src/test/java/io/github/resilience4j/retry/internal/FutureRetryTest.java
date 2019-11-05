package io.github.resilience4j.retry.internal;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.test.HelloWorldException;
import io.github.resilience4j.test.HelloWorldService;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.*;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class FutureRetryTest {

    private HelloWorldService helloWorldService;
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private CompletableFuture<String> completed = completedFuture("Hello world");
    private CompletableFuture<String> failed = new CompletableFuture<>();
    {
        failed.completeExceptionally(new HelloWorldException());
    }

    @Before
    public void setUp() {
        helloWorldService = mock(HelloWorldService.class);
    }

    @Test
    public void shouldNotRetry() throws InterruptedException, ExecutionException, TimeoutException {
        given(helloWorldService.returnFuture()).willReturn(completed);
        Retry retryContext = Retry.ofDefaults("id");
        Retry.Metrics metrics = retryContext.getMetrics();

        String result = resultFromDecoratedSupplier(retryContext);

        then(helloWorldService).should().returnFuture();
        assertThat(result).isEqualTo("Hello world");
        assertThat(metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void shouldNotRetryWithThatResult() throws InterruptedException, ExecutionException, TimeoutException {
        given(helloWorldService.returnFuture()).willReturn(completed);
        Retry retryContext = Retry.of(
                "id",
                RetryConfig.<String>custom()
                        .retryOnResult(s -> s.contains("NoRetry"))
                        .maxAttempts(1)
                        .build());
        Retry.Metrics metrics = retryContext.getMetrics();

        String result = resultFromDecoratedSupplier(retryContext);

        then(helloWorldService).should().returnFuture();
        assertThat(result).isEqualTo("Hello world");
        assertThat(metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void shouldRetryInCaseOfResultRetryMatchAtSyncStage()
            throws InterruptedException, ExecutionException, TimeoutException {
        shouldTryRepeatedlyInCaseOfRetryOnResult(1);
    }

    @Test
    public void shouldRetryTwoAttemptsInCaseOfResultRetryMatchAtSyncStage()
            throws InterruptedException, ExecutionException, TimeoutException {
        shouldTryRepeatedlyInCaseOfRetryOnResult(2);
    }

    @Test
    public void shouldRetryThreeAttemptsInCaseOfResultRetryMatchAtSyncStage()
            throws InterruptedException, ExecutionException, TimeoutException {
        shouldTryRepeatedlyInCaseOfRetryOnResult(3);
    }

    private void shouldTryRepeatedlyInCaseOfRetryOnResult(int numAttempts)
            throws InterruptedException, ExecutionException, TimeoutException {
        given(helloWorldService.returnFuture()).willReturn(completed);
        Retry retryContext = Retry.of(
                "id",
                RetryConfig.<String>custom()
                        .maxAttempts(numAttempts)
                        .retryOnResult(s -> s.contains("Hello world"))
                        .build());
        Retry.Metrics metrics = retryContext.getMetrics();

        String result = resultFromDecoratedSupplier(retryContext);

        then(helloWorldService).should(times(numAttempts)).returnFuture();
        assertThat(result).isEqualTo("Hello world");
        assertThat(metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(1);
    }

    @Test
    public void shouldRethrowExceptionInCaseOfExceptionAtSyncStage()
            throws InterruptedException, ExecutionException, TimeoutException {
        given(helloWorldService.returnFuture())
                .willThrow(new IllegalArgumentException("BAM!"));
        Retry retryContext = Retry.ofDefaults("id");
        Retry.Metrics metrics = retryContext.getMetrics();

        try {
            resultFromDecoratedSupplier(retryContext);
            fail("should throw ExecutionException");
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(IllegalArgumentException.class);
        }

        assertThat(metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void shouldRetryInCaseOfExceptionAtAsyncStage()
            throws InterruptedException, ExecutionException, TimeoutException {
        given(helloWorldService.returnFuture())
                .willReturn(failed)
                .willReturn(completed);
        Retry retryContext = Retry.ofDefaults("id");
        Retry.Metrics metrics = retryContext.getMetrics();

        String result = resultFromDecoratedSupplier(retryContext);

        then(helloWorldService).should(times(2)).returnFuture();
        assertThat(result).isEqualTo("Hello world");
        assertThat(metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void shouldRetryInCaseOfTwoExceptionsAtAsyncStage()
            throws InterruptedException, ExecutionException, TimeoutException {
        given(helloWorldService.returnFuture())
                .willReturn(failed)
                .willReturn(failed)
                .willReturn(completed);
        Retry retryContext = Retry.ofDefaults("id");
        Retry.Metrics metrics = retryContext.getMetrics();

        String result = resultFromDecoratedSupplier(retryContext);

        then(helloWorldService).should(times(3)).returnFuture();
        assertThat(result).isEqualTo("Hello world");
        assertThat(metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void shouldThrowAfterOneAttemptPerRetryConfiguration() {
        shouldThrowAfterAttemptsPerRetryConfiguration(1);
    }

    @Test
    public void shouldThrowAfterTwoAttemptsPerRetryConfiguration() {
        shouldThrowAfterAttemptsPerRetryConfiguration(2);
    }

    @Test
    public void shouldThrowAfterThreeAttemptsPerRetryConfiguration() {
        shouldThrowAfterAttemptsPerRetryConfiguration(3);
    }

    private void shouldThrowAfterAttemptsPerRetryConfiguration(int numAttempts) {
        given(helloWorldService.returnFuture()).willReturn(failed);
        Retry retryContext = Retry.of("id", RetryConfig.custom().maxAttempts(numAttempts).build());
        Retry.Metrics metrics = retryContext.getMetrics();

        Try<String> resultTry = Try.of(() -> resultFromDecoratedSupplier(retryContext));

        then(helloWorldService).should(times(numAttempts)).returnFuture();
        assertThat(resultTry.isFailure()).isTrue();

        Throwable immediateCause = resultTry.getCause();
        assertThat(immediateCause).isInstanceOf(ExecutionException.class);
        assertThat(immediateCause.getCause()).isInstanceOf(HelloWorldException.class);

        assertThat(metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(1);
    }

    private String resultFromDecoratedSupplier(Retry retryContext)
            throws InterruptedException, ExecutionException, TimeoutException {
        return Retry.decorateFuture(retryContext, scheduler, helloWorldService::returnFuture)
                .get()
                .get(2, TimeUnit.SECONDS);
    }
}
