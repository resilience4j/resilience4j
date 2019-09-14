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
import io.github.resilience4j.test.HelloWorldService;
import io.reactivex.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

import javax.xml.ws.WebServiceException;
import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

public class RetryTransformerTest {

    private HelloWorldService helloWorldService;

    @Before
    public void setUp() {
        helloWorldService = Mockito.mock(HelloWorldService.class);
    }

    @Test
    public void returnOnCompleteUsingSingle() {
        //Given
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);

        given(helloWorldService.returnHelloWorld())
                .willReturn("Hello world")
                .willThrow(new WebServiceException("BAM!"))
                .willThrow(new WebServiceException("BAM!"))
                .willReturn("Hello world");

        //When
        Single.fromCallable(helloWorldService::returnHelloWorld)
                .compose(RetryTransformer.of(retry))
                .test()
                .assertValueCount(1)
                .assertValues("Hello world")
                .assertComplete();

        Single.fromCallable(helloWorldService::returnHelloWorld)
                .compose(RetryTransformer.of(retry))
                .test()
                .assertValueCount(1)
                .assertValues("Hello world")
                .assertComplete();

        //Then
        BDDMockito.then(helloWorldService).should(Mockito.times(4)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
    }


    @Test(expected = StackOverflowError.class)
    public void shouldNotRetryUsingSingleStackOverFlow() {
        //Given
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);

        given(helloWorldService.returnHelloWorld())
                .willThrow(new StackOverflowError("BAM!"));

        //When
        Single.fromCallable(helloWorldService::returnHelloWorld)
                .compose(RetryTransformer.of(retry))
                .test();


        //Then
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void shouldNotRetryWhenItThrowErrorSingle() {
        //Given
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);

        given(helloWorldService.returnHelloWorld())
                .willThrow(new Error("BAM!"));

