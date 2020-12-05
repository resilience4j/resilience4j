/*
 *
 *  Copyright 2016 Robert Winkler and Bohdan Storozhuk
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

import io.github.resilience4j.core.functions.CallsResult;
import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedRunnable;
import io.vavr.control.Either;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.Future;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.jayway.awaitility.Awaitility.await;
import static io.vavr.API.*;
import static io.vavr.Predicates.instanceOf;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;


@SuppressWarnings("unchecked")
public class RateLimiterWithConditionalDrainTest {

    private static final int LIMIT = 50;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REFRESH_PERIOD = Duration.ofNanos(500);

    private Function<CallsResult, Boolean> drainConditionChecker;
    private RateLimiterConfig config;
    private RateLimiter limit;

    @Before
    public void init() {
        drainConditionChecker = mock(Function.class);
        config = RateLimiterConfig.custom()
            .timeoutDuration(TIMEOUT)
            .limitRefreshPeriod(REFRESH_PERIOD)
            .limitForPeriod(LIMIT)
            .drainPermissionsOnResult(drainConditionChecker)
            .build();
        limit = mock(RateLimiter.class);
        given(limit.getRateLimiterConfig()).willReturn(config);
    }

    @Test
    public void decorateCheckedSupplierAndApplyWithDrainConditionNotMet() throws Throwable {
        CheckedFunction0 supplier = mock(CheckedFunction0.class);
        CheckedFunction0 decorated = RateLimiter.decorateCheckedSupplier(limit, supplier);
        given(limit.acquirePermission(1)).willReturn(true);
        given(drainConditionChecker.apply(any())).willReturn(false);

        Try result = Try.of(decorated);

        assertThat(result.isSuccess()).isTrue();
        verify(drainConditionChecker).apply(argThat(CallsResult::isSuccessful));
        verify(limit, never()).drainPermissions();
    }

    @Test
    public void decorateFailingCheckedSupplierAndApplyWithDrainConditionNotMet() throws Throwable {
        CheckedFunction0 supplier = mock(CheckedFunction0.class);
        when(supplier.apply()).thenThrow(RuntimeException.class);
        CheckedFunction0 decorated = RateLimiter.decorateCheckedSupplier(limit, supplier);
        given(limit.acquirePermission(1)).willReturn(true);
        given(drainConditionChecker.apply(any())).willReturn(false);

        Try result = Try.of(decorated);

        assertThat(result.isFailure()).isTrue();
        verify(drainConditionChecker).apply(argThat(CallsResult::isFailed));
        verify(limit, never()).drainPermissions();
    }

    @Test
    public void decorateCheckedSupplierAndApplyWithDrainConditionMet() throws Throwable {
        CheckedFunction0 supplier = mock(CheckedFunction0.class);
        CheckedFunction0 decorated = RateLimiter.decorateCheckedSupplier(limit, supplier);
        given(limit.acquirePermission(1)).willReturn(true);
        given(drainConditionChecker.apply(any())).willReturn(true);

        Try result = Try.of(decorated);

        assertThat(result.isSuccess()).isTrue();
        verify(drainConditionChecker).apply(argThat(CallsResult::isSuccessful));
        verify(limit).drainPermissions();
    }

    // TODO: I'll write more tests after this features implementation was approved
}