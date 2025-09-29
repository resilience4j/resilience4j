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

import com.jayway.awaitility.Awaitility;
import io.github.resilience4j.test.HelloWorldService;
import io.vavr.CheckedRunnable;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class ThreadPoolBulkheadTest {

    private HelloWorldService helloWorldService;
    private ThreadPoolBulkheadConfig config;

    @Before
    public void setUp() {
        Awaitility.reset();
        helloWorldService = mock(HelloWorldService.class);
        config = ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(1)
            .coreThreadPoolSize(1)
            .queueCapacity(1)
            .build();
    }

    @Test
    public void shouldExecuteRunnableAndFailWithBulkHeadFull() throws InterruptedException {
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("test", config);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        final AtomicReference<Exception> exception = new AtomicReference<>();

        Thread first = new Thread(() -> {
            try {
                bulkhead.executeRunnable(() -> Try.run(() -> Thread.sleep(200)));
            } catch (Exception e) {
                exception.set(e);
            }

        });
        first.start();

        Thread second = new Thread(() -> {
            try {
                bulkhead.executeRunnable(helloWorldService::returnHelloWorld);
            } catch (Exception e) {
                exception.set(e);
            }
        });
        second.start();

        Thread third = new Thread(() -> {
            try {
                bulkhead.executeRunnable(helloWorldService::returnHelloWorld);
            } catch (Exception e) {
                exception.set(e);
            }
        });
        third.start();

        first.join(100);
        second.join(100);
        third.join(100);

        final Exception caughtException = exception.get();

        assertThat(caughtException).isInstanceOf(BulkheadFullException.class);
        assertThat(((BulkheadFullException) caughtException).getBulkheadName()).isEqualTo(bulkhead.getName());
    }

    @Test
    public void shouldExecuteSupplierAndFailWithBulkHeadFull() throws InterruptedException {
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("test", config);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        final AtomicReference<Exception> exception = new AtomicReference<>();

        Thread first = new Thread(() -> {
            try {
                bulkhead.executeSupplier(() -> Try.run(() -> Thread.sleep(200)));
            } catch (Exception e) {
                exception.set(e);
            }

        });
        first.start();

        Thread second = new Thread(() -> {
            try {
                bulkhead.executeSupplier(helloWorldService::returnHelloWorld);
            } catch (Exception e) {
                exception.set(e);
            }
        });
        second.start();

        Thread third = new Thread(() -> {
            try {
                bulkhead.executeSupplier(helloWorldService::returnHelloWorld);
            } catch (Exception e) {
                exception.set(e);
            }
        });
        third.start();

        first.join(100);
        second.join(100);
        third.join(100);

        final Exception caughtException = exception.get();

        assertThat(caughtException).isInstanceOf(BulkheadFullException.class);
        assertThat(((BulkheadFullException) caughtException).getBulkheadName()).isEqualTo(bulkhead.getName());
    }

    @Test
    public void shouldExecuteCallableAndFailWithBulkHeadFull() throws InterruptedException {
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("test", config);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        final AtomicReference<Exception> exception = new AtomicReference<>();

        Thread first = new Thread(() -> {
            try {
                bulkhead.executeCallable(() -> Try.run(() -> Thread.sleep(200)));
            } catch (Exception e) {
                exception.set(e);
            }

        });
        first.start();

        Thread second = new Thread(() -> {
            try {
                bulkhead.executeCallable(helloWorldService::returnHelloWorld);
            } catch (Exception e) {
                exception.set(e);
            }
        });
        second.start();

        Thread third = new Thread(() -> {
            try {
                bulkhead.executeCallable(helloWorldService::returnHelloWorld);
            } catch (Exception e) {
                exception.set(e);
            }
        });
        third.start();

        first.join(100);
        second.join(100);
        third.join(100);

        final Exception caughtException = exception.get();

        assertThat(caughtException).isInstanceOf(BulkheadFullException.class);
        assertThat(((BulkheadFullException) caughtException).getBulkheadName()).isEqualTo(bulkhead.getName());
    }


    @Test
    public void shouldExecuteSupplierAndReturnWithSuccess()
        throws ExecutionException, InterruptedException {
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("test", config);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        CompletionStage<String> result = bulkhead
            .executeSupplier(helloWorldService::returnHelloWorld);

        assertThat(result.toCompletableFuture().get()).isEqualTo("Hello world");
        then(helloWorldService).should(times(1)).returnHelloWorld();
    }

    @Test
    public void testCreateWithNullConfig() {
        assertThatThrownBy(() -> ThreadPoolBulkhead.of("test", (ThreadPoolBulkheadConfig) null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Config must not be null");
    }

    @Test
    public void testCreateThreadsUsingNameForPrefix()
        throws ExecutionException, InterruptedException {
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("TEST", config);
        Supplier<String> getThreadName = () -> Thread.currentThread().getName();

        CompletionStage<String> result = bulkhead.executeSupplier(getThreadName);

        assertThat(result.toCompletableFuture().get()).isEqualTo("bulkhead-TEST-1");
    }

    @Test
    public void testWithSynchronousQueue() {
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead
            .of("test", ThreadPoolBulkheadConfig.custom()
                .maxThreadPoolSize(2)
                .coreThreadPoolSize(1)
                .queueCapacity(0)
                .build());
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        CountDownLatch latch = new CountDownLatch(1);

        bulkhead.executeRunnable(CheckedRunnable.of(latch::await).unchecked());
        bulkhead.executeRunnable(CheckedRunnable.of(latch::await).unchecked());

        assertThatThrownBy(() -> bulkhead.executeCallable(helloWorldService::returnHelloWorld))
            .isInstanceOf(BulkheadFullException.class)
            .hasFieldOrPropertyWithValue("bulkheadName", bulkhead.getName());

        assertThat(bulkhead.getMetrics().getQueueDepth()).isZero();
        assertThat(bulkhead.getMetrics().getRemainingQueueCapacity()).isZero();
        assertThat(bulkhead.getMetrics().getQueueCapacity()).isZero();
        assertThat(bulkhead.getMetrics().getActiveThreadCount()).isEqualTo(2);
        assertThat(bulkhead.getMetrics().getThreadPoolSize()).isEqualTo(2);

        latch.countDown();
    }
}
