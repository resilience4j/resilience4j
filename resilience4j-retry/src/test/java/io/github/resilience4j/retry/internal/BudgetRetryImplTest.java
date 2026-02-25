package io.github.resilience4j.retry.internal;

import io.github.resilience4j.core.metrics.Metrics;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryBudgetExceededException;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.test.HelloWorldException;
import io.github.resilience4j.test.HelloWorldService;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

public class BudgetRetryImplTest {
    private HelloWorldService helloWorldService;
    private long sleptTime = 0L;

    @Before
    public void setUp() {
        helloWorldService = mock(HelloWorldService.class);
        RetryImpl.sleepFunction = sleep -> sleptTime += sleep;
    }

    @Test
    public void shouldRetryNormallyWhenFailureRatioBelowThreshold() {
        given(helloWorldService.returnHelloWorld())
                .willThrow(new HelloWorldException())
                .willReturn("Hello world");

        RetryConfig config = RetryConfig.<String>custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(10))
                .maxRetryRatio(0.5) // 허용 실패율 50%
                .windowSize(10)
                .minSampleSize(2)
                .build();

        Retry retry = Retry.ofRetryBudget("budget-test", config);

        Supplier<String> supplier = Retry.decorateSupplier(retry, helloWorldService::returnHelloWorld);
        String result = supplier.get();

        then(helloWorldService).should(times(2)).returnHelloWorld();
        assertThat(result).isEqualTo("Hello world");
        assertThat(sleptTime).isEqualTo(10L);
        assertThat(((BudgetRetryImpl) retry).getSlidingWindowMetrics().getSnapshot().getFailureRate())
                .isLessThanOrEqualTo(34F);
    }

    @Test
    public void shouldStopRetryWhenFailureRatioExceedsBudget() {
        given(helloWorldService.returnHelloWorld())
                .willThrow(new HelloWorldException("fail"));

        RetryConfig config = RetryConfig.<String>custom()
                .maxAttempts(1000)
                .waitDuration(Duration.ofMillis(10))
                .maxRetryRatio(0.3)
                .windowSize(10)
                .minSampleSize(1)
                .build();

        Retry retry = Retry.ofRetryBudget("budget-over", config);
        Supplier<String> supplier = Retry.decorateSupplier(retry, helloWorldService::returnHelloWorld);

        for (int i = 0; i < 10; i++) {
            try {
                supplier.get();
            } catch (Exception ignored) {}
        }

        assertThatThrownBy(supplier::get)
                .isInstanceOf(RetryBudgetExceededException.class)
                .hasMessageContaining("Retry budget exceeded");
    }

    @Test
    public void shouldRecordMetricsForSuccessAndFailure() {
        given(helloWorldService.returnHelloWorld())
                .willThrow(new HelloWorldException())
                .willReturn("Hello world")
                .willThrow(new HelloWorldException())
                .willReturn("Hello again");

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(10))
                .windowSize(10)
                .maxRetryRatio(0.6)
                .minSampleSize(2)
                .build();

        Retry retry = Retry.ofRetryBudget("metrics-test", config);

        Supplier<String> supplier = Retry.decorateSupplier(retry, helloWorldService::returnHelloWorld);

        for (int i = 0; i < 4; i++) {
            try {
                supplier.get();
            } catch (Exception ignored) {
            }
        }

        var snapshot = ((BudgetRetryImpl) retry).getSlidingWindowMetrics().getSnapshot();
        assertThat(snapshot.getTotalNumberOfCalls()).isGreaterThan(0);
        assertThat(snapshot.getFailureRate()).isBetween(14F, 15F);
    }

    @Test
    public void shouldNotThrowDuringWarmupWhenBelowMinSampleSize() {
        given(helloWorldService.returnHelloWorld()).willThrow(new HelloWorldException());
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(10))
                .windowSize(10)
                .maxRetryRatio(0.0)
                .minSampleSize(5)
                .build();

        Retry retry = Retry.ofRetryBudget("warmup-test", config);
        Supplier<String> supplier = Retry.decorateSupplier(retry, helloWorldService::returnHelloWorld);

        assertThatThrownBy(supplier::get)
                .isInstanceOf(HelloWorldException.class);
    }

    @Test
    public void shouldAllowSuccessAfterFailuresWhenBudgetRecovers() {
        RetryConfig config = RetryConfig.<String>custom()
                .windowSize(10)
                .minSampleSize(1)
                .maxRetryRatio(0.5)
                .build();
        Retry retry = Retry.ofRetryBudget("recover", config);
        BudgetRetryImpl impl = (BudgetRetryImpl) retry;


        for (int i = 0; i < 5; i++) {
            impl.getSlidingWindowMetrics().record(0, TimeUnit.MILLISECONDS, Metrics.Outcome.ERROR);
        }

        for (int i = 0; i < 5; i++) {
            impl.getSlidingWindowMetrics().record(0, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        }

        var snapshot = impl.getSlidingWindowMetrics().getSnapshot();
        System.out.println("failureRate=" + snapshot.getFailureRate());

        assertThat(snapshot.getFailureRate()).isLessThanOrEqualTo(50.0f);
    }

    @Test
    public void shouldHandleAsyncRetryBudgetGracefully() {
        given(helloWorldService.returnHelloWorld())
                .willThrow(new HelloWorldException())
                .willReturn("Hello async");

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(10))
                .windowSize(10)
                .maxRetryRatio(0.5)
                .build();

        Retry retry = Retry.ofRetryBudget("async-budget", config);

        var context = retry.asyncContext();
        context.onError(new HelloWorldException());
        context.onComplete();

        var snapshot = ((BudgetRetryImpl) retry).getSlidingWindowMetrics().getSnapshot();
        assertThat(snapshot.getTotalNumberOfCalls()).isGreaterThan(0);
        assertThat(snapshot.getFailureRate()).isLessThanOrEqualTo(50.0F);
    }

    @Test
    public void shouldDecorateSupplierAndExecuteMultipleTimes() {
        given(helloWorldService.returnHelloWorld())
                .willThrow(new HelloWorldException())
                .willReturn("ok")
                .willThrow(new HelloWorldException())
                .willReturn("ok");

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(10))
                .windowSize(10)
                .maxRetryRatio(0.7)
                .minSampleSize(5)
                .build();

        Retry retry = Retry.ofRetryBudget("multi-exec", config);
        Supplier<String> supplier = Retry.decorateSupplier(retry, helloWorldService::returnHelloWorld);

        String r1 = supplier.get();
        String r2 = supplier.get();

        then(helloWorldService).should(times(4)).returnHelloWorld();
        assertThat(r1).isEqualTo("ok");
        assertThat(r2).isEqualTo("ok");
    }

}
