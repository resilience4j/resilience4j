/*
 * Copyright 2017 Dan Maas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.resilience4j.reactor.retry;

import io.github.resilience4j.retry.MaxRetriesExceededException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.test.HelloWorldException;
import io.github.resilience4j.test.HelloWorldService;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class RetryOperatorTest {

    private HelloWorldService helloWorldService;

    @BeforeClass
    public static void beforeClass() {
        //BlockHound.install(new ReactorIntegration());
    }

    @Before
    public void setUp() {
        helloWorldService = mock(HelloWorldService.class);
    }

    @Test
    public void returnOnCompleteUsingMono() {
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);
        RetryOperator<String> retryOperator = RetryOperator.of(retry);
        given(helloWorldService.returnHelloWorld())
            .willReturn("Hello world")
            .willThrow(new HelloWorldException())
            .willThrow(new HelloWorldException())
            .willReturn("Hello world");

        StepVerifier.create(Mono.fromCallable(helloWorldService::returnHelloWorld)
            .transformDeferred(retryOperator))
            .expectNext("Hello world")
            .expectComplete()
            .verify(Duration.ofSeconds(1));
        StepVerifier.create(Mono.fromCallable(helloWorldService::returnHelloWorld)
            .transformDeferred(retryOperator))
            .expectNext("Hello world")
            .expectComplete()
            .verify(Duration.ofSeconds(1));

        then(helloWorldService).should(times(4)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
    }


    @Test(expected = StackOverflowError.class)
    public void shouldNotRetryUsingMonoStackOverFlow() {
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);
        RetryOperator<String> retryOperator = RetryOperator.of(retry);
        given(helloWorldService.returnHelloWorld())
            .willThrow(new StackOverflowError("BAM!"));

        StepVerifier.create(Mono.fromCallable(helloWorldService::returnHelloWorld)
            .transformDeferred(retryOperator))
            .expectSubscription()
            .expectError(StackOverflowError.class)
            .verify(Duration.ofSeconds(1));
    }

    @Test
    public void shouldNotRetryWhenItThrowErrorMono() {
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);
        RetryOperator<String> retryOperator = RetryOperator.of(retry);
        given(helloWorldService.returnHelloWorld())
            .willThrow(new Error("BAM!"));

        StepVerifier.create(Mono.fromCallable(helloWorldService::returnHelloWorld)
            .transformDeferred(retryOperator))
            .expectSubscription()
            .expectError(Error.class)
            .verify(Duration.ofSeconds(1));

        then(helloWorldService).should().returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
    }


    @Test
    public void returnOnErrorUsingMono() {
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);
        RetryOperator<String> retryOperator = RetryOperator.of(retry);
        given(helloWorldService.returnHelloWorld())
            .willThrow(new HelloWorldException());

        StepVerifier.create(Mono.fromCallable(helloWorldService::returnHelloWorld)
            .transformDeferred(retryOperator))
            .expectSubscription()
            .expectError(HelloWorldException.class)
            .verify(Duration.ofSeconds(1));

        StepVerifier.create(Mono.fromCallable(helloWorldService::returnHelloWorld)
            .transformDeferred(retryOperator))
            .expectSubscription()
            .expectError(HelloWorldException.class)
            .verify(Duration.ofSeconds(1));

        then(helloWorldService).should(times(6)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void doNotRetryFromPredicateUsingMono() {
        RetryConfig config = RetryConfig.custom()
            .retryOnException(t -> t instanceof IOException)
            .waitDuration(Duration.ofMillis(50))
            .maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
            .willThrow(new HelloWorldException());

        StepVerifier.create(Mono.fromCallable(helloWorldService::returnHelloWorld)
            .transformDeferred(RetryOperator.of(retry)))
            .expectSubscription()
            .expectError(HelloWorldException.class)
            .verify(Duration.ofSeconds(1));

        then(helloWorldService).should().returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void retryOnResultUsingMono() {
        RetryConfig config = RetryConfig.<String>custom()
            .retryOnResult("retry"::equals)
            .waitDuration(Duration.ofMillis(10))
            .maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
            .willReturn("retry")
            .willReturn("success");

        StepVerifier.create(Mono.fromCallable(helloWorldService::returnHelloWorld)
            .transformDeferred(RetryOperator.of(retry)))
            .expectSubscription()
            .expectNext("success")
            .expectComplete()
            .verify(Duration.ofSeconds(1));

        then(helloWorldService).should(times(2)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
    }

    @Test
    public void retryOnResultFailAfterMaxAttemptsUsingMono() {
        RetryConfig config = RetryConfig.<String>custom()
            .retryOnResult("retry"::equals)
            .waitDuration(Duration.ofMillis(10))
            .maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
            .willReturn("retry");

        StepVerifier.create(Mono.fromCallable(helloWorldService::returnHelloWorld)
            .transformDeferred(RetryOperator.of(retry)))
            .expectSubscription()
            .expectNextCount(1)
            .expectComplete()
            .verify(Duration.ofSeconds(1));

        then(helloWorldService).should(times(3)).returnHelloWorld();
    }

    @Test
    public void retryOnResultFailAfterMaxAttemptsWithExceptionUsingMono() {
        RetryConfig config = RetryConfig.<String>custom()
            .retryOnResult("retry"::equals)
            .waitDuration(Duration.ofMillis(10))
            .maxAttempts(3)
            .failAfterMaxAttempts(true)
            .build();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
            .willReturn("retry");

        StepVerifier.create(Mono.fromCallable(helloWorldService::returnHelloWorld)
            .transformDeferred(RetryOperator.of(retry)))
            .expectSubscription()
            .expectError(MaxRetriesExceededException.class)
            .verify(Duration.ofSeconds(1));

        then(helloWorldService).should(times(3)).returnHelloWorld();
    }

    @Test
    public void shouldFailWithExceptionFlux() {
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);
        RetryOperator<Object> retryOperator = RetryOperator.of(retry);

        StepVerifier.create(Flux.error(new HelloWorldException())
            .transformDeferred(retryOperator))
            .expectSubscription()
            .expectError(HelloWorldException.class)
            .verify(Duration.ofSeconds(1));

        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void retryOnResultUsingFlux() {
        RetryConfig config = RetryConfig.<String>custom()
            .retryOnResult("retry"::equals)
            .waitDuration(Duration.ofMillis(10))
            .maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);

        StepVerifier.create(Flux.just("retry", "success")
            .transformDeferred(RetryOperator.of(retry)))
            .expectSubscription()
            .expectNext("retry")
            .expectNext("success")
            .expectComplete()
            .verify(Duration.ofMillis(100));

        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(1);
    }

    @Test
    public void retryOnResultFailAfterMaxAttemptsUsingFlux() {
        RetryConfig config = RetryConfig.<String>custom()
            .retryOnResult("retry"::equals)
            .waitDuration(Duration.ofMillis(10))
            .maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);

        StepVerifier.create(Flux.just("retry")
            .transformDeferred(RetryOperator.of(retry)))
            .expectSubscription()
            .expectNextCount(1)
            .expectComplete()
            .verify(Duration.ofSeconds(1));

        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(1);
    }

    @Test
    public void retryOnResultFailAfterMaxAttemptsWithExceptionUsingFlux() {
        RetryConfig config = RetryConfig.<String>custom()
            .retryOnResult("retry"::equals)
            .waitDuration(Duration.ofMillis(10))
            .maxAttempts(3)
            .failAfterMaxAttempts(true)
            .build();
        Retry retry = Retry.of("testName", config);

        StepVerifier.create(Flux.just("retry")
            .transformDeferred(RetryOperator.of(retry)))
            .expectSubscription()
            .expectNextCount(1)
            .expectError(MaxRetriesExceededException.class)
            .verify(Duration.ofSeconds(1));

        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(1);
    }

    private RetryConfig retryConfig() {
        return RetryConfig.custom()
            .waitDuration(Duration.ofMillis(10))
            .build();
    }
}
