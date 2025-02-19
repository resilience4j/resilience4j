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

import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.core.functions.Either;
import io.github.resilience4j.ratelimiter.internal.AtomicRateLimiter;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


@SuppressWarnings("unchecked")
public class RateLimiterWithConditionalDrainTest {

    private static final int LIMIT = 50;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REFRESH_PERIOD = Duration.ofNanos(500);

    private Predicate<Either<? extends Throwable, ?>> drainConditionChecker;
    private RateLimiter limit;

    @Before
    public void init() {
        drainConditionChecker = mock(Predicate.class);
        RateLimiterConfig config = RateLimiterConfig.custom()
            .timeoutDuration(TIMEOUT)
            .limitRefreshPeriod(REFRESH_PERIOD)
            .limitForPeriod(LIMIT)
            .drainPermissionsOnResult(drainConditionChecker)
            .build();
        RateLimiter realLimit = new AtomicRateLimiter("test", config);
        limit = spy(realLimit);
        doReturn(true).when(limit).acquirePermission(1);
    }

    @Test
    public void decorateCheckedSupplierAndApplyWithDrainConditionNotMet() throws Throwable {
        CheckedSupplier<?> supplier = mock(CheckedSupplier.class);
        CheckedSupplier<?> decorated = RateLimiter.decorateCheckedSupplier(limit, supplier);
        given(limit.acquirePermission(1)).willReturn(true);
        given(drainConditionChecker.test(any())).willReturn(false);

        Try<?> result = Try.of(() -> decorated.get());

        assertThat(result.isSuccess()).isTrue();
        verify(drainConditionChecker).test(argThat(Either::isRight));
        verify(limit, never()).drainPermissions();
    }

    @Test
    public void decorateFailingCheckedSupplierAndApplyWithDrainConditionNotMet() throws Throwable {
        CheckedSupplier<?> supplier = mock(CheckedSupplier.class);
        when(supplier.get()).thenThrow(RuntimeException.class);
        CheckedSupplier<?> decorated = RateLimiter.decorateCheckedSupplier(limit, supplier);
        given(limit.acquirePermission(1)).willReturn(true);
        given(drainConditionChecker.test(any())).willReturn(false);

        final Try<?> result = Try.of(() -> decorated.get());

        assertThat(result.isFailure()).isTrue();
        verify(drainConditionChecker).test(argThat(Either::isLeft));
        verify(limit, never()).drainPermissions();
    }

    @Test
    public void decorateCheckedSupplierAndApplyWithDrainConditionMet() throws Throwable {
        CheckedSupplier<?> supplier = mock(CheckedSupplier.class);
        CheckedSupplier<?> decorated = RateLimiter.decorateCheckedSupplier(limit, supplier);
        given(limit.acquirePermission(1)).willReturn(true);
        given(drainConditionChecker.test(any())).willReturn(true);

        Try<?> result = Try.of(() -> decorated.get());

        assertThat(result.isSuccess()).isTrue();
        verify(drainConditionChecker).test(argThat(Either::isRight));
        verify(limit).drainPermissions();
    }
}