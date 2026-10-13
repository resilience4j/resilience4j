/*
 * Copyright 2026 Oleksandr Shevchenko
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
package io.github.resilience4j.rxjava3.bulkhead.operator;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests the non-blocking queueing behavior of the {@link BulkheadOperator} with a real semaphore
 * based Bulkhead: subscriptions to a full Bulkhead with a max wait duration are queued without
 * blocking the subscribing thread and proceed once a permission is released.
 */
class BulkheadOperatorQueueingTest {

    private Bulkhead bulkhead(int maxConcurrentCalls, Duration maxWaitDuration) {
        return Bulkhead.of("test", BulkheadConfig.custom()
            .maxConcurrentCalls(maxConcurrentCalls)
            .maxWaitDuration(maxWaitDuration)
            .build());
    }

    private Bulkhead fullBulkhead(Duration maxWaitDuration) {
        Bulkhead bulkhead = bulkhead(1, maxWaitDuration);
        assertThat(bulkhead.tryAcquirePermission()).isTrue();
        return bulkhead;
    }

    private static int availablePermits(Bulkhead bulkhead) {
        return bulkhead.getMetrics().getAvailableConcurrentCalls();
    }

    /**
     * Captures the errors which RxJava routes to {@link RxJavaPlugins#onError(Throwable)}, i.e.
     * the ones which could not be delivered to a downstream.
     */
    private static List<Throwable> captureUndeliverableErrors() {
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        RxJavaPlugins.setErrorHandler(errors::add);
        return errors;
    }

    @AfterEach
    void resetErrorHandler() {
        RxJavaPlugins.setErrorHandler(null);
    }

    @Test
    @Timeout(5)
    void shouldQueueSingleUntilPermissionIsReleased() {
        Bulkhead bulkhead = fullBulkhead(Duration.ofSeconds(10));

        TestObserver<Integer> observer = Single.just(1)
            .compose(BulkheadOperator.<Integer>of(bulkhead))
            .test();

        observer.assertEmpty();
        bulkhead.onComplete();
        observer.assertResult(1);
        assertThat(availablePermits(bulkhead)).isEqualTo(1);
    }

    @Test
    @Timeout(5)
    void shouldQueueMaybeUntilPermissionIsReleased() {
        Bulkhead bulkhead = fullBulkhead(Duration.ofSeconds(10));

        TestObserver<Integer> observer = Maybe.just(1)
            .compose(BulkheadOperator.<Integer>of(bulkhead))
            .test();

        observer.assertEmpty();
        bulkhead.onComplete();
        observer.assertResult(1);
        assertThat(availablePermits(bulkhead)).isEqualTo(1);
    }

    @Test
    @Timeout(5)
    void shouldQueueCompletableUntilPermissionIsReleased() {
        Bulkhead bulkhead = fullBulkhead(Duration.ofSeconds(10));

        TestObserver<Void> observer = Completable.complete()
            .compose(BulkheadOperator.of(bulkhead))
            .test();

        observer.assertEmpty();
        bulkhead.onComplete();
        observer.assertResult();
        assertThat(availablePermits(bulkhead)).isEqualTo(1);
    }

    @Test
    @Timeout(5)
    void shouldQueueObservableUntilPermissionIsReleased() {
        Bulkhead bulkhead = fullBulkhead(Duration.ofSeconds(10));

        TestObserver<Integer> observer = Observable.just(1, 2)
            .compose(BulkheadOperator.<Integer>of(bulkhead))
            .test();

        observer.assertEmpty();
        bulkhead.onComplete();
        observer.assertResult(1, 2);
        assertThat(availablePermits(bulkhead)).isEqualTo(1);
    }

    @Test
    @Timeout(5)
    void shouldQueueFlowableAndHonourRequestsMadeBeforeAndAfterGrant() {
        Bulkhead bulkhead = fullBulkhead(Duration.ofSeconds(10));

        TestSubscriber<Integer> subscriber = Flowable.just(1, 2, 3)
            .compose(BulkheadOperator.<Integer>of(bulkhead))
            .test(0);
        subscriber.request(1);

        subscriber.assertEmpty();
        bulkhead.onComplete();
        subscriber.assertValues(1).assertNotComplete();
        subscriber.request(2);
        subscriber.assertResult(1, 2, 3);
        assertThat(availablePermits(bulkhead)).isEqualTo(1);
    }

