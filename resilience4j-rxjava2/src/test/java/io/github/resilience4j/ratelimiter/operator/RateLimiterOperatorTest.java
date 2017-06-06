/*
 * Copyright 2017 Dan Maas
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

package io.github.resilience4j.ratelimiter.operator;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vavr.collection.List;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class RateLimiterOperatorTest {

    // 10 requests / 10 ms max
    private static final String LIMITER_NAME = "test";
    private static final int PERMISSIONS_RER_CYCLE = 10;
    private static final long CYCLE_IN_MILLIS = 1_000L;
    private static final Duration TIMEOUT_DURATION = Duration.ofMillis(0);
    
    @Test
    public void shouldReturnOnCompleteUsingSingle() {
        //Given
        RateLimiter rateLimiter = RateLimiter.ofDefaults(LIMITER_NAME);
        assertThat(rateLimiter.getPermission(rateLimiter.getRateLimiterConfig().getTimeoutDuration()));

        Single.just(1)
                .lift(RateLimiterOperator.of(rateLimiter))
                .test()
                .assertValueCount(1)
                .assertValues(1)
                .assertComplete();

        //Then
        RateLimiter.Metrics metrics = rateLimiter.getMetrics();

        assertThat(metrics.getAvailablePermissions()).isEqualTo(50);
        assertThat(metrics.getNumberOfWaitingThreads()).isEqualTo(0);
    }

    @Test
    public void shouldReturnOnErrorUsingUsingSingle() {
        //Given
        RateLimiter rateLimiter = RateLimiter.ofDefaults(LIMITER_NAME);
        assertThat(rateLimiter.getPermission(rateLimiter.getRateLimiterConfig().getTimeoutDuration()));

        Single.fromCallable(() -> {throw new IOException("BAM!");})
                .lift(RateLimiterOperator.of(rateLimiter))
                .test()
                .assertError(IOException.class)
                .assertNotComplete()
                .assertSubscribed();

        //Then
        RateLimiter.Metrics metrics = rateLimiter.getMetrics();

        assertThat(metrics.getAvailablePermissions()).isEqualTo(50);
        assertThat(metrics.getNumberOfWaitingThreads()).isEqualTo(0);
    }

    @Test
    public void shouldReturnOnCompleteUsingObservable() {
        //Given
        RateLimiter rateLimiter = RateLimiter.ofDefaults(LIMITER_NAME);
        assertThat(rateLimiter.getPermission(rateLimiter.getRateLimiterConfig().getTimeoutDuration()));

        //When
        Observable.fromArray("Event 1", "Event 2")
                .lift(RateLimiterOperator.of(rateLimiter))
                .test()
                .assertValueCount(2)
                .assertValues("Event 1", "Event 2")
                .assertComplete();

        //Then
        RateLimiter.Metrics metrics = rateLimiter.getMetrics();

        assertThat(metrics.getAvailablePermissions()).isEqualTo(50);
        assertThat(metrics.getNumberOfWaitingThreads()).isEqualTo(0);
    }

    @Test
    public void shouldReturnOnCompleteUsingFlowable() {
        //Given
        RateLimiter rateLimiter = RateLimiter.ofDefaults(LIMITER_NAME);
        assertThat(rateLimiter.getPermission(rateLimiter.getRateLimiterConfig().getTimeoutDuration()));

        //When
        Flowable.fromArray("Event 1", "Event 2")
                .lift(RateLimiterOperator.of(rateLimiter))
                .test()
                .assertValueCount(2)
                .assertValues("Event 1", "Event 2")
                .assertComplete();

        //Then
        RateLimiter.Metrics metrics = rateLimiter.getMetrics();

        assertThat(metrics.getAvailablePermissions()).isEqualTo(50);
        assertThat(metrics.getNumberOfWaitingThreads()).isEqualTo(0);
    }

    @Test
    public void shouldReturnOnErrorUsingObservable() {
        //Given
        RateLimiter rateLimiter = RateLimiter.ofDefaults(LIMITER_NAME);
        assertThat(rateLimiter.getPermission(rateLimiter.getRateLimiterConfig().getTimeoutDuration()));

        //When
        Observable.fromCallable(() -> {throw new IOException("BAM!");})
                .lift(RateLimiterOperator.of(rateLimiter))
                .test()
                .assertError(IOException.class)
                .assertNotComplete()
                .assertSubscribed();

        //Then
        RateLimiter.Metrics metrics = rateLimiter.getMetrics();

        assertThat(metrics.getAvailablePermissions()).isEqualTo(50);
        assertThat(metrics.getNumberOfWaitingThreads()).isEqualTo(0);
    }

    @Test
    public void shouldReturnOnErrorUsingFlowable() {
        //Given
        RateLimiter rateLimiter = RateLimiter.ofDefaults(LIMITER_NAME);
        assertThat(rateLimiter.getPermission(rateLimiter.getRateLimiterConfig().getTimeoutDuration()));

        //When
        Flowable.fromCallable(() -> {throw new IOException("BAM!");})
                .lift(RateLimiterOperator.of(rateLimiter))
                .test()
                .assertError(IOException.class)
                .assertNotComplete()
                .assertSubscribed();

        //Then
        RateLimiter.Metrics metrics = rateLimiter.getMetrics();

        assertThat(metrics.getAvailablePermissions()).isEqualTo(50);
        assertThat(metrics.getNumberOfWaitingThreads()).isEqualTo(0);
    }

    @Test
    public void shouldReturnOnErrorWithRequestNotPermittedUsingObservable() {
        // Given
        // Create a custom configuration for a RateLimiter
        RateLimiterConfig rateLimiterConfig = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMillis(CYCLE_IN_MILLIS))
                .limitForPeriod(PERMISSIONS_RER_CYCLE)
                .timeoutDuration(TIMEOUT_DURATION)
                .build();

        // Create a RateLimiterRegistry with a custom global configuration
        RateLimiter rateLimiter = RateLimiter.of(LIMITER_NAME, rateLimiterConfig);

        assertThat(rateLimiter.getPermission(rateLimiter.getRateLimiterConfig().getTimeoutDuration()));

        Observable.fromIterable(makeEleven())
                .lift(RateLimiterOperator.of(rateLimiter))
                .test()
                .assertError(RequestNotPermitted.class)
                .assertNotComplete()
                .assertSubscribed();

        assertThat(!rateLimiter.getPermission(rateLimiter.getRateLimiterConfig().getTimeoutDuration()));

        RateLimiter.Metrics metrics = rateLimiter.getMetrics();

        assertThat(metrics.getAvailablePermissions()).isEqualTo(0);
        assertThat(metrics.getNumberOfWaitingThreads()).isEqualTo(0);
    }

    @Test
    public void shouldReturnOnErrorWithRequestNotPermittedUsingFlowable() {
        // Given
        // Create a custom configuration for a RateLimiter
        RateLimiterConfig rateLimiterConfig = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMillis(CYCLE_IN_MILLIS))
                .limitForPeriod(PERMISSIONS_RER_CYCLE)
                .timeoutDuration(TIMEOUT_DURATION)
                .build();

        // Create a RateLimiterRegistry with a custom global configuration
        RateLimiter rateLimiter = RateLimiter.of(LIMITER_NAME, rateLimiterConfig);

        assertThat(rateLimiter.getPermission(rateLimiter.getRateLimiterConfig().getTimeoutDuration()));

        Flowable.fromIterable(makeEleven())
                .lift(RateLimiterOperator.of(rateLimiter))
                .test()
                .assertError(RequestNotPermitted.class)
                .assertNotComplete()
                .assertSubscribed();

        assertThat(!rateLimiter.getPermission(rateLimiter.getRateLimiterConfig().getTimeoutDuration()));

        RateLimiter.Metrics metrics = rateLimiter.getMetrics();

        assertThat(metrics.getAvailablePermissions()).isEqualTo(0);
        assertThat(metrics.getNumberOfWaitingThreads()).isEqualTo(0);
    }

    @Test
    public void shouldReturnOnErrorAndWithIOExceptionUsingObservable() {
        // Given
        // Create a custom configuration for a RateLimiter
        RateLimiterConfig rateLimiterConfig = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMillis(CYCLE_IN_MILLIS))
                .limitForPeriod(PERMISSIONS_RER_CYCLE)
                .timeoutDuration(TIMEOUT_DURATION)
                .build();

        // Create a RateLimiterRegistry with a custom global configuration
        RateLimiter rateLimiter = RateLimiter.of(LIMITER_NAME, rateLimiterConfig);

        assertThat(rateLimiter.getPermission(rateLimiter.getRateLimiterConfig().getTimeoutDuration()));

        Observable.fromCallable(() -> {throw new IOException("BAM!");})
                .lift(RateLimiterOperator.of(rateLimiter))
                .test()
                .assertError(IOException.class)
                .assertNotComplete()
                .assertSubscribed();

        RateLimiter.Metrics metrics = rateLimiter.getMetrics();

        assertThat(metrics.getAvailablePermissions()).isEqualTo(8);
        assertThat(metrics.getNumberOfWaitingThreads()).isEqualTo(0);
    }

    private List<String> makeEleven() {
        return List.of("Event 1", "Event 2", "Event 3", "Event 4", "Event 5", "Event 6",
                "Event 7", "Event 8", "Event 9", "Event 10", "Event 11").toList();
    }

}
