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
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.junit.Test;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class RetryTransformerTest {

    @Test
    public void shouldReturnOnCompleteUsingSingle() {
        //Given
        RetryConfig config = RetryConfig.ofDefaults();
        Retry retry = Retry.of("testName", config);

        Single.just(1)
                .compose(RetryTransformer.of(retry))
                .test()
                .assertValueCount(1)
                .assertValues(1)
                .assertComplete();

        Single.just(1)
                .compose(RetryTransformer.of(retry))
                .test()
                .assertValueCount(1)
                .assertValues(1)
                .assertComplete();

        //Then
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt()).isEqualTo(2);
    }

    @Test
    public void shouldReturnOnErrorUsingSingle() {
        //Given
        RetryConfig config = RetryConfig.ofDefaults();
        Retry retry = Retry.of("testName", config);

        Single.fromCallable(() -> {throw new IOException("BAM!");})
                .compose(RetryTransformer.of(retry))
                .test()
                .assertError(IOException.class)
                .assertNotComplete()
                .assertSubscribed();

        Single.fromCallable(() -> {throw new IOException("BAM!");})
                .compose(RetryTransformer.of(retry))
                .test()
                .assertError(IOException.class)
                .assertNotComplete()
                .assertSubscribed();
        //Then
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(2);
    }

    @Test
    public void shouldNotRetryFromPredicateUsingSingle() {
        //Given
        RetryConfig config = RetryConfig.custom().retryOnException(t -> t instanceof IOException).maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);

        Single.fromCallable(() -> {throw new NoSuchElementException("BAM!");})
                .compose(RetryTransformer.of(retry))
                .test()
                .assertError(NoSuchElementException.class)
                .assertNotComplete()
                .assertSubscribed();
        //Then
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(1);
    }

    /*
    @Test
    public void shouldReturnOnErrorAfterRetryFailureUsingSingle() {
        //Given
        RetryConfig config = RetryConfig.ofDefaults();
        Retry retry = Retry.of("testName", config);

        for (int i = 0; i < 3; i++) {
            try {
                retry.onError(new IOException("BAM!"));
            } catch (Throwable t) {
            }
        }

        Single.fromCallable(() -> {throw new IOException("BAM!");})
                .compose(RetryTransformer.of(retry))
                .test()
                .assertError(IOException.class)
                .assertNotComplete()
                .assertSubscribed();

        //Then
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumAttempts()).isEqualTo(4);
        assertThat(metrics.getMaxAttempts()).isEqualTo(config.getMaxAttempts());
    }
    */

    @Test
    public void shouldReturnOnCompleteAfterRetryFailureUsingSingle() {
        //Given
        RetryConfig config = RetryConfig.ofDefaults();
        Retry retry = Retry.of("testName", config);
        AtomicInteger count = new AtomicInteger(0);

        Callable<Integer> c = () -> {
            if (count.get() == 0) {
                count.incrementAndGet();
                throw new IOException("BAM!");
            } else {
                return count.get();
            }
        };

        Single.fromCallable(c)
                .compose(RetryTransformer.of(retry))
                .test()
                .assertValueCount(1)
                .assertValues(1)
                .assertComplete();

        //Then
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
    }

    @Test
    public void shouldReturnOnCompleteUsingObservable() {
        //Given
        RetryConfig config = RetryConfig.ofDefaults();
        Retry retry = Retry.of("testName", config);

        Observable.just(1)
                .compose(RetryTransformer.of(retry))
                .test()
                .assertValueCount(1)
                .assertValues(1)
                .assertComplete();

        //Then
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt()).isEqualTo(1);
    }

    @Test
    public void shouldReturnOnErrorUsingObservable() {
        //Given
        RetryConfig config = RetryConfig.ofDefaults();
        Retry retry = Retry.of("testName", config);

        Observable.fromCallable(() -> {throw new IOException("BAM!");})
                .compose(RetryTransformer.of(retry))
                .test()
                .assertError(IOException.class)
                .assertNotComplete()
                .assertSubscribed();
        //Then
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(1);
    }

    @Test
    public void shouldNotRetryFromPredicateUsingObservable() {
        //Given
        RetryConfig config = RetryConfig.custom().retryOnException(t -> t instanceof IOException).maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);

        Observable.fromCallable(() -> {throw new NoSuchElementException("BAM!");})
                .compose(RetryTransformer.of(retry))
                .test()
                .assertError(NoSuchElementException.class)
                .assertNotComplete()
                .assertSubscribed();
        //Then
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(1);
    }

    /*
    @Test
    public void shouldReturnOnErrorAfterRetryFailureUsingObservable() {
        //Given
        RetryConfig config = RetryConfig.ofDefaults();
        Retry retry = Retry.of("testName", config);

        for (int i = 0; i < 3; i++) {
            try {
                retry.onError(new IOException("BAM!"));
            } catch (Throwable t) {
            }
        }

        Observable.fromCallable(() -> {throw new IOException("BAM!");})
                .compose(RetryTransformer.of(retry))
                .test()
                .assertError(IOException.class)
                .assertNotComplete()
                .assertSubscribed();

        //Then
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumAttempts()).isEqualTo(4);
        assertThat(metrics.getMaxAttempts()).isEqualTo(config.getMaxAttempts());
    }
    */

    @Test
    public void shouldReturnOnCompleteAfterRetryFailureUsingObservable() {
        //Given
        RetryConfig config = RetryConfig.ofDefaults();
        Retry retry = Retry.of("testName", config);
        AtomicInteger count = new AtomicInteger(0);

        Callable<Integer> c = () -> {
            if (count.get() == 0) {
                count.incrementAndGet();
                throw new IOException("BAM!");
            } else {
                return count.get();
            }
        };

        Observable.fromCallable(c)
                .compose(RetryTransformer.of(retry))
                .test()
                .assertValueCount(1)
                .assertValues(1)
                .assertComplete();

        //Then
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
    }

    @Test
    public void shouldReturnOnCompleteUsingFlowable() {
        //Given
        RetryConfig config = RetryConfig.ofDefaults();
        Retry retry = Retry.of("testName", config);

        Flowable.just(1)
                .compose(RetryTransformer.of(retry))
                .test()
                .assertValueCount(1)
                .assertValues(1)
                .assertComplete();

        //Then
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt()).isEqualTo(1);
    }

    @Test
    public void shouldReturnOnErrorUsingFlowable() {
        //Given
        RetryConfig config = RetryConfig.ofDefaults();
        Retry retry = Retry.of("testName", config);

        Flowable.fromCallable(() -> {throw new IOException("BAM!");})
                .compose(RetryTransformer.of(retry))
                .test()
                .assertError(IOException.class)
                .assertNotComplete()
                .assertSubscribed();
        //Then
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(1);
    }

    @Test
    public void shouldNotRetryFromPredicateUsingFlowable() {
        //Given
        RetryConfig config = RetryConfig.custom().retryOnException(t -> t instanceof IOException).maxAttempts(3).build();
        Retry retry = Retry.of("testName", config);

        Flowable.fromCallable(() -> {throw new NoSuchElementException("BAM!");})
                .compose(RetryTransformer.of(retry))
                .test()
                .assertError(NoSuchElementException.class)
                .assertNotComplete()
                .assertSubscribed();
        //Then
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(1);
    }

    /*
    @Test
    public void shouldReturnOnErrorAfterRetryFailureUsingFlowable() {
        //Given
        RetryConfig config = RetryConfig.ofDefaults();
        Retry retry = Retry.of("testName", config);

        for (int i = 0; i < 3; i++) {
            try {
                retry.onError(new IOException("BAM!"));
            } catch (Throwable t) {
            }
        }

        Flowable.fromCallable(() -> {throw new IOException("BAM!");})
                .compose(RetryTransformer.of(retry))
                .test()
                .assertError(IOException.class)
                .assertNotComplete()
                .assertSubscribed();

        //Then
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumAttempts()).isEqualTo(4);
        assertThat(metrics.getMaxAttempts()).isEqualTo(config.getMaxAttempts());
    }
    */

    @Test
    public void shouldReturnOnCompleteAfterRetryFailureUsingFlowable() {
        //Given
        RetryConfig config = RetryConfig.ofDefaults();
        Retry retry = Retry.of("testName", config);
        AtomicInteger count = new AtomicInteger(0);

        Callable<Integer> c = () -> {
            if (count.get() == 0) {
                count.incrementAndGet();
                throw new IOException("BAM!");
            } else {
                return count.get();
            }
        };

        Flowable.fromCallable(c)
                .compose(RetryTransformer.of(retry))
                .test()
                .assertValueCount(1)
                .assertValues(1)
                .assertComplete();

        //Then
        Retry.Metrics metrics = retry.getMetrics();

        assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
    }


}