    @Test
    @Timeout(5)
    void shouldGrantQueuedSubscriptionsInFifoOrder() {
        Bulkhead bulkhead = fullBulkhead(Duration.ofSeconds(10));
        List<String> results = new CopyOnWriteArrayList<>();

        Single.just("first").compose(BulkheadOperator.<String>of(bulkhead))
            .subscribe(value -> results.add(value));
        Single.just("second").compose(BulkheadOperator.<String>of(bulkhead))
            .subscribe(value -> results.add(value));
        assertThat(results).isEmpty();

        bulkhead.onComplete();

        assertThat(results)
            .as("releasing one permission should cascade through the queued subscriptions")
            .containsExactly("first", "second");
        assertThat(availablePermits(bulkhead)).isEqualTo(1);
    }

    @Test
    void shouldEmitBulkheadFullExceptionAfterMaxWaitDuration() {
        Bulkhead bulkhead = fullBulkhead(Duration.ofMillis(100));

        Single.just(1)
            .compose(BulkheadOperator.<Integer>of(bulkhead))
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertError(BulkheadFullException.class);

        bulkhead.onComplete();
        assertThat(availablePermits(bulkhead)).isEqualTo(1);
    }

    @Test
    @Timeout(5)
    void shouldNotConsumePermitWhenWaitingObserverIsDisposed() {
        Bulkhead bulkhead = fullBulkhead(Duration.ofSeconds(10));
        TestObserver<Integer> waiting = Single.just(1)
            .compose(BulkheadOperator.<Integer>of(bulkhead))
            .test();

        waiting.dispose();
        bulkhead.onComplete();

        assertThat(availablePermits(bulkhead))
            .as("a disposed waiting subscription must not consume the released permit")
            .isEqualTo(1);
        Single.just(2).compose(BulkheadOperator.<Integer>of(bulkhead)).test().assertResult(2);
    }

    @Test
    @Timeout(5)
    void shouldReleasePermissionWhenDisposedAfterQueuedGrant() {
        Bulkhead bulkhead = fullBulkhead(Duration.ofSeconds(10));
        TestObserver<Integer> waiting = Single.<Integer>never()
            .compose(BulkheadOperator.<Integer>of(bulkhead))
            .test();

        bulkhead.onComplete();
        assertThat(availablePermits(bulkhead))
            .as("the queued subscription should hold the released permit")
            .isZero();

        waiting.dispose();
        assertThat(availablePermits(bulkhead)).isEqualTo(1);
    }

    @Test
    @Timeout(5)
    void shouldReleasePermissionWhenUpstreamErrorsAfterQueuedGrant() {
        Bulkhead bulkhead = fullBulkhead(Duration.ofSeconds(10));
        TestObserver<Integer> observer = Single.<Integer>error(new IOException("BAM!"))
            .compose(BulkheadOperator.<Integer>of(bulkhead))
            .test();

        bulkhead.onComplete();

        observer.assertError(IOException.class);
        assertThat(availablePermits(bulkhead)).isEqualTo(1);
    }

    @Test
    @Timeout(5)
    void shouldNotConsumePermitWhenWaitingSubscriberIsCancelled() {
        Bulkhead bulkhead = fullBulkhead(Duration.ofSeconds(10));
        TestSubscriber<Integer> waiting = Flowable.just(1)
            .compose(BulkheadOperator.<Integer>of(bulkhead))
            .test();

        waiting.cancel();
        bulkhead.onComplete();

        assertThat(availablePermits(bulkhead))
            .as("a cancelled waiting subscription must not consume the released permit")
            .isEqualTo(1);
        Flowable.just(2).compose(BulkheadOperator.<Integer>of(bulkhead)).test().assertResult(2);
    }

