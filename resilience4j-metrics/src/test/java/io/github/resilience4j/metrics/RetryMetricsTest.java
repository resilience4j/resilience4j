package io.github.resilience4j.metrics;

import com.codahale.metrics.MetricRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.test.HelloWorldService;
import io.vavr.CheckedFunction0;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class RetryMetricsTest {

    private MetricRegistry metricRegistry;
    private HelloWorldService helloWorldService;

    @Before
    public void setUp(){
        metricRegistry = new MetricRegistry();
        helloWorldService = mock(HelloWorldService.class);
    }

    @Test
    public void shouldRegisterMetrics() throws Throwable {
        //Given
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        Retry retry = retryRegistry.retry("testName");
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        metricRegistry.registerAll(RetryMetrics.ofRetryRegistry(retryRegistry));

        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        // Setup circuitbreaker with retry
        CheckedFunction0<String> decoratedSupplier = CircuitBreaker
                .decorateCheckedSupplier(circuitBreaker, helloWorldService::returnHelloWorld);
        decoratedSupplier = Retry
                .decorateCheckedSupplier(retry, decoratedSupplier);


        //When
        String value = Try.of(decoratedSupplier)
                .recover(throwable -> "Hello from Recovery").get();

        //Then
        assertThat(value).isEqualTo("Hello world");
        // Then the helloWorldService should be invoked 1 context
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorld();
        assertThat(metricRegistry.getMetrics()).hasSize(1);
        assertThat(metricRegistry.getGauges().get("resilience4j.retry.testName.retry_max_ratio").getValue()).isEqualTo(0.0);
    }

    @Test
    public void shouldUseCustomPrefix() throws Throwable {
        //Given
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        Retry retry = retryRegistry.retry("testName");
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        metricRegistry.registerAll(RetryMetrics.ofRetryRegistry("testPrefix",retryRegistry));

        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        // Setup circuitbreaker with retry
        CheckedFunction0<String> decoratedSupplier = CircuitBreaker
                .decorateCheckedSupplier(circuitBreaker, helloWorldService::returnHelloWorld);
        decoratedSupplier = Retry
                .decorateCheckedSupplier(retry, decoratedSupplier);

        //When
        String value = Try.of(decoratedSupplier)
                .recover(throwable -> "Hello from Recovery").get();

        //Then
        assertThat(value).isEqualTo("Hello world");
        // Then the helloWorldService should be invoked 1 context
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorld();
        assertThat(metricRegistry.getMetrics()).hasSize(1);
        assertThat(metricRegistry.getGauges().get("testPrefix.testName.retry_max_ratio").getValue()).isEqualTo(0.0);
    }
}
