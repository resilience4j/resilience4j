/*
 * Copyright 2026 Dan Maas
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

package io.github.resilience4j.rxjava3.retry.transformer;

import io.github.resilience4j.retry.MaxRetriesExceededException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.test.HelloWorldException;
import io.github.resilience4j.test.HelloWorldService;
import io.reactivex.rxjava3.core.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

class RetryTransformerTest {

    private HelloWorldService helloWorldService;

    @BeforeEach
    void setUp() {
        helloWorldService = mock(HelloWorldService.class);
    }

    @Test
    void returnOnCompleteUsingSingle() throws InterruptedException {
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
            .willReturn("Hello world")
            .willThrow(new HelloWorldException())
            .willThrow(new HelloWorldException())
            .willReturn("Hello world");

        Single.fromCallable(helloWorldService::returnHelloWorld)
            .compose(RetryTransformer.of(retry))
            .test()
            .await()
            .assertValueCount(1)
            .assertValues("Hello world")
            .assertComplete();
        Single.fromCallable(helloWorldService::returnHelloWorld)
            .compose(RetryTransformer.of(retry))
            .test()
            .await()
            .assertValueCount(1)
            .assertValues("Hello world")
            .assertComplete();

        then(helloWorldService).should(times(4)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isZero();
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isZero();
    }

    @Test
    void shouldNotRetryUsingSingleStackOverFlow() {
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
            .willThrow(new StackOverflowError("BAM!"));

        assertThatThrownBy(() ->
            Single.fromCallable(helloWorldService::returnHelloWorld)
                .compose(RetryTransformer.of(retry))
                .test())
            .isInstanceOf(StackOverflowError.class);

        then(helloWorldService).should().returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isZero();
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isZero();
    }

    @Test
    void shouldNotRetryWhenItThrowErrorSingle() {
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
            .willThrow(new Error("BAM!"));

        Single.fromCallable(helloWorldService::returnHelloWorld)
            .compose(RetryTransformer.of(retry))
            .test()
            .assertError(Error.class)
            .assertNotComplete();

        then(helloWorldService).should().returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isZero();
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isZero();
    }

    @Test
    void returnOnErrorUsingSingle() throws InterruptedException {
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
            .willThrow(new HelloWorldException());

        Single.fromCallable(helloWorldService::returnHelloWorld)
            .compose(RetryTransformer.of(retry))
            .test()
            .await()
            .assertError(HelloWorldException.class)
            .assertNotComplete();
        Single.fromCallable(helloWorldService::returnHelloWorld)
            .compose(RetryTransformer.of(retry))
            .test()
            .await()
            .assertError(HelloWorldException.class)
            .assertNotComplete();

        then(helloWorldService).should(times(6)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isZero();
    }

    @Test
    void doNotRetryFromPredicateUsingSingle() {
        RetryConfig config = RetryConfig.custom()
            .retryOnException(t -> t instanceof IOException)
            .waitDuration(Duration.ofMillis(50))
            .maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
            .willThrow(new HelloWorldException());
        Single.fromCallable(helloWorldService::returnHelloWorld)
            .compose(RetryTransformer.of(retry))
            .test()
            .assertError(HelloWorldException.class)
            .assertNotComplete();

        then(helloWorldService).should().returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isZero();
    }

    @Test
    void retryOnResultUsingSingle() throws InterruptedException {
        RetryConfig config = RetryConfig.<String>custom()
            .retryOnResult("retry"::equals)
            .waitDuration(Duration.ofMillis(50))
            .maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
            .willReturn("retry")
            .willReturn("success");

        Single.fromCallable(helloWorldService::returnHelloWorld)
            .compose(RetryTransformer.of(retry))
            .test()
            .await()
            .assertValueCount(1)
            .assertValue("success")
            .assertComplete();

        then(helloWorldService).should(times(2)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isZero();
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
    }

    @Test
    void retryOnResultFailAfterMaxAttemptsUsingSingle() throws InterruptedException {
        RetryConfig config = RetryConfig.<String>custom()
            .retryOnResult("retry"::equals)
            .waitDuration(Duration.ofMillis(50))
            .maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
            .willReturn("retry");

        Single.fromCallable(helloWorldService::returnHelloWorld)
            .compose(RetryTransformer.of(retry))
            .test()
            .await()
            .assertValue("retry")
            .assertComplete();

        then(helloWorldService).should(times(3)).returnHelloWorld();
    }

    @Test
    void returnOnCompleteUsingMaybe() throws InterruptedException {
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);
        RetryTransformer<Object> retryTransformer = RetryTransformer.of(retry);
        given(helloWorldService.returnHelloWorld())
            .willReturn("Hello world")
            .willThrow(new HelloWorldException())
            .willThrow(new HelloWorldException())
            .willReturn("Hello world");
        Maybe.fromCallable(helloWorldService::returnHelloWorld)
            .compose(retryTransformer)
            .test()
            .await()
            .assertValueCount(1)
            .assertValues("Hello world")
            .assertComplete();
        Maybe.fromCallable(helloWorldService::returnHelloWorld)
            .compose(retryTransformer)
            .test()
            .await()
            .assertValueCount(1)
            .assertValues("Hello world")
            .assertComplete();

        then(helloWorldService).should(times(4)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isZero();
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isZero();
    }

    @Test
    void returnOnErrorUsingMaybe() throws InterruptedException {
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
            .willThrow(new HelloWorldException());
        Maybe.fromCallable(helloWorldService::returnHelloWorld)
            .compose(RetryTransformer.of(retry))
            .test()
            .await()
            .assertError(HelloWorldException.class)
            .assertNotComplete();
        Maybe.fromCallable(helloWorldService::returnHelloWorld)
            .compose(RetryTransformer.of(retry))
            .test()
            .await()
            .assertError(HelloWorldException.class)
            .assertNotComplete();

        then(helloWorldService).should(times(6)).returnHelloWorld();

        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isZero();
    }

    @Test
    void doNotRetryFromPredicateUsingMaybe() {
        RetryConfig config = RetryConfig.custom()
            .retryOnException(t -> t instanceof IOException)
            .waitDuration(Duration.ofMillis(50))
            .maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
            .willThrow(new HelloWorldException());

        Maybe.fromCallable(helloWorldService::returnHelloWorld)
            .compose(RetryTransformer.of(retry))
            .test()
            .assertError(HelloWorldException.class)
            .assertNotComplete();

        then(helloWorldService).should().returnHelloWorld();

        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isZero();
    }

    @Test
    void retryOnResultUsingMaybe() throws InterruptedException {
        RetryConfig config = RetryConfig.<String>custom()
            .retryOnResult("retry"::equals)
            .waitDuration(Duration.ofMillis(50))
            .maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
            .willReturn("retry")
            .willReturn("success");
        Maybe.fromCallable(helloWorldService::returnHelloWorld)
            .compose(RetryTransformer.of(retry))
            .test()
            .await()
            .assertValueCount(1)
            .assertValue("success")
            .assertComplete();

        then(helloWorldService).should(times(2)).returnHelloWorld();

        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isZero();
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
    }

    @Test
    void retryOnResultFailAfterMaxAttemptsUsingMaybe() throws InterruptedException {
        RetryConfig config = RetryConfig.<String>custom()
            .retryOnResult("retry"::equals)
            .waitDuration(Duration.ofMillis(50))
            .maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
            .willReturn("retry");

        Maybe.fromCallable(helloWorldService::returnHelloWorld)
            .compose(RetryTransformer.of(retry))
            .test()
            .await()
            .assertValueCount(1)
            .assertValue("retry")
            .assertComplete();

        then(helloWorldService).should(times(3)).returnHelloWorld();
    }

    @Test
    void returnOnCompleteUsingCompletable() throws InterruptedException {
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);
        RetryTransformer<Object> retryTransformer = RetryTransformer.of(retry);
        doNothing()
            .doThrow(new HelloWorldException())
            .doThrow(new HelloWorldException())
            .doNothing()
            .when(helloWorldService).sayHelloWorld();
        Completable.fromRunnable(helloWorldService::sayHelloWorld)
            .compose(retryTransformer)
            .test()
            .await()
            .assertNoValues()
            .assertComplete();
        Completable.fromRunnable(helloWorldService::sayHelloWorld)
            .compose(retryTransformer)
            .test()
            .await()
            .assertNoValues()
            .assertComplete();

        then(helloWorldService).should(times(4)).sayHelloWorld();

        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isZero();
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isZero();
    }

    @Test
    void returnOnErrorUsingCompletable() throws InterruptedException {
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);
        RetryTransformer<Object> retryTransformer = RetryTransformer.of(retry);
        doThrow(new HelloWorldException()).when(helloWorldService).sayHelloWorld();

        Completable.fromRunnable(helloWorldService::sayHelloWorld)
            .compose(retryTransformer)
            .test()
            .await()
            .assertError(HelloWorldException.class)
            .assertNotComplete();
        Completable.fromRunnable(helloWorldService::sayHelloWorld)
            .compose(retryTransformer)
            .test()
            .await()
            .assertError(HelloWorldException.class)
            .assertNotComplete();

        then(helloWorldService).should(times(6)).sayHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isZero();
    }

    @Test
    void doNotRetryFromPredicateUsingCompletable() {
        RetryConfig config = RetryConfig.custom()
            .retryOnException(t -> t instanceof IOException)
            .waitDuration(Duration.ofMillis(50))
            .maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);
        doThrow(new HelloWorldException()).when(helloWorldService).sayHelloWorld();

        Completable.fromRunnable(helloWorldService::sayHelloWorld)
            .compose(RetryTransformer.of(retry))
            .test()
            .assertError(HelloWorldException.class)
            .assertNotComplete();

        then(helloWorldService).should().sayHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isZero();
    }

    @Test
    void returnOnCompleteUsingObservable() throws InterruptedException {
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);
        RetryTransformer<Object> retryTransformer = RetryTransformer.of(retry);
        given(helloWorldService.returnHelloWorld())
            .willThrow(new HelloWorldException());

        Observable.fromCallable(helloWorldService::returnHelloWorld)
            .compose(retryTransformer)
            .test()
            .await()
            .assertError(HelloWorldException.class)
            .assertNotComplete();
        Observable.fromCallable(helloWorldService::returnHelloWorld)
            .compose(retryTransformer)
            .test()
            .await()
            .assertError(HelloWorldException.class)
            .assertNotComplete();

        then(helloWorldService).should(times(6)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isZero();
    }

    @Test
    void returnOnErrorUsingObservable() throws InterruptedException {
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);
        RetryTransformer<Object> retryTransformer = RetryTransformer.of(retry);
        given(helloWorldService.returnHelloWorld())
            .willThrow(new HelloWorldException());

        Observable.fromCallable(helloWorldService::returnHelloWorld)
            .compose(retryTransformer)
            .test()
            .await()
            .assertError(HelloWorldException.class)
            .assertNotComplete();
        Observable.fromCallable(helloWorldService::returnHelloWorld)
            .compose(retryTransformer)
            .test()
            .await()
            .assertError(HelloWorldException.class)
            .assertNotComplete();

        then(helloWorldService).should(times(6)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isZero();
    }

    @Test
    void doNotRetryFromPredicateUsingObservable() {
        RetryConfig config = RetryConfig.custom()
            .retryOnException(t -> t instanceof IOException)
            .waitDuration(Duration.ofMillis(50))
            .maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
            .willThrow(new HelloWorldException());

        Observable.fromCallable(helloWorldService::returnHelloWorld)
            .compose(RetryTransformer.of(retry))
            .test()
            .assertError(HelloWorldException.class)
            .assertNotComplete();

        then(helloWorldService).should().returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isZero();
    }

    @Test
    void retryOnResultUsingObservable() throws InterruptedException {
        RetryConfig config = RetryConfig.<String>custom()
            .retryOnResult("retry"::equals)
            .waitDuration(Duration.ofMillis(50))
            .maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
            .willReturn("retry")
            .willReturn("success");

        Observable.fromCallable(helloWorldService::returnHelloWorld)
            .compose(RetryTransformer.of(retry))
            .test()
            .await()
            .assertValueCount(1)
            .assertValue("success")
            .assertComplete();

        then(helloWorldService).should(times(2)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isZero();
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
    }

    @Test
    void retryOnResultFailAfterMaxAttemptsUsingObservable() throws InterruptedException {
        RetryConfig config = RetryConfig.<String>custom()
            .retryOnResult("retry"::equals)
            .waitDuration(Duration.ofMillis(50))
            .maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
            .willReturn("retry");

        Observable.fromCallable(helloWorldService::returnHelloWorld)
            .compose(RetryTransformer.of(retry))
            .test()
            .await()
            .assertValueCount(1)
            .assertValue("retry")
            .assertComplete();

        then(helloWorldService).should(times(3)).returnHelloWorld();
    }

    @Test
    void returnOnCompleteUsingFlowable() throws InterruptedException {
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);
        RetryTransformer<Object> retryTransformer = RetryTransformer.of(retry);
        given(helloWorldService.returnHelloWorld())
            .willThrow(new HelloWorldException());

        Flowable.fromCallable(helloWorldService::returnHelloWorld)
            .compose(retryTransformer)
            .test()
            .await()
            .assertError(HelloWorldException.class)
            .assertNotComplete();

        Flowable.fromCallable(helloWorldService::returnHelloWorld)
            .compose(retryTransformer)
            .test()
            .await()
            .assertError(HelloWorldException.class)
            .assertNotComplete();

        then(helloWorldService).should(times(6)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isZero();
    }

    @Test
    void returnOnErrorUsingFlowable() throws InterruptedException {
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);
        RetryTransformer<Object> retryTransformer = RetryTransformer.of(retry);
        given(helloWorldService.returnHelloWorld())
            .willThrow(new HelloWorldException());

        Flowable.fromCallable(helloWorldService::returnHelloWorld)
            .compose(retryTransformer)
            .test()
            .await()
            .assertError(HelloWorldException.class)
            .assertNotComplete();
        Flowable.fromCallable(helloWorldService::returnHelloWorld)
            .compose(retryTransformer)
            .test()
            .await()
            .assertError(HelloWorldException.class)
            .assertNotComplete();

        then(helloWorldService).should(times(6)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isZero();
    }

    @Test
    void doNotRetryFromPredicateUsingFlowable() {
        RetryConfig config = RetryConfig.custom()
            .retryOnException(t -> t instanceof IOException)
            .waitDuration(Duration.ofMillis(50))
            .maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
            .willThrow(new HelloWorldException());

        Flowable.fromCallable(helloWorldService::returnHelloWorld)
            .compose(RetryTransformer.of(retry))
            .test()
            .assertError(HelloWorldException.class)
            .assertNotComplete();

        then(helloWorldService).should().returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isZero();
    }

    @Test
    void retryOnResultUsingFlowable() throws InterruptedException {
        RetryConfig config = RetryConfig.<String>custom()
            .retryOnResult("retry"::equals)
            .waitDuration(Duration.ofMillis(50))
            .maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
            .willReturn("retry")
            .willReturn("success");

        Flowable.fromCallable(helloWorldService::returnHelloWorld)
            .compose(RetryTransformer.of(retry))
            .test()
            .await()
            .assertValueCount(1)
            .assertValue("success")
            .assertComplete();

        then(helloWorldService).should(times(2)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isZero();
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
    }

    @Test
    void retryOnResultFailAfterMaxAttemptsUsingFlowable() throws InterruptedException {
        RetryConfig config = RetryConfig.<String>custom()
            .retryOnResult("retry"::equals)
            .waitDuration(Duration.ofMillis(50))
            .maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
            .willReturn("retry");

        Flowable.fromCallable(helloWorldService::returnHelloWorld)
            .compose(RetryTransformer.of(retry))
            .test()
            .await()
            .assertValueCount(1)
            .assertValue("retry")
            .assertComplete();

        then(helloWorldService).should(times(3)).returnHelloWorld();
    }

    @Test
    void shouldThrowMaxRetriesExceptionAfterRetriesExhaustedWhenConfigured() throws InterruptedException {
        RetryConfig config = RetryConfig.<String>custom()
            .retryOnResult("retry"::equals)
            .waitDuration(Duration.ofMillis(50))
            .maxAttempts(3)
            .failAfterMaxAttempts(true)
            .build();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
            .willReturn("retry");

        Flowable.fromCallable(helloWorldService::returnHelloWorld)
            .compose(RetryTransformer.of(retry))
            .test()
            .await()
            .assertFailure(MaxRetriesExceededException.class, "retry");

        then(helloWorldService).should(times(3)).returnHelloWorld();
    }

    private RetryConfig retryConfig() {
        return RetryConfig.custom().waitDuration(Duration.ofMillis(10)).build();
    }
}
