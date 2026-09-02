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
package io.github.resilience4j.reactor.bulkhead.operator;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests the non-blocking queueing behavior of the {@link BulkheadOperator} with a real
 * semaphore based Bulkhead: subscriptions to a full Bulkhead with a max wait duration are
 * queued without blocking the subscribing thread and are granted in FIFO order as permissions
 * are released.
 */
class BulkheadOperatorQueueingTest {

    private Bulkhead bulkhead(int maxConcurrentCalls, Duration maxWaitDuration) {
        return Bulkhead.of("test", BulkheadConfig.custom()
            .maxConcurrentCalls(maxConcurrentCalls)
            .maxWaitDuration(maxWaitDuration)
            .build());
    }

    @Test
    @Timeout(5)
    void shouldNotBlockSubscribingThreadWhenBulkheadIsFull() {
        Bulkhead bulkhead = bulkhead(1, Duration.ofMinutes(10));
        assertThat(bulkhead.tryAcquirePermission()).isTrue();
        AtomicReference<String> result = new AtomicReference<>();

        Disposable subscription = Mono.just("Event")
            .transformDeferred(BulkheadOperator.of(bulkhead))
            .subscribe(result::set);

        assertThat(result.get())
            .as("subscription should be queued instead of being rejected or blocking")
            .isNull();

        bulkhead.onComplete();

        assertThat(result.get()).isEqualTo("Event");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        subscription.dispose();
    }

