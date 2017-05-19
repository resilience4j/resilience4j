package io.github.resilience4j.metrics;

import com.codahale.metrics.MetricRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.test.HelloWorldService;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;

import javax.xml.ws.WebServiceException;

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
    public void shouldRegisterMetricsWithoutRetry() throws Throwable {
        //Given
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        Retry retry = retryRegistry.retry("testName");
        metricRegistry.registerAll(RetryMetrics.ofRetryRegistry(retryRegistry));

        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        // Setup circuitbreaker with retry
        String value = retry.executeSupplier(helloWorldService::returnHelloWorld);

        //Then
        assertThat(value).isEqualTo("Hello world");
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorld();
        assertThat(metricRegistry.getMetrics()).hasSize(4);
        assertThat(metricRegistry.getGauges().get("resilience4j.retry.testName." + RetryMetrics.SUCCESSFUL_CALLS_WITH_RETRY).getValue()).isEqualTo(0L);
        assertThat(metricRegistry.getGauges().get("resilience4j.retry.testName." + RetryMetrics.SUCCESSFUL_CALLS_WITHOUT_RETRY).getValue()).isEqualTo(1L);
        assertThat(metricRegistry.getGauges().get("resilience4j.retry.testName." + RetryMetrics.FAILED_CALLS_WITH_RETRY).getValue()).isEqualTo(0L);
        assertThat(metricRegistry.getGauges().get("resilience4j.retry.testName." + RetryMetrics.FAILED_CALLS_WITHOUT_RETRY).getValue()).isEqualTo(0L);
    }

    @Test
    public void shouldRegisterMetricsWithRetry() throws Throwable {
        //Given
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        Retry retry = retryRegistry.retry("testName");
        metricRegistry.registerAll(RetryMetrics.ofRetryRegistry(retryRegistry));

        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorld())
                .willThrow(new WebServiceException("BAM!"))
                .willReturn("Hello world")
                .willThrow(new WebServiceException("BAM!"))
                .willThrow(new WebServiceException("BAM!"))
                .willThrow(new WebServiceException("BAM!"));

        // Setup circuitbreaker with retry
        String value1 = retry.executeSupplier(helloWorldService::returnHelloWorld);
        Try.ofSupplier(Retry.decorateSupplier(retry, helloWorldService::returnHelloWorld));

        //Then
        assertThat(value1).isEqualTo("Hello world");
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(5)).returnHelloWorld();
        assertThat(metricRegistry.getMetrics()).hasSize(4);
        assertThat(metricRegistry.getGauges().get("resilience4j.retry.testName." + RetryMetrics.SUCCESSFUL_CALLS_WITH_RETRY).getValue()).isEqualTo(1L);
        assertThat(metricRegistry.getGauges().get("resilience4j.retry.testName." + RetryMetrics.SUCCESSFUL_CALLS_WITHOUT_RETRY).getValue()).isEqualTo(0L);
        assertThat(metricRegistry.getGauges().get("resilience4j.retry.testName." + RetryMetrics.FAILED_CALLS_WITH_RETRY).getValue()).isEqualTo(1L);
        assertThat(metricRegistry.getGauges().get("resilience4j.retry.testName." + RetryMetrics.FAILED_CALLS_WITHOUT_RETRY).getValue()).isEqualTo(0L);
    }

    @Test
    public void shouldUseCustomPrefix() throws Throwable {
        //Given
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        Retry retry = retryRegistry.retry("testName");
        metricRegistry.registerAll(RetryMetrics.ofRetryRegistry("testPrefix",retryRegistry));

        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        String value = retry.executeSupplier(helloWorldService::returnHelloWorld);

        //Then
        assertThat(value).isEqualTo("Hello world");
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorld();
        assertThat(metricRegistry.getMetrics()).hasSize(4);
        assertThat(metricRegistry.getGauges().get("testPrefix.testName." + RetryMetrics.SUCCESSFUL_CALLS_WITH_RETRY).getValue()).isEqualTo(0L);
        assertThat(metricRegistry.getGauges().get("testPrefix.testName." + RetryMetrics.SUCCESSFUL_CALLS_WITHOUT_RETRY).getValue()).isEqualTo(1L);
        assertThat(metricRegistry.getGauges().get("testPrefix.testName." + RetryMetrics.FAILED_CALLS_WITH_RETRY).getValue()).isEqualTo(0L);
        assertThat(metricRegistry.getGauges().get("testPrefix.testName." + RetryMetrics.FAILED_CALLS_WITHOUT_RETRY).getValue()).isEqualTo(0L);
    }
}