    @Test
    @Timeout(5)
    void shouldReleasePermissionWhenSubscriberIsCancelledAfterQueuedGrant() {
        Bulkhead bulkhead = fullBulkhead(Duration.ofSeconds(10));
        TestSubscriber<Integer> waiting = Flowable.<Integer>never()
            .compose(BulkheadOperator.<Integer>of(bulkhead))
            .test();

        bulkhead.onComplete();
        assertThat(availablePermits(bulkhead))
            .as("the queued subscription should hold the released permit")
            .isZero();

        waiting.cancel();
        assertThat(availablePermits(bulkhead)).isEqualTo(1);
    }

    @Test
    @Timeout(5)
    void shouldReleasePermissionWithoutSubscribingWhenGrantRacesSubscriberCancel() {
        Bulkhead bulkhead = mock(Bulkhead.class);
        // Models a permission which is granted concurrently with the cancellation: cancelling the
        // request loses the race and the granted permission has to be released.
        CompletableFuture<Void> permission = new CompletableFuture<>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }
        };
        given(bulkhead.acquirePermissionAsync()).willReturn(permission);
        AtomicBoolean subscribed = new AtomicBoolean();

        TestSubscriber<Integer> subscriber = Flowable.just(1)
            .doOnSubscribe(subscription -> subscribed.set(true))
            .compose(BulkheadOperator.<Integer>of(bulkhead))
            .test();
        subscriber.cancel();
        permission.complete(null);

        assertThat(subscribed)
            .as("upstream must not be subscribed for a cancelled subscriber")
            .isFalse();
        verify(bulkhead).releasePermission();
        verify(bulkhead, never()).onComplete();
    }

    @Test
    @Timeout(5)
    void shouldReleasePermissionWithoutSubscribingWhenGrantRacesDispose() {
        Bulkhead bulkhead = mock(Bulkhead.class);
        // Models a permission which is granted concurrently with the dispose: cancelling the
        // request loses the race and the granted permission has to be released.
        CompletableFuture<Void> permission = new CompletableFuture<>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }
        };
        given(bulkhead.acquirePermissionAsync()).willReturn(permission);
        AtomicBoolean subscribed = new AtomicBoolean();

        TestObserver<Integer> observer = Single.just(1)
            .doOnSubscribe(disposable -> subscribed.set(true))
            .compose(BulkheadOperator.<Integer>of(bulkhead))
            .test();
        observer.dispose();
        permission.complete(null);

        assertThat(subscribed)
            .as("upstream must not be subscribed for a disposed observer")
            .isFalse();
        verify(bulkhead).releasePermission();
        verify(bulkhead, never()).onComplete();
    }

    @Test
    @Timeout(5)
    void shouldReleasePermissionAndSignalErrorWhenQueuedUpstreamSubscribeThrows() {
        Bulkhead bulkhead = fullBulkhead(Duration.ofSeconds(10));
        captureUndeliverableErrors();
        RuntimeException failure = new RuntimeException("BAM!");
        TestObserver<Integer> observer = Single.<Integer>unsafeCreate(downstream -> {
                throw failure;
            })
            .compose(BulkheadOperator.<Integer>of(bulkhead))
            .test();
        observer.assertEmpty();

        bulkhead.onComplete();

        // RxJava rethrows an exception thrown by subscribeActual(...) wrapped in a NullPointerException
        observer.assertError(error -> error instanceof NullPointerException
            && error.getCause() == failure);
        assertThat(availablePermits(bulkhead))
            .as("the permission of a failed upstream subscription must be released")
            .isEqualTo(1);
    }

    @Test
    @Timeout(5)
    void shouldReleasePermissionOnceAndSignalErrorWhenQueuedUpstreamThrowsAfterOnSubscribe() {
        Bulkhead bulkhead = fullBulkhead(Duration.ofSeconds(10));
        captureUndeliverableErrors();
        RuntimeException failure = new RuntimeException("BAM!");
        TestObserver<Integer> observer = Single.<Integer>unsafeCreate(downstream -> {
                downstream.onSubscribe(Disposable.empty());
                throw failure;
            })
            .compose(BulkheadOperator.<Integer>of(bulkhead))
            .test();
        observer.assertEmpty();

        bulkhead.onComplete();

        observer.assertError(error -> error instanceof NullPointerException
            && error.getCause() == failure);
        assertThat(availablePermits(bulkhead))
            .as("the subscribed bulkhead observer releases the permission exactly once")
            .isEqualTo(1);
    }

    @Test
    @Timeout(5)
    void shouldReleasePermissionWithoutSignalWhenQueuedUpstreamSubscribeThrowsAfterDispose() {
        Bulkhead bulkhead = fullBulkhead(Duration.ofSeconds(10));
        List<Throwable> undeliverable = captureUndeliverableErrors();
        RuntimeException failure = new RuntimeException("BAM!");
        AtomicReference<TestObserver<Integer>> observerRef = new AtomicReference<>();
        TestObserver<Integer> observer = Single.<Integer>unsafeCreate(downstream -> {
                observerRef.get().dispose();
                throw failure;
            })
            .compose(BulkheadOperator.<Integer>of(bulkhead))
            .test();
        observerRef.set(observer);

        bulkhead.onComplete();

        observer.assertEmpty();
        assertThat(undeliverable)
            .as("an error which cannot be delivered to a disposed observer goes to RxJavaPlugins")
            .anySatisfy(error -> assertThat(error)
                .isInstanceOf(NullPointerException.class)
                .hasCause(failure));
        assertThat(availablePermits(bulkhead)).isEqualTo(1);
    }

    @Test
    @Timeout(5)
    void shouldReleasePermissionAndSignalErrorWhenQueuedUpstreamPublisherSubscribeThrows() {
        Bulkhead bulkhead = fullBulkhead(Duration.ofSeconds(10));
        captureUndeliverableErrors();
        RuntimeException failure = new RuntimeException("BAM!");
        TestSubscriber<Integer> subscriber = Flowable.<Integer>unsafeCreate(downstream -> {
                throw failure;
            })
            .compose(BulkheadOperator.<Integer>of(bulkhead))
            .test();
        subscriber.assertEmpty();

        bulkhead.onComplete();

        subscriber.assertError(error -> error instanceof NullPointerException
            && error.getCause() == failure);
        assertThat(availablePermits(bulkhead))
            .as("the permission of a failed upstream subscription must be released")
            .isEqualTo(1);
    }

    @Test
    @Timeout(5)
    void shouldReleasePermissionOnceAndSignalErrorWhenQueuedUpstreamPublisherThrowsAfterOnSubscribe() {
        Bulkhead bulkhead = fullBulkhead(Duration.ofSeconds(10));
        captureUndeliverableErrors();
        RuntimeException failure = new RuntimeException("BAM!");
        Subscription upstream = mock(Subscription.class);
        TestSubscriber<Integer> subscriber = Flowable.<Integer>unsafeCreate(downstream -> {
                downstream.onSubscribe(upstream);
                throw failure;
            })
            .compose(BulkheadOperator.<Integer>of(bulkhead))
            .test();
        subscriber.assertEmpty();

        bulkhead.onComplete();

        subscriber.assertError(error -> error instanceof NullPointerException
            && error.getCause() == failure);
        verify(upstream).cancel();
        assertThat(availablePermits(bulkhead))
            .as("the subscribed bulkhead subscriber releases the permission exactly once")
            .isEqualTo(1);
    }

    @Test
    @Timeout(5)
    void shouldUnwrapCompletionExceptionOfQueuedPermission() {
        Bulkhead bulkhead = mock(Bulkhead.class);
        CompletableFuture<Void> permission = new CompletableFuture<>();
        given(bulkhead.acquirePermissionAsync()).willReturn(permission);
        IOException cause = new IOException("BAM!");

        TestObserver<Integer> observer = Single.just(1)
            .compose(BulkheadOperator.<Integer>of(bulkhead))
            .test();
        permission.completeExceptionally(new CompletionException(cause));

        observer.assertError(error -> error == cause);
    }
}
