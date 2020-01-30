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

package io.github.resilience4j.retry.transformer;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.test.HelloWorldException;
import io.github.resilience4j.test.HelloWorldService;
import io.reactivex.*;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

public class RetryTransformerTest {

    private HelloWorldService helloWorldService;

    @Before
    public void setUp() {
        helloWorldService = mock(HelloWorldService.class);
    }

    @Test
    public void returnOnCompleteUsingSingle() throws InterruptedException {
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
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
    }


    @Test(expected = StackOverflowError.class)
    public void shouldNotRetryUsingSingleStackOverFlow() {
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
            .willThrow(new StackOverflowError("BAM!"));

        Single.fromCallable(helloWorldService::returnHelloWorld)
            .compose(RetryTransformer.of(retry))
            .test();

        then(helloWorldService).should().returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void shouldNotRetryWhenItThrowErrorSingle() {
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
            .willThrow(new Error("BAM!"));

        Single.fromCallable(helloWorldService::returnHelloWorld)
            .compose(RetryTransformer.of(retry))
            .test()
            .assertError(Error.class)
            .assertNotComplete()
            .assertSubscribed();

        then(helloWorldService).should().returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
    }


    @Test
    public void returnOnErrorUsingSingle() throws InterruptedException {
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
            .willThrow(new HelloWorldException());

        Single.fromCallable(helloWorldService::returnHelloWorld)
            .compose(RetryTransformer.of(retry))
            .test()
            .await()
            .assertError(HelloWorldException.class)
            .assertNotComplete()
            .assertSubscribed();
        Single.fromCallable(helloWorldService::returnHelloWorld)
            .compose(RetryTransformer.of(retry))
            .test()
            .await()
            .assertError(HelloWorldException.class)
            .assertNotComplete()
            .assertSubscribed();

        then(helloWorldService).should(times(6)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void doNotRetryFromPredicateUsingSingle() {
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
            .assertNotComplete()
            .assertSubscribed();

        then(helloWorldService).should().returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void retryOnResultUsingSingle() throws InterruptedException {
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
            .assertComplete()
            .assertSubscribed();

        then(helloWorldService).should(times(2)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
    }

    @Test
    public void retryOnResultFailAfterMaxAttemptsUsingSingle() throws InterruptedException {
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
            .assertComplete()
            .assertSubscribed();

        then(helloWorldService).should(times(3)).returnHelloWorld();
    }

    @Test
    public void returnOnCompleteUsingMaybe() throws InterruptedException {
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
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void returnOnErrorUsingMaybe() throws InterruptedException {
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
            .willThrow(new HelloWorldException());
        Maybe.fromCallable(helloWorldService::returnHelloWorld)
            .compose(RetryTransformer.of(retry))
            .test()
            .await()
            .assertError(HelloWorldException.class)
            .assertNotComplete()
            .assertSubscribed();
        Maybe.fromCallable(helloWorldService::returnHelloWorld)
            .compose(RetryTransformer.of(retry))
            .test()
            .await()
            .assertError(HelloWorldException.class)
            .assertNotComplete()
            .assertSubscribed();

        then(helloWorldService).should(times(6)).returnHelloWorld();

        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void doNotRetryFromPredicateUsingMaybe() {
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
            .assertNotComplete()
            .assertSubscribed();

        then(helloWorldService).should().returnHelloWorld();

        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void retryOnResultUsingMaybe() throws InterruptedException {
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
            .assertComplete()
            .assertSubscribed();

        then(helloWorldService).should(times(2)).returnHelloWorld();

        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
    }

    @Test
    public void retryOnResultFailAfterMaxAttemptsUsingMaybe() throws InterruptedException {
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
            .assertComplete()
            .assertSubscribed();

        then(helloWorldService).should(times(3)).returnHelloWorld();
    }

    @Test
    public void returnOnCompleteUsingCompletable() throws InterruptedException {
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
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void returnOnErrorUsingCompletable() throws InterruptedException {
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);
        RetryTransformer<Object> retryTransformer = RetryTransformer.of(retry);
        doThrow(new HelloWorldException()).when(helloWorldService).sayHelloWorld();

        Completable.fromRunnable(helloWorldService::sayHelloWorld)
            .compose(retryTransformer)
            .test()
            .await()
            .assertError(HelloWorldException.class)
            .assertNotComplete()
            .assertSubscribed();
        Completable.fromRunnable(helloWorldService::sayHelloWorld)
            .compose(retryTransformer)
            .test()
            .await()
            .assertError(HelloWorldException.class)
            .assertNotComplete()
            .assertSubscribed();

        then(helloWorldService).should(times(6)).sayHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void doNotRetryFromPredicateUsingCompletable() {
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
            .assertNotComplete()
            .assertSubscribed();

        then(helloWorldService).should().sayHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void returnOnCompleteUsingObservable() throws InterruptedException {
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
            .assertNotComplete()
            .assertSubscribed();
        Observable.fromCallable(helloWorldService::returnHelloWorld)
            .compose(retryTransformer)
            .test()
            .await()
            .assertError(HelloWorldException.class)
            .assertNotComplete()
            .assertSubscribed();

        then(helloWorldService).should(times(6)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void returnOnErrorUsingObservable() throws InterruptedException {
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
            .assertNotComplete()
            .assertSubscribed();
        Observable.fromCallable(helloWorldService::returnHelloWorld)
            .compose(retryTransformer)
            .test()
            .await()
            .assertError(HelloWorldException.class)
            .assertNotComplete()
            .assertSubscribed();

        then(helloWorldService).should(times(6)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void doNotRetryFromPredicateUsingObservable() {
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
            .assertNotComplete()
            .assertSubscribed();

        then(helloWorldService).should().returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void retryOnResultUsingObservable() throws InterruptedException {
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
            .assertComplete()
            .assertSubscribed();

        then(helloWorldService).should(times(2)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
    }

    @Test
    public void retryOnResultFailAfterMaxAttemptsUsingObservable() throws InterruptedException {
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
            .assertComplete()
            .assertSubscribed();

        then(helloWorldService).should(times(3)).returnHelloWorld();
    }

    @Test
    public void returnOnCompleteUsingFlowable() throws InterruptedException {
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
            .assertNotComplete()
            .assertSubscribed();

        Flowable.fromCallable(helloWorldService::returnHelloWorld)
            .compose(retryTransformer)
            .test()
            .await()
            .assertError(HelloWorldException.class)
            .assertNotComplete()
            .assertSubscribed();

        then(helloWorldService).should(times(6)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void returnOnErrorUsingFlowable() throws InterruptedException {
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
            .assertNotComplete()
            .assertSubscribed();
        Flowable.fromCallable(helloWorldService::returnHelloWorld)
            .compose(retryTransformer)
            .test()
            .await()
            .assertError(HelloWorldException.class)
            .assertNotComplete()
            .assertSubscribed();

        then(helloWorldService).should(times(6)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void doNotRetryFromPredicateUsingFlowable() {
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
            .assertNotComplete()
            .assertSubscribed();

        then(helloWorldService).should().returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void retryOnResultUsingFlowable() throws InterruptedException {
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
            .assertComplete()
            .assertSubscribed();

        then(helloWorldService).should(times(2)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
    }

    @Test
    public void retryOnResultFailAfterMaxAttemptsUsingFlowable() throws InterruptedException {
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
            .assertComplete()
            .assertSubscribed();

        then(helloWorldService).should(times(3)).returnHelloWorld();
    }

    private RetryConfig retryConfig() {
        return RetryConfig.custom().waitDuration(Duration.ofMillis(10)).build();
    }
}