    @Test
    @Timeout(5)
    void shouldGrantQueuedSubscriptionsInFifoOrder() {
        Bulkhead bulkhead = bulkhead(1, Duration.ofSeconds(10));
        assertThat(bulkhead.tryAcquirePermission()).isTrue();
        List<String> results = new CopyOnWriteArrayList<>();

        Mono.just("first").transformDeferred(BulkheadOperator.of(bulkhead))
            .subscribe(results::add);
        Mono.just("second").transformDeferred(BulkheadOperator.of(bulkhead))
            .subscribe(results::add);

        assertThat(results).isEmpty();

        bulkhead.onComplete();

        assertThat(results)
            .as("releasing one permission should cascade through the queued subscriptions")
            .containsExactly("first", "second");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    void shouldEmitBulkheadFullExceptionAfterMaxWaitDuration() {
        Bulkhead bulkhead = bulkhead(1, Duration.ofMillis(100));
        assertThat(bulkhead.tryAcquirePermission()).isTrue();

        StepVerifier.create(
            Mono.just("Event")
                .transformDeferred(BulkheadOperator.of(bulkhead)))
            .expectSubscription()
            .expectError(BulkheadFullException.class)
            .verify(Duration.ofSeconds(5));

        bulkhead.onComplete();
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    @Timeout(5)
    void shouldNotConsumePermitWhenWaitingSubscriberIsCancelled() {
        Bulkhead bulkhead = bulkhead(1, Duration.ofSeconds(10));
        assertThat(bulkhead.tryAcquirePermission()).isTrue();

        Disposable waiting = Mono.just("never")
            .transformDeferred(BulkheadOperator.of(bulkhead))
            .subscribe();
        waiting.dispose();

        bulkhead.onComplete();

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls())
            .as("cancelled waiting subscription must not consume the released permit")
            .isEqualTo(1);

        StepVerifier.create(
            Mono.just("after")
                .transformDeferred(BulkheadOperator.of(bulkhead)))
            .expectNext("after")
            .verifyComplete();
    }

    @Test
    @Timeout(5)
    void shouldReleasePermissionWhenSubscriptionIsCancelledAfterQueuedGrant() {
        Bulkhead bulkhead = bulkhead(1, Duration.ofSeconds(10));
        assertThat(bulkhead.tryAcquirePermission()).isTrue();

        Disposable waiting = Mono.never()
            .transformDeferred(BulkheadOperator.of(bulkhead))
            .subscribe();

        bulkhead.onComplete();
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls())
            .as("queued subscription should hold the released permit")
            .isZero();

        waiting.dispose();
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    @Timeout(5)
    void shouldReleasePermissionWhenUpstreamErrorsAfterQueuedGrant() {
        Bulkhead bulkhead = bulkhead(1, Duration.ofSeconds(10));
        assertThat(bulkhead.tryAcquirePermission()).isTrue();
        AtomicReference<Throwable> error = new AtomicReference<>();

        Mono.error(new IOException("BAM!"))
            .transformDeferred(BulkheadOperator.of(bulkhead))
            .subscribe(value -> {
            }, error::set);

        bulkhead.onComplete();

        assertThat(error.get()).isInstanceOf(IOException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    @Timeout(5)
    void shouldDeliverAllElementsOfQueuedFlux() {
        Bulkhead bulkhead = bulkhead(1, Duration.ofSeconds(10));
        assertThat(bulkhead.tryAcquirePermission()).isTrue();
        List<String> results = new CopyOnWriteArrayList<>();

        Flux.just("Event 1", "Event 2")
            .transformDeferred(BulkheadOperator.of(bulkhead))
            .subscribe(results::add);

        assertThat(results).isEmpty();

        bulkhead.onComplete();

        assertThat(results).containsExactly("Event 1", "Event 2");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    void shouldHonourRequestsMadeBeforeAndAfterQueuedGrant() {
        Bulkhead bulkhead = bulkhead(1, Duration.ofSeconds(10));
        assertThat(bulkhead.tryAcquirePermission()).isTrue();

        StepVerifier.create(
            Flux.just("Event 1", "Event 2", "Event 3")
                .transformDeferred(BulkheadOperator.of(bulkhead)), 0)
            .expectSubscription()
            .thenRequest(1)
            .then(bulkhead::onComplete)
            .expectNext("Event 1")
            .thenRequest(2)
            .expectNext("Event 2", "Event 3")
            .expectComplete()
            .verify(Duration.ofSeconds(5));

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    @Timeout(5)
    void shouldReleasePermissionWithoutSubscribingWhenGrantRacesCancellation() {
        Bulkhead bulkhead = mock(Bulkhead.class);
        // Models a permission which is granted concurrently with the cancellation: cancelling
        // the request loses the race and the granted permission has to be released.
        CompletableFuture<Void> permission = new CompletableFuture<>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }
        };
        given(bulkhead.acquirePermissionAsync()).willReturn(permission);
        AtomicBoolean subscribed = new AtomicBoolean();

        Disposable waiting = Mono.just("Event")
            .doOnSubscribe(subscription -> subscribed.set(true))
            .transformDeferred(BulkheadOperator.of(bulkhead))
            .subscribe();
        waiting.dispose();
        permission.complete(null);

        assertThat(subscribed)
            .as("upstream must not be subscribed for a cancelled subscriber")
            .isFalse();
        verify(bulkhead).releasePermission();
        verify(bulkhead, never()).onComplete();
    }

    @Test
    @Timeout(5)
    void shouldUnwrapCompletionExceptionOfQueuedPermission() {
        Bulkhead bulkhead = mock(Bulkhead.class);
        CompletableFuture<Void> permission = new CompletableFuture<>();
        given(bulkhead.acquirePermissionAsync()).willReturn(permission);
        IOException cause = new IOException("BAM!");

        StepVerifier.create(
            Mono.just("Event")
                .transformDeferred(BulkheadOperator.of(bulkhead)))
            .expectSubscription()
            .then(() -> permission.completeExceptionally(new CompletionException(cause)))
            .expectErrorSatisfies(error -> assertThat(error).isSameAs(cause))
            .verify(Duration.ofSeconds(5));
    }

    @Test
    @Timeout(5)
    void shouldPropagateContextAcrossQueuedGrant() {
        Bulkhead bulkhead = bulkhead(1, Duration.ofSeconds(10));
        assertThat(bulkhead.tryAcquirePermission()).isTrue();
        AtomicReference<String> result = new AtomicReference<>();

        Mono.deferContextual(context -> Mono.just(context.get("key").toString()))
            .transformDeferred(BulkheadOperator.of(bulkhead))
            .contextWrite(Context.of("key", "value"))
            .subscribe(result::set);

        assertThat(result.get()).isNull();

        bulkhead.onComplete();

        assertThat(result.get()).isEqualTo("value");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }
}
