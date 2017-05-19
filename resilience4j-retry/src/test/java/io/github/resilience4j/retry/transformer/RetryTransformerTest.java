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
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

import javax.xml.ws.WebServiceException;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

public class RetryTransformerTest {

    private HelloWorldService helloWorldService;

    @Before
    public void setUp(){
        helloWorldService = Mockito.mock(HelloWorldService.class);
    }

    @Test
    public void shouldReturnOnCompleteUsingSingle() {
        //Given
        RetryConfig config = RetryConfig.ofDefaults();
        Retry retry = Retry.of("testName", config);
        RetryTransformer<Object> retryTransformer = RetryTransformer.of(retry);

        given(helloWorldService.returnHelloWorld())
                .willReturn("Hello world")
                .willThrow(new WebServiceException("BAM!"))
                .willThrow(new WebServiceException("BAM!"))
                .willReturn("Hello world");

        //When
        Single.fromCallable(helloWorldService::returnHelloWorld)
                .compose(retryTransformer)
                .test()
                .assertValueCount(1)
                .assertValues("Hello world")
                .assertComplete();

        Single.fromCallable(helloWorldService::returnHelloWorld)
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
    public void shouldReturnOnErrorUsingSingle() {
        //Given
        RetryConfig config = RetryConfig.ofDefaults();
        Retry retry = Retry.of("testName", config);
        RetryTransformer<Object> retryTransformer = RetryTransformer.of(retry);

        given(helloWorldService.returnHelloWorld())
                .willThrow(new WebServiceException("BAM!"));

        //When
        Single.fromCallable(helloWorldService::returnHelloWorld)
                .compose(retryTransformer)
                .test()
                .assertError(WebServiceException.class)
                .assertNotComplete()
                .assertSubscribed();

        Single.fromCallable(helloWorldService::returnHelloWorld)
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
    public void shouldNotRetryFromPredicateUsingSingle() {
        //Given
        RetryConfig config = RetryConfig.custom()
                .retryOnException(t -> t instanceof IOException)
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
    public void shouldReturnOnCompleteUsingObservable() {
        //Given
        RetryConfig config = RetryConfig.ofDefaults();
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
    public void shouldReturnOnErrorUsingObservable() {
        //Given
        RetryConfig config = RetryConfig.ofDefaults();
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
    public void shouldNotRetryFromPredicateUsingObservable() {
        //Given
        RetryConfig config = RetryConfig.custom()
                .retryOnException(t -> t instanceof IOException)
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
    public void shouldReturnOnCompleteUsingFlowable() {
        //Given
        RetryConfig config = RetryConfig.ofDefaults();
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
    public void shouldReturnOnErrorUsingFlowable() {
        //Given
        RetryConfig config = RetryConfig.ofDefaults();
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
    public void shouldNotRetryFromPredicateUsingFlowable() {
        //Given
        RetryConfig config = RetryConfig.custom()
                .retryOnException(t -> t instanceof IOException)
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

}
