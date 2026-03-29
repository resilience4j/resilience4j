/*
 *
 * Copyright 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package io.github.resilience4j.bulkhead;

import io.github.resilience4j.test.HelloWorldService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

class BulkheadFutureTest {

    private HelloWorldService helloWorldService;
    private Future<String> future;
    private BulkheadConfig config;

    @BeforeEach
    @SuppressWarnings("unchecked") 
    void setUp() {
        helloWorldService = mock(HelloWorldService.class);        
        future = mock(Future.class);
        config = BulkheadConfig.custom()
                .maxConcurrentCalls(1)
                .build();
    }

    @Test
    void shouldDecorateSupplierAndReturnWithSuccess() throws Exception {
        Bulkhead bulkhead = Bulkhead.of("test", config);

        given(future.get()).willReturn("Hello world");
        given(helloWorldService.returnHelloWorldFuture()).willReturn(future);

        Supplier<Future<String>> supplier = Bulkhead
                .decorateFuture(bulkhead, helloWorldService::returnHelloWorldFuture);

        String result = supplier.get().get();

        assertThat(result).isEqualTo("Hello world");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isOne();
        then(helloWorldService).should(times(1)).returnHelloWorldFuture();
        then(future).should(times(1)).get();
    }

    @Test
    void shouldDecorateSupplierAndReturnWithSuccessAndTimeout() throws Exception {
        Bulkhead bulkhead = Bulkhead.of("test", config);

        given(future.get(anyLong(), any(TimeUnit.class))).willReturn("Hello world");
        given(helloWorldService.returnHelloWorldFuture()).willReturn(future);

        Supplier<Future<String>> supplier = Bulkhead
                .decorateFuture(bulkhead, helloWorldService::returnHelloWorldFuture);

        String result = supplier.get().get(5, TimeUnit.SECONDS);

        assertThat(result).isEqualTo("Hello world");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isOne();
        then(helloWorldService).should(times(1)).returnHelloWorldFuture();
        then(future).should(times(1)).get(anyLong(), any(TimeUnit.class));
    }

    @Test
    void shouldDecorateFutureAndBulkheadApplyOnceOnMultipleFutureEval() throws Exception {
        Bulkhead bulkhead = Bulkhead.of("test", config);

        given(future.get(anyLong(), any(TimeUnit.class))).willReturn("Hello world");
        given(helloWorldService.returnHelloWorldFuture()).willReturn(future);

        Supplier<Future<String>> supplier = Bulkhead
                .decorateFuture(bulkhead, helloWorldService::returnHelloWorldFuture);

        Future<String> decoratedFuture = supplier.get();

        decoratedFuture.get(5, TimeUnit.SECONDS);
        decoratedFuture.get(5, TimeUnit.SECONDS);

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isOne();
        then(helloWorldService).should(times(1)).returnHelloWorldFuture();
        then(future).should(times(2)).get(anyLong(), any(TimeUnit.class));
    }

    @Test
    void shouldDecorateFutureAndBulkheadApplyOnceOnMultipleFutureEvalFailure() throws Exception {
        Bulkhead bulkhead = Bulkhead.of("test", config);

        given(future.get()).willThrow(new ExecutionException(new RuntimeException("Hello world")));
        given(helloWorldService.returnHelloWorldFuture()).willReturn(future);

        Supplier<Future<String>> supplier = Bulkhead
                .decorateFuture(bulkhead, helloWorldService::returnHelloWorldFuture);

        Future<String> decoratedFuture = supplier.get();

        catchThrowable(decoratedFuture::get);
        catchThrowable(decoratedFuture::get);

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isOne();
        then(helloWorldService).should(times(1)).returnHelloWorldFuture();
        then(future).should(times(2)).get();
    }

    @Test
    void shouldDecorateSupplierAndReturnWithExceptionAtAsyncStage() throws Exception {
        Bulkhead bulkhead = Bulkhead.of("test", config);

        given(future.get()).willThrow(new ExecutionException(new RuntimeException("BAM!")));
        given(helloWorldService.returnHelloWorldFuture()).willReturn(future);

        Supplier<Future<String>> supplier = Bulkhead
                .decorateFuture(bulkhead, helloWorldService::returnHelloWorldFuture);

        Throwable thrown = catchThrowable(() -> supplier.get().get());

        assertThat(thrown).isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RuntimeException.class);

        assertThat(thrown.getCause().getMessage()).isEqualTo("BAM!");

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isOne();
        then(helloWorldService).should(times(1)).returnHelloWorldFuture();
        then(future).should(times(1)).get();
    }

    @Test
    void shouldDecorateSupplierAndReturnWithExceptionAtSyncStage() {
        Bulkhead bulkhead = Bulkhead.of("test", config);

        given(helloWorldService.returnHelloWorldFuture()).willThrow(new RuntimeException("BAM!"));

        Supplier<Future<String>> supplier = Bulkhead
                .decorateFuture(bulkhead, helloWorldService::returnHelloWorldFuture);

        Throwable thrown = catchThrowable(() -> supplier.get().get());

        assertThat(thrown).isInstanceOf(RuntimeException.class)
                .hasMessage("BAM!");

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isOne();
        then(helloWorldService).should(times(1)).returnHelloWorldFuture();
        then(future).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturnFailureWithBulkheadFullException() throws Exception {
        // tag::bulkheadFullException[]
        BulkheadConfig config = BulkheadConfig.custom().maxConcurrentCalls(2).build();
        Bulkhead bulkhead = Bulkhead.of("test", config);
        bulkhead.tryAcquirePermission();
        bulkhead.tryAcquirePermission();
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isZero();

        given(future.get()).willReturn("Hello world");
        given(helloWorldService.returnHelloWorldFuture()).willReturn(future);

        Supplier<Future<String>> supplier = Bulkhead.decorateFuture(bulkhead, helloWorldService::returnHelloWorldFuture);

        Throwable thrown = catchThrowable(() -> supplier.get().get());

        assertThat(thrown).isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(BulkheadFullException.class);

        then(helloWorldService).shouldHaveNoInteractions();
        then(future).shouldHaveNoInteractions();
        // end::bulkheadFullException[]
    }

    @Test
    void shouldReturnFailureWithFutureCancellationException() throws Exception {
        Bulkhead bulkhead = Bulkhead.of("test", config);

        given(future.get()).willThrow(new CancellationException());
        given(helloWorldService.returnHelloWorldFuture()).willReturn(future);

        Supplier<Future<String>> supplier = Bulkhead
                .decorateFuture(bulkhead, helloWorldService::returnHelloWorldFuture);

        Throwable thrown = catchThrowable(() -> supplier.get().get());

        assertThat(thrown).isInstanceOf(CancellationException.class);

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isOne();
        then(helloWorldService).should(times(1)).returnHelloWorldFuture();
        then(future).should(times(1)).get();
    }

    @Test
    void shouldReturnFailureWithFutureTimeoutException() throws Exception {
        Bulkhead bulkhead = Bulkhead.of("test", config);

        given(future.get(anyLong(), any(TimeUnit.class))).willThrow(new TimeoutException());
        given(helloWorldService.returnHelloWorldFuture()).willReturn(future);

        Supplier<Future<String>> supplier = Bulkhead
                .decorateFuture(bulkhead, helloWorldService::returnHelloWorldFuture);

        Throwable thrown = catchThrowable(() -> supplier.get().get(5, TimeUnit.SECONDS));

        assertThat(thrown).isInstanceOf(TimeoutException.class);

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isOne();
        then(helloWorldService).should(times(1)).returnHelloWorldFuture();
        then(future).should(times(1)).get(anyLong(), any(TimeUnit.class));
    }
}
