/*
 *
 *  Copyright 2020: KrnSaurabh
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
package io.github.resilience4j.ratelimiter;

import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedRunnable;
import io.vavr.control.Either;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

public class VavrRateLimiterTest {

    private static final int LIMIT = 50;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REFRESH_PERIOD = Duration.ofNanos(500);

    private RateLimiterConfig config;
    private RateLimiter limit;

    @Before
    public void init() {
        config = RateLimiterConfig.custom()
            .timeoutDuration(TIMEOUT)
            .limitRefreshPeriod(REFRESH_PERIOD)
            .limitForPeriod(LIMIT)
            .build();
        limit = mock(RateLimiter.class);
        given(limit.getRateLimiterConfig()).willReturn(config);
    }

    @Test
    public void decorateCheckedSupplier() throws Throwable {
        CheckedFunction0 supplier = mock(CheckedFunction0.class);
        CheckedFunction0 decorated = VavrRateLimiter.decorateCheckedSupplier(limit, supplier);
        given(limit.acquirePermission(1)).willReturn(false);
        Try decoratedSupplierResult = Try.of(decorated);
        assertThat(decoratedSupplierResult.isFailure()).isTrue();
        assertThat(decoratedSupplierResult.getCause()).isInstanceOf(RequestNotPermitted.class);
        then(supplier).should(never()).apply();
        given(limit.acquirePermission(1)).willReturn(true);

        Try secondSupplierResult = Try.of(decorated);

        assertThat(secondSupplierResult.isSuccess()).isTrue();
        then(supplier).should().apply();
    }

    @Test
    public void decorateCheckedRunnable() throws Throwable {
        CheckedRunnable runnable = mock(CheckedRunnable.class);
        CheckedRunnable decorated = VavrRateLimiter.decorateCheckedRunnable(limit, runnable);
        given(limit.acquirePermission(1)).willReturn(false);
        Try decoratedRunnableResult = Try.run(decorated);
        assertThat(decoratedRunnableResult.isFailure()).isTrue();
        assertThat(decoratedRunnableResult.getCause()).isInstanceOf(RequestNotPermitted.class);
        then(runnable).should(never()).run();
        given(limit.acquirePermission(1)).willReturn(true);

        Try secondRunnableResult = Try.run(decorated);

        assertThat(secondRunnableResult.isSuccess()).isTrue();
        then(runnable).should().run();
    }

    @Test
    public void decorateCheckedFunction() throws Throwable {
        CheckedFunction1<Integer, String> function = mock(CheckedFunction1.class);
        CheckedFunction1<Integer, String> decorated = VavrRateLimiter
            .decorateCheckedFunction(limit, function);
        given(limit.acquirePermission(1)).willReturn(false);
        Try<String> decoratedFunctionResult = Try.success(1).mapTry(decorated);
        assertThat(decoratedFunctionResult.isFailure()).isTrue();
        assertThat(decoratedFunctionResult.getCause()).isInstanceOf(RequestNotPermitted.class);
        then(function).should(never()).apply(any());
        given(limit.acquirePermission(1)).willReturn(true);

        Try secondFunctionResult = Try.success(1).mapTry(decorated);

        assertThat(secondFunctionResult.isSuccess()).isTrue();
        then(function).should().apply(1);
    }

    @Test
    public void decorateTrySupplier() {
        Supplier<Try<String>> supplier = mock(Supplier.class);
        given(supplier.get()).willReturn(Try.success("Resource"));
        given(limit.acquirePermission(1)).willReturn(true);

        Try<String> result = VavrRateLimiter.decorateTrySupplier(limit, supplier).get();

        assertThat(result.isSuccess()).isTrue();
        then(supplier).should().get();
    }

    @Test
    public void decorateEitherSupplier() {
        Supplier<Either<RuntimeException, String>> supplier = mock(Supplier.class);
        given(supplier.get()).willReturn(Either.right("Resource"));
        given(limit.acquirePermission(1)).willReturn(true);

        Either<Exception, String> result = VavrRateLimiter.decorateEitherSupplier(limit, supplier::get)
            .get();

        assertThat(result.isRight()).isTrue();
        then(supplier).should().get();
    }

    @Test
    public void shouldExecuteTrySupplierAndReturnRequestNotPermitted() {
        Supplier<Try<String>> supplier = mock(Supplier.class);
        given(limit.acquirePermission(1)).willReturn(false);

        Try<String> result = VavrRateLimiter.decorateTrySupplier(limit, supplier).get();

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isInstanceOf(RequestNotPermitted.class);
        then(supplier).should(never()).get();
    }

    @Test
    public void shouldExecuteEitherSupplierAndReturnRequestNotPermitted() {
        Supplier<Either<RuntimeException, String>> supplier = mock(Supplier.class);
        given(limit.acquirePermission(1)).willReturn(false);

        Either<Exception, String> result = VavrRateLimiter.decorateEitherSupplier(limit, supplier::get)
            .get();

        assertThat(result.isLeft()).isTrue();
        assertThat(result.getLeft()).isInstanceOf(RequestNotPermitted.class);
        then(supplier).should(never()).get();
    }
}
