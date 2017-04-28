/*
 *
 *  Copyright 2016 Robert Winkler
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.bulkhead.operator;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import org.junit.Test;
import org.mockito.Mockito;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class BulkheadOperatorTest {

    @Test
    public void shouldReturnOnCompleteUsingSingle() {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", 1);

        Single.just(1)
                .lift(BulkheadOperator.of(bulkhead))
                .test()
                .assertValueCount(1)
                .assertValues(1)
                .assertComplete();

        // Then
        assertThat(bulkhead.getRemainingDepth()).isEqualTo(1);
    }

    @Test
    public void shouldReturnOnErrorUsingUsingSingle() {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", 1);

        Single.fromCallable(() -> {
            throw new IOException("BAM!");
        })
                .lift(BulkheadOperator.of(bulkhead))
                .test()
                .assertError(IOException.class)
                .assertNotComplete()
                .assertSubscribed();

        // Then
        assertThat(bulkhead.getRemainingDepth()).isEqualTo(1);
    }

    @Test
    public void shouldReturnOnCompleteUsingObservable() {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", 1);

        // When
        Observable.fromArray("Event 1", "Event 2")
                .lift(BulkheadOperator.of(bulkhead))
                .test()
                .assertValueCount(2)
                .assertValues("Event 1", "Event 2")
                .assertComplete();

        // Then
        assertThat(bulkhead.getRemainingDepth()).isEqualTo(1);
    }

    @Test
    public void shouldReturnOnCompleteUsingFlowable() {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", 1);

        // When
        Flowable.fromArray("Event 1", "Event 2")
                .lift(BulkheadOperator.of(bulkhead))
                .test()
                .assertValueCount(2)
                .assertValues("Event 1", "Event 2")
                .assertComplete();

        // Then
        assertThat(bulkhead.getRemainingDepth()).isEqualTo(1);
    }

    @Test
    public void shouldReturnOnErrorUsingObservable() {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", 1);

        // When
        Observable.fromCallable(() -> {
            throw new IOException("BAM!");
        })
                .lift(BulkheadOperator.of(bulkhead))
                .test()
                .assertError(IOException.class)
                .assertNotComplete()
                .assertSubscribed();

        // Then
        assertThat(bulkhead.getRemainingDepth()).isEqualTo(1);
    }

    @Test
    public void shouldReturnOnErrorUsingFlowable() {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", 1);

        // When
        Flowable.fromCallable(() -> {
            throw new IOException("BAM!");
        })
                .lift(BulkheadOperator.of(bulkhead))
                .test()
                .assertError(IOException.class)
                .assertNotComplete()
                .assertSubscribed();

        // Then
        assertThat(bulkhead.getRemainingDepth()).isEqualTo(1);
    }

    @Test
    public void shouldReturnOnErrorWithBulkheadFullExceptionUsingObservable() {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", 1);
        bulkhead.isCallPermitted();

        // When
        Observable.fromArray("Event 1", "Event 2")
                .lift(BulkheadOperator.of(bulkhead))
                .test()
                .assertError(BulkheadFullException.class)
                .assertNotComplete()
                .assertSubscribed();

        bulkhead.onComplete();

        // Then
        assertThat(bulkhead.getRemainingDepth()).isEqualTo(1);
    }

    @Test
    public void shouldReturnOnErrorWithBulkheadFullExceptionUsingFlowable() {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", 1);
        bulkhead.isCallPermitted();

        // When
        Flowable.fromArray("Event 1", "Event 2")
                .lift(BulkheadOperator.of(bulkhead))
                .test()
                .assertError(BulkheadFullException.class)
                .assertNotComplete()
                .assertSubscribed();

        bulkhead.onComplete();

        // Then
        assertThat(bulkhead.getRemainingDepth()).isEqualTo(1);
    }

    @Test
    public void shouldReturnOnErrorWithBulkheadFullExceptionUsingSingle() {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", 1);
        bulkhead.isCallPermitted();

        // When
        Single.just("foobar")
                .lift(BulkheadOperator.of(bulkhead))
                .test()
                .assertError(BulkheadFullException.class);

        bulkhead.onComplete();

        // Then
        assertThat(bulkhead.getRemainingDepth()).isEqualTo(1);
    }

    @Test
    public void testBulkheadObserverOnNext() throws Exception {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", 1);
        Disposable disposable = mock(Disposable.class);
        Observer childObserver = mock(Observer.class);
        Observer decoratedObserver = BulkheadOperator.of(bulkhead)
                .apply(childObserver);

        decoratedObserver.onSubscribe(disposable);

        // When
        decoratedObserver.onNext("one");
        ((Disposable) decoratedObserver).dispose();
        decoratedObserver.onNext("two");

        // Then
        verify(childObserver, times(1)).onNext(any());
        assertThat(bulkhead.getRemainingDepth()).isEqualTo(1);
    }

    @Test
    public void testBulkheadObserverOnError() throws Exception {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", 1);
        Disposable disposable = mock(Disposable.class);
        Observer childObserver = mock(Observer.class);
        Observer decoratedObserver = BulkheadOperator.of(bulkhead)
                .apply(childObserver);

        decoratedObserver.onSubscribe(disposable);

        // When
        ((Disposable) decoratedObserver).dispose();
        decoratedObserver.onError(new IllegalStateException());

        // Then
        verify(childObserver, times(0)).onError(any());
        assertThat(bulkhead.getRemainingDepth()).isEqualTo(1);
    }

    @Test
    public void testBulkheadObserverOnComplete() throws Exception {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", 1);
        Disposable disposable = mock(Disposable.class);
        Observer childObserver = mock(Observer.class);
        Observer decoratedObserver = BulkheadOperator.of(bulkhead)
                .apply(childObserver);

        decoratedObserver.onSubscribe(disposable);

        // When
        ((Disposable) decoratedObserver).dispose();
        decoratedObserver.onComplete();

        // Then
        verify(childObserver, times(0)).onComplete();
        assertThat(bulkhead.getRemainingDepth()).isEqualTo(1);
    }

    @Test
    public void testBulkheadSubscriberOnNext() throws Exception {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", 1);
        Subscription subscription = mock(Subscription.class);
        Subscriber childSubscriber = mock(Subscriber.class);
        Subscriber decoratedSubcriber = BulkheadOperator.of(bulkhead)
                .apply(childSubscriber);

        decoratedSubcriber.onSubscribe(subscription);

        // When
        decoratedSubcriber.onNext("one");
        ((Subscription) decoratedSubcriber).cancel();
        decoratedSubcriber.onNext("two");

        // Then
        verify(childSubscriber, times(1)).onNext(any());
        assertThat(bulkhead.getRemainingDepth()).isEqualTo(1);
    }

    @Test
    public void testBulkheadSubscriberOnError() throws Exception {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", 1);
        Subscription subscription = mock(Subscription.class);
        Subscriber childSubscriber = mock(Subscriber.class);
        Subscriber decoratedSubcriber = BulkheadOperator.of(bulkhead)
                .apply(childSubscriber);

        decoratedSubcriber.onSubscribe(subscription);

        // When
        ((Subscription) decoratedSubcriber).cancel();
        decoratedSubcriber.onError(new IllegalStateException());

        // Then
        verify(childSubscriber, times(0)).onError(any());
        assertThat(bulkhead.getRemainingDepth()).isEqualTo(1);
    }

    @Test
    public void testBulkheadSubscriberOnComplete() throws Exception {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", 1);
        Subscription subscription = mock(Subscription.class);
        Subscriber childSubscriber = mock(Subscriber.class);
        Subscriber decoratedSubcriber = BulkheadOperator.of(bulkhead)
                .apply(childSubscriber);

        decoratedSubcriber.onSubscribe(subscription);

        // When
        ((Subscription) decoratedSubcriber).cancel();
        decoratedSubcriber.onComplete();

        // Then
        verify(childSubscriber, times(0)).onComplete();
        assertThat(bulkhead.getRemainingDepth()).isEqualTo(1);
    }

    @Test
    public void testBulkheadSingleOnSuccess() throws Exception {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", 1);
        Disposable disposable = mock(Disposable.class);
        SingleObserver childObserver = mock(SingleObserver.class);
        SingleObserver decoratedObserver = BulkheadOperator.of(bulkhead)
                .apply(childObserver);

        decoratedObserver.onSubscribe(disposable);

        // When
        ((Disposable) decoratedObserver).dispose();
        decoratedObserver.onSuccess("two");

        // Then
        verify(childObserver, times(0)).onSuccess(any());
        assertThat(bulkhead.getRemainingDepth()).isEqualTo(1);
    }

    @Test
    public void testBulkheadSingleOnError() throws Exception {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", 1);
        Disposable disposable = mock(Disposable.class);
        SingleObserver childObserver = mock(SingleObserver.class);
        SingleObserver decoratedObserver = BulkheadOperator.of(bulkhead)
                .apply(childObserver);

        decoratedObserver.onSubscribe(disposable);

        // When
        ((Disposable) decoratedObserver).dispose();
        decoratedObserver.onError(new IllegalStateException());

        // Then
        verify(childObserver, times(0)).onError(any());
        assertThat(bulkhead.getRemainingDepth()).isEqualTo(1);
    }

}