        //When
        Single.fromCallable(helloWorldService::returnHelloWorld)
                .compose(RetryTransformer.of(retry))
                .test()
                .assertError(Error.class)
                .assertNotComplete()
                .assertSubscribed();
        //Then
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
    }


    @Test
    public void returnOnErrorUsingSingle() {
        //Given
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);

        given(helloWorldService.returnHelloWorld())
                .willThrow(new WebServiceException("BAM!"));

        //When
        Single.fromCallable(helloWorldService::returnHelloWorld)
                .compose(RetryTransformer.of(retry))
                .test()
                .assertError(WebServiceException.class)
                .assertNotComplete()
                .assertSubscribed();

        Single.fromCallable(helloWorldService::returnHelloWorld)
                .compose(RetryTransformer.of(retry))
                .test()
                .assertError(WebServiceException.class)
                .assertNotComplete()
                .assertSubscribed();
        //Then
        BDDMockito.then(helloWorldService).should(Mockito.times(6)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void doNotRetryFromPredicateUsingSingle() {
        //Given
        RetryConfig config = RetryConfig.custom()
                .retryOnException(t -> t instanceof IOException)
                .waitDuration(Duration.ofMillis(50))
                .maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
                .willThrow(new WebServiceException("BAM!"));

        //When
        Single.fromCallable(helloWorldService::returnHelloWorld)
                .compose(RetryTransformer.of(retry))
                .test()
                .assertError(WebServiceException.class)
                .assertNotComplete()
                .assertSubscribed();
        //Then
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void retryOnResultUsingSingle() {
        //Given
        RetryConfig config = RetryConfig.<String>custom()
                .retryOnResult("retry"::equals)
                .waitDuration(Duration.ofMillis(50))
                .maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
                .willReturn("retry")
                .willReturn("success");

        //When
        Single.fromCallable(helloWorldService::returnHelloWorld)
                .compose(RetryTransformer.of(retry))
                .test()
                .assertValueCount(1)
                .assertValue("success")
                .assertComplete()
                .assertSubscribed();
        //Then
        BDDMockito.then(helloWorldService).should(Mockito.times(2)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
    }

    @Test
    public void retryOnResultFailAfterMaxAttemptsUsingSingle() {
        //Given
        RetryConfig config = RetryConfig.<String>custom()
                .retryOnResult("retry"::equals)
                .waitDuration(Duration.ofMillis(50))
                .maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
                .willReturn("retry");

        //When
        Single.fromCallable(helloWorldService::returnHelloWorld)
                .compose(RetryTransformer.of(retry))
                .test()
                .assertValue("retry")
                .assertComplete()
                .assertSubscribed();
        //Then
        BDDMockito.then(helloWorldService).should(Mockito.times(3)).returnHelloWorld();
    }

    @Test
    public void returnOnCompleteUsingMaybe() {
        //Given
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);
        RetryTransformer<Object> retryTransformer = RetryTransformer.of(retry);

        given(helloWorldService.returnHelloWorld())
                .willReturn("Hello world")
                .willThrow(new WebServiceException("BAM!"))
                .willThrow(new WebServiceException("BAM!"))
                .willReturn("Hello world");

        //When
        Maybe.fromCallable(helloWorldService::returnHelloWorld)
                .compose(retryTransformer)
                .test()
                .assertValueCount(1)
                .assertValues("Hello world")
                .assertComplete();

        Maybe.fromCallable(helloWorldService::returnHelloWorld)
                .compose(retryTransformer)
                .test()
                .assertValueCount(1)
                .assertValues("Hello world")
                .assertComplete();

        //Then
        BDDMockito.then(helloWorldService).should(Mockito.times(4)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void returnOnErrorUsingMaybe() {
        //Given
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);

        given(helloWorldService.returnHelloWorld())
                .willThrow(new WebServiceException("BAM!"));

        //When
        Maybe.fromCallable(helloWorldService::returnHelloWorld)
                .compose(RetryTransformer.of(retry))
                .test()
                .assertError(WebServiceException.class)
                .assertNotComplete()
                .assertSubscribed();

        Maybe.fromCallable(helloWorldService::returnHelloWorld)
                .compose(RetryTransformer.of(retry))
                .test()
                .assertError(WebServiceException.class)
                .assertNotComplete()
                .assertSubscribed();
        //Then
        BDDMockito.then(helloWorldService).should(Mockito.times(6)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void doNotRetryFromPredicateUsingMaybe() {
        //Given
        RetryConfig config = RetryConfig.custom()
                .retryOnException(t -> t instanceof IOException)
                .waitDuration(Duration.ofMillis(50))
                .maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
                .willThrow(new WebServiceException("BAM!"));

        //When
        Maybe.fromCallable(helloWorldService::returnHelloWorld)
                .compose(RetryTransformer.of(retry))
                .test()
                .assertError(WebServiceException.class)
                .assertNotComplete()
                .assertSubscribed();
        //Then
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void retryOnResultUsingMaybe() {
        //Given
        RetryConfig config = RetryConfig.<String>custom()
                .retryOnResult("retry"::equals)
                .waitDuration(Duration.ofMillis(50))
                .maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
                .willReturn("retry")
                .willReturn("success");

        //When
        Maybe.fromCallable(helloWorldService::returnHelloWorld)
                .compose(RetryTransformer.of(retry))
                .test()
                .assertValueCount(1)
                .assertValue("success")
                .assertComplete()
                .assertSubscribed();
        //Then
        BDDMockito.then(helloWorldService).should(Mockito.times(2)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
    }

    @Test
    public void retryOnResultFailAfterMaxAttemptsUsingMaybe() {
        //Given
        RetryConfig config = RetryConfig.<String>custom()
                .retryOnResult("retry"::equals)
                .waitDuration(Duration.ofMillis(50))
                .maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
                .willReturn("retry");

        //When
        Maybe.fromCallable(helloWorldService::returnHelloWorld)
                .compose(RetryTransformer.of(retry))
                .test()
                .assertValueCount(1)
                .assertValue("retry")
                .assertComplete()
                .assertSubscribed();
        //Then
        BDDMockito.then(helloWorldService).should(Mockito.times(3)).returnHelloWorld();
    }

    @Test
    public void returnOnCompleteUsingCompletable() {
        //Given
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);
        RetryTransformer<Object> retryTransformer = RetryTransformer.of(retry);
        doNothing()
                .doThrow(new WebServiceException("BAM!"))
                .doThrow(new WebServiceException("BAM!"))
                .doNothing()
                .when(helloWorldService).sayHelloWorld();

        //When
        Completable.fromRunnable(helloWorldService::sayHelloWorld)
                .compose(retryTransformer)
                .test()
                .assertNoValues()
                .assertComplete();

        Completable.fromRunnable(helloWorldService::sayHelloWorld)
                .compose(retryTransformer)
                .test()
                .assertNoValues()
                .assertComplete();

        //Then
        BDDMockito.then(helloWorldService).should(Mockito.times(4)).sayHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void returnOnErrorUsingCompletable() {
        //Given
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);
        RetryTransformer<Object> retryTransformer = RetryTransformer.of(retry);
        doThrow(new WebServiceException("BAM!")).when(helloWorldService).sayHelloWorld();

        //When
        Completable.fromRunnable(helloWorldService::sayHelloWorld)
                .compose(retryTransformer)
                .test()
                .assertError(WebServiceException.class)
                .assertNotComplete()
                .assertSubscribed();

        Completable.fromRunnable(helloWorldService::sayHelloWorld)
                .compose(retryTransformer)
                .test()
                .assertError(WebServiceException.class)
                .assertNotComplete()
                .assertSubscribed();
        //Then
        BDDMockito.then(helloWorldService).should(Mockito.times(6)).sayHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void doNotRetryFromPredicateUsingCompletable() {
        //Given
        RetryConfig config = RetryConfig.custom()
                .retryOnException(t -> t instanceof IOException)
                .waitDuration(Duration.ofMillis(50))
                .maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);
        doThrow(new WebServiceException("BAM!")).when(helloWorldService).sayHelloWorld();

        //When
        Completable.fromRunnable(helloWorldService::sayHelloWorld)
                .compose(RetryTransformer.of(retry))
                .test()
                .assertError(WebServiceException.class)
                .assertNotComplete()
                .assertSubscribed();
        //Then
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).sayHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void returnOnCompleteUsingObservable() {
        //Given
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);
        RetryTransformer<Object> retryTransformer = RetryTransformer.of(retry);

        given(helloWorldService.returnHelloWorld())
                .willThrow(new WebServiceException("BAM!"));

        //When
        Observable.fromCallable(helloWorldService::returnHelloWorld)
                .compose(retryTransformer)
                .test()
                .assertError(WebServiceException.class)
                .assertNotComplete()
                .assertSubscribed();

        Observable.fromCallable(helloWorldService::returnHelloWorld)
                .compose(retryTransformer)
                .test()
                .assertError(WebServiceException.class)
                .assertNotComplete()
                .assertSubscribed();
        //Then
        BDDMockito.then(helloWorldService).should(Mockito.times(6)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void returnOnErrorUsingObservable() {
        //Given
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);
        RetryTransformer<Object> retryTransformer = RetryTransformer.of(retry);

        given(helloWorldService.returnHelloWorld())
                .willThrow(new WebServiceException("BAM!"));

        //When
        Observable.fromCallable(helloWorldService::returnHelloWorld)
                .compose(retryTransformer)
                .test()
                .assertError(WebServiceException.class)
                .assertNotComplete()
                .assertSubscribed();

        Observable.fromCallable(helloWorldService::returnHelloWorld)
                .compose(retryTransformer)
                .test()
                .assertError(WebServiceException.class)
                .assertNotComplete()
                .assertSubscribed();
        //Then
        BDDMockito.then(helloWorldService).should(Mockito.times(6)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void doNotRetryFromPredicateUsingObservable() {
        //Given
        RetryConfig config = RetryConfig.custom()
                .retryOnException(t -> t instanceof IOException)
                .waitDuration(Duration.ofMillis(50))
                .maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
                .willThrow(new WebServiceException("BAM!"));

        //When
        Observable.fromCallable(helloWorldService::returnHelloWorld)
                .compose(RetryTransformer.of(retry))
                .test()
                .assertError(WebServiceException.class)
                .assertNotComplete()
                .assertSubscribed();
        //Then
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void retryOnResultUsingObservable() {
        //Given
        RetryConfig config = RetryConfig.<String>custom()
                .retryOnResult("retry"::equals)
                .waitDuration(Duration.ofMillis(50))
                .maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
                .willReturn("retry")
                .willReturn("success");

        //When
        Observable.fromCallable(helloWorldService::returnHelloWorld)
                .compose(RetryTransformer.of(retry))
                .test()
                .assertValueCount(1)
                .assertValue("success")
                .assertComplete()
                .assertSubscribed();
        //Then
        BDDMockito.then(helloWorldService).should(Mockito.times(2)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
    }

    @Test
    public void retryOnResultFailAfterMaxAttemptsUsingObservable() {
        //Given
        RetryConfig config = RetryConfig.<String>custom()
                .retryOnResult("retry"::equals)
                .waitDuration(Duration.ofMillis(50))
                .maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
                .willReturn("retry");

        //When
        Observable.fromCallable(helloWorldService::returnHelloWorld)
                .compose(RetryTransformer.of(retry))
                .test()
                .assertValueCount(1)
                .assertValue("retry")
                .assertComplete()
                .assertSubscribed();
        //Then
        BDDMockito.then(helloWorldService).should(Mockito.times(3)).returnHelloWorld();
    }

    @Test
    public void returnOnCompleteUsingFlowable() {
        //Given
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);
        RetryTransformer<Object> retryTransformer = RetryTransformer.of(retry);

        given(helloWorldService.returnHelloWorld())
                .willThrow(new WebServiceException("BAM!"));

        //When
        Flowable.fromCallable(helloWorldService::returnHelloWorld)
                .compose(retryTransformer)
                .test()
                .assertError(WebServiceException.class)
                .assertNotComplete()
                .assertSubscribed();

        Flowable.fromCallable(helloWorldService::returnHelloWorld)
                .compose(retryTransformer)
                .test()
                .assertError(WebServiceException.class)
                .assertNotComplete()
                .assertSubscribed();
        //Then
        BDDMockito.then(helloWorldService).should(Mockito.times(6)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void returnOnErrorUsingFlowable() {
        //Given
        RetryConfig config = retryConfig();
        Retry retry = Retry.of("testName", config);
        RetryTransformer<Object> retryTransformer = RetryTransformer.of(retry);

        given(helloWorldService.returnHelloWorld())
                .willThrow(new WebServiceException("BAM!"));

        //When
        Flowable.fromCallable(helloWorldService::returnHelloWorld)
                .compose(retryTransformer)
                .test()
                .assertError(WebServiceException.class)
                .assertNotComplete()
                .assertSubscribed();

        Flowable.fromCallable(helloWorldService::returnHelloWorld)
                .compose(retryTransformer)
                .test()
                .assertError(WebServiceException.class)
                .assertNotComplete()
                .assertSubscribed();
        //Then
        BDDMockito.then(helloWorldService).should(Mockito.times(6)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void doNotRetryFromPredicateUsingFlowable() {
        //Given
        RetryConfig config = RetryConfig.custom()
                .retryOnException(t -> t instanceof IOException)
                .waitDuration(Duration.ofMillis(50))
                .maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
                .willThrow(new WebServiceException("BAM!"));

        //When
        Flowable.fromCallable(helloWorldService::returnHelloWorld)
                .compose(RetryTransformer.of(retry))
                .test()
                .assertError(WebServiceException.class)
                .assertNotComplete()
                .assertSubscribed();
        //Then
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
    }

    @Test
    public void retryOnResultUsingFlowable() {
        //Given
        RetryConfig config = RetryConfig.<String>custom()
                .retryOnResult("retry"::equals)
                .waitDuration(Duration.ofMillis(50))
                .maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
                .willReturn("retry")
                .willReturn("success");

        //When
        Flowable.fromCallable(helloWorldService::returnHelloWorld)
                .compose(RetryTransformer.of(retry))
                .test()
                .assertValueCount(1)
                .assertValue("success")
                .assertComplete()
                .assertSubscribed();
        //Then
        BDDMockito.then(helloWorldService).should(Mockito.times(2)).returnHelloWorld();
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
    }

    @Test
    public void retryOnResultFailAfterMaxAttemptsUsingFlowable() {
        //Given
        RetryConfig config = RetryConfig.<String>custom()
                .retryOnResult("retry"::equals)
                .waitDuration(Duration.ofMillis(50))
                .maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);
        given(helloWorldService.returnHelloWorld())
                .willReturn("retry");

        //When
        Flowable.fromCallable(helloWorldService::returnHelloWorld)
                .compose(RetryTransformer.of(retry))
                .test()
                .assertValueCount(1)
                .assertValue("retry")
                .assertComplete()
                .assertSubscribed();
        //Then
        BDDMockito.then(helloWorldService).should(Mockito.times(3)).returnHelloWorld();
    }

    private RetryConfig retryConfig() {
        return RetryConfig.custom().waitDuration(Duration.ofMillis(50)).build();
    }
}
