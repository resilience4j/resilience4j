package io.github.resilience4j.reactor;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.test.HelloWorldException;
import io.github.resilience4j.test.HelloWorldService;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;


public class ReactorOperatorFallbackDecoratorTest {

    private HelloWorldService helloWorldService;

    private CircuitBreaker circuitBreaker;

    private final TimeLimiter timeLimiter = mock(TimeLimiter.class);

    @Before
    public void setUp() {
        helloWorldService = mock(HelloWorldService.class);
        circuitBreaker = mock(CircuitBreaker.class, RETURNS_DEEP_STUBS);
    }

    @Test
    @Ignore("when mono completes with onError, onComplete is not triggered and no MaxRetriesExceededException is thrown so this test fails")
    public void shouldFallbackOnRetriesExceededUsingMono() {
        RetryConfig config = RetryConfig.custom()
            .waitDuration(Duration.ofMillis(10))
            .failAfterMaxAttempts(true)
            .build();
        Retry retry = Retry.of("testName", config);
        RetryOperator<String> retryOperator = RetryOperator.of(retry);
        Function<Publisher<String>, Publisher<String>> decorate =
            ReactorOperatorFallbackDecorator.decorateRetry(retryOperator, Mono.just("Fallback"));

        given(helloWorldService.returnHelloWorld())
            .willReturn("Hello world")
            .willThrow(new HelloWorldException())
            .willThrow(new HelloWorldException())
            .willThrow(new HelloWorldException())
            .willReturn("Hello world");

        StepVerifier.create(Mono.fromCallable(helloWorldService::returnHelloWorld)
                .transformDeferred(decorate))
            .expectNext("Hello world")
            .expectComplete()
            .verify(Duration.ofSeconds(1));
        StepVerifier.create(Mono.fromCallable(helloWorldService::returnHelloWorld)
                .transformDeferred(decorate))
            .expectNext("Fallback")
            .expectComplete()
            .verify(Duration.ofSeconds(1));

        then(helloWorldService).should(times(4)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(2L);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isZero();
    }

    @Test
    public void shouldFallbackOnRetriesExceededUsingFlux() {
        RetryConfig config = RetryConfig.<String>custom()
            .retryOnResult("retry"::equals)
            .waitDuration(Duration.ofMillis(10))
            .maxAttempts(3)
            .failAfterMaxAttempts(true)
            .build();
        Retry retry = Retry.of("testName", config);

        StepVerifier.create(Flux.just("retry")
                .log()
                .transformDeferred(ReactorOperatorFallbackDecorator.decorateRetry(
                    RetryOperator.of(retry), Mono.just("Fallback"))))
            .expectSubscription()
            .expectNextCount(1)
            .expectNext("Fallback")
            .verifyComplete();

        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isZero();
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(1);
    }

    @Test
    public void shouldFallbackOntimeoutUsingMono() {
        given(timeLimiter.getTimeLimiterConfig())
            .willReturn(TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(1))
                .build());

        Mono<?> mono = Mono.delay(Duration.ofMinutes(1))
            .transformDeferred(
                ReactorOperatorFallbackDecorator.decorateTimeLimiter(TimeLimiterOperator.of(timeLimiter),
                    Mono.just(-1L)));

        StepVerifier.create(mono)
            .expectNextMatches(o -> o instanceof Long && ((long) o) == -1L)
            .expectComplete()
            .verify(Duration.ofMinutes(1));
        then(timeLimiter).should()
            .onError(any(TimeoutException.class));
    }

    @Test
    public void shouldFallbackOnTimeoutUsingFlux() {
        given(timeLimiter.getTimeLimiterConfig())
            .willReturn(TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(1))
                .build());

        Flux<?> flux = Flux.interval(Duration.ofSeconds(1))
            .transformDeferred(
                ReactorOperatorFallbackDecorator.decorateTimeLimiter(TimeLimiterOperator.of(timeLimiter),
                    Mono.just(-1L)));

        StepVerifier.create(flux)
            .expectNextMatches(o -> o instanceof Long && ((long) o) == -1L)
            .expectComplete()
            .verify(Duration.ofMinutes(1));
        then(timeLimiter).should()
            .onError(any(TimeoutException.class));
    }

    @Test
    public void shouldFallbackOnCircuitOpen() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(false);

        StepVerifier.create(
                Flux.<String>error(new IOException("BAM!"))
                    .transformDeferred(
                        ReactorOperatorFallbackDecorator.decorateCircuitBreaker(CircuitBreakerOperator.of(circuitBreaker),
                            Mono.just("Fallback")))
            )
            .expectNext("Fallback")
            .expectComplete()
            .verify(Duration.ofSeconds(1));

        verify(circuitBreaker, never()).onResult(anyLong(), any(TimeUnit.class), any());
    }


}