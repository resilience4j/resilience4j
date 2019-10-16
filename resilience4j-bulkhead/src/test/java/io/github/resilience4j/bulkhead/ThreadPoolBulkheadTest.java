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
import com.jayway.awaitility.Duration;
import io.github.resilience4j.test.HelloWorldService;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
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
    public void shouldExecuteSupplierAndFailWithBulkHeadFull() {
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("testSupplier", config);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        final Exception exception = new Exception();

        new Thread(() -> {
            try {
                final AtomicInteger counter = new AtomicInteger(0);
                bulkhead.executeRunnable(() -> {
                    Awaitility.waitAtMost(Duration.TWO_HUNDRED_MILLISECONDS).until(() -> counter.incrementAndGet() > 1);
                });
            } catch (Exception e) {
                exception.initCause(e);
            }
        }).start();
        new Thread(() -> {
            try {
                bulkhead.executeSupplier(helloWorldService::returnHelloWorld);
            } catch (Exception e) {
                exception.initCause(e);
            }
        }).start();
        new Thread(() -> {
            try {
                bulkhead.executeSupplier(helloWorldService::returnHelloWorld);
            } catch (Exception e) {
                exception.initCause(e);
            }
        }).start();

        final AtomicInteger counter = new AtomicInteger(0);
        Awaitility.waitAtMost(Duration.FIVE_HUNDRED_MILLISECONDS).until(() -> counter.incrementAndGet() >= 2);
        assertThat(exception.getCause().getMessage()).contains("Bulkhead 'testSupplier' is full and does not permit further calls");
    }


    @Test
    public void shouldExecuteCallableAndFailWithBulkHeadFull() throws InterruptedException {
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
        first.setDaemon(true);
        first.start();

        Thread second = new Thread(() -> {
            try {
                bulkhead.executeCallable(helloWorldService::returnHelloWorld);
            } catch (Exception e) {
                exception.set(e);
            }
        });
        second.setDaemon(true);
        second.start();

        Thread third = new Thread(() -> {
            try {
                bulkhead.executeCallable(helloWorldService::returnHelloWorld);
            } catch (Exception e) {
                exception.set(e);
            }
        });
        third.setDaemon(true);
        third.start();

        first.join(100);
        second.join(100);
        third.join(100);

        assertThat(exception.get()).isInstanceOf(BulkheadFullException.class);
    }


    @Test
    public void shouldExecuteSupplierAndReturnWithSuccess() throws ExecutionException, InterruptedException {
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("test", config);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        CompletionStage<String> result = bulkhead.executeSupplier(helloWorldService::returnHelloWorld);

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
    public void testCreateThreadsUsingNameForPrefix() throws ExecutionException, InterruptedException {
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("TEST", config);
        Supplier<String> getThreadName = () -> Thread.currentThread().getName();

        CompletionStage<String> result = bulkhead.executeSupplier(getThreadName);

        assertThat(result.toCompletableFuture().get()).isEqualTo("bulkhead-TEST-1");
    }

}
