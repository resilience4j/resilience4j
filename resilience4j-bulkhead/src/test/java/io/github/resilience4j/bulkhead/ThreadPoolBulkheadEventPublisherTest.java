/*
 *
 *  Copyright 2017 Robert Winkler, Lucas Lech, Mahmoud Romeh
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
package io.github.resilience4j.bulkhead;

import io.github.resilience4j.test.HelloWorldService;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class ThreadPoolBulkheadEventPublisherTest {

    private HelloWorldService helloWorldService;
    private ThreadPoolBulkheadConfig config;
    private Logger logger;
    private ThreadPoolBulkhead bulkhead;

    @Before
    public void setUp() {
        helloWorldService = mock(HelloWorldService.class);
        config = ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(1)
            .coreThreadPoolSize(1)
            .build();

        bulkhead = ThreadPoolBulkhead.of("test", config);

        logger = mock(Logger.class);
        Awaitility.reset();
    }

    @Test
    public void shouldReturnTheSameConsumer() {
        ThreadPoolBulkhead.ThreadPoolBulkheadEventPublisher eventPublisher = bulkhead
            .getEventPublisher();
        ThreadPoolBulkhead.ThreadPoolBulkheadEventPublisher eventPublisher2 = bulkhead
            .getEventPublisher();

        assertThat(eventPublisher).isEqualTo(eventPublisher2);
    }

    @Test
    public void shouldConsumeOnCallRejectedEvent() throws InterruptedException {
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead
            .of("test", ThreadPoolBulkheadConfig.custom()
                .maxThreadPoolSize(1)
                .coreThreadPoolSize(1)
                .queueCapacity(1)
                .build());
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        bulkhead.getEventPublisher().onCallRejected(
            event -> logger.info(event.getEventType().toString()));
        final Exception exception = new Exception();

        // Occupy the running slot with a task that blocks until we release it.
        // This is deterministic: the subsequent submissions happen only after the
        // blocker is confirmed to be executing, so the queue/rejection state is
        // predictable regardless of thread scheduling.
        CountDownLatch blockerEntered = new CountDownLatch(1);
        CountDownLatch blockerRelease = new CountDownLatch(1);
        bulkhead.executeRunnable(() -> {
            blockerEntered.countDown();
            try {
                blockerRelease.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        assertThat(blockerEntered.await(2, TimeUnit.SECONDS))
            .as("blocker task must be executing inside the bulkhead")
            .isTrue();

        // Fill the queue (capacity 1).
        bulkhead.executeCallable(helloWorldService::returnHelloWorld);

        // Third submission: pool is busy and queue is full, so this must be rejected.
        try {
            bulkhead.executeCallable(helloWorldService::returnHelloWorld);
        } catch (Exception e) {
            exception.initCause(e);
        }

        // Allow the blocker to complete so the executor can drain cleanly.
        blockerRelease.countDown();

        assertThat(exception).hasCauseInstanceOf(BulkheadFullException.class);
        then(logger).should(times(1)).info("CALL_REJECTED");
    }

    @Test
    public void shouldConsumeOnCallPermittedEvent()
        throws ExecutionException, InterruptedException {
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("test", config);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        bulkhead.getEventPublisher().onCallPermitted(
            event -> logger.info(event.getEventType().toString()));

        String result = bulkhead.executeSupplier(helloWorldService::returnHelloWorld)
            .toCompletableFuture().get();

        assertThat(result).isEqualTo("Hello world");
        then(logger).should(times(1)).info("CALL_PERMITTED");
    }

    @Test
    public void shouldConsumeOnCallFinishedEventWhenExecutionIsFinished() throws Exception {
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("test", config);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        bulkhead.getEventPublisher().onCallFinished(
            event -> logger.info(event.getEventType().toString()));

        bulkhead.executeSupplier(helloWorldService::returnHelloWorld).toCompletableFuture().get();

        then(logger).should(times(1)).info("CALL_FINISHED");
    }
}
