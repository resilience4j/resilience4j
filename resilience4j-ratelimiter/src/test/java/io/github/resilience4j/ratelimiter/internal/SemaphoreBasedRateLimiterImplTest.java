/*
 *
 *  Copyright 2026 Robert Winkler and Bohdan Storozhuk
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
package io.github.resilience4j.ratelimiter.internal;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.awaitility.core.ConditionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Function;

import static io.vavr.control.Try.run;
import static java.lang.Thread.State.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.then;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SemaphoreBasedRateLimiterImplTest extends RateLimitersImplementationTest {

    private static final Logger LOG = LoggerFactory.getLogger(SemaphoreBasedRateLimiterImplTest.class);

    private static final int LIMIT = 2;
    private static final Duration TIMEOUT = Duration.ofMillis(100);
    private static final Duration REFRESH_PERIOD = Duration.ofMillis(100);
    private static final String CONFIG_MUST_NOT_BE_NULL = "Config must not be null";
    private static final String NAME_MUST_NOT_BE_NULL = "Name must not be null";
    private static final Object O = new Object();

    private RateLimiterConfig config;

    private static ConditionFactory awaitImpatiently() {
        return await()
            .pollDelay(1, TimeUnit.MICROSECONDS)
            .pollInterval(2, TimeUnit.MILLISECONDS);
    }

    @Override
    protected RateLimiter buildRateLimiter(RateLimiterConfig config) {
        return new SemaphoreBasedRateLimiter("test", config, Executors.newScheduledThreadPool(1));
    }

    @BeforeEach
    void init() {
        config = RateLimiterConfig.custom()
            .timeoutDuration(TIMEOUT)
            .limitRefreshPeriod(REFRESH_PERIOD)
            .limitForPeriod(LIMIT)
            .build();
    }

    @Test
    void rateLimiterCreationWithProvidedScheduler() throws Exception {
        ScheduledExecutorService scheduledExecutorService = mock(ScheduledExecutorService.class);
        RateLimiterConfig configSpy = spy(config);
        SemaphoreBasedRateLimiter limit = new SemaphoreBasedRateLimiter("test", configSpy,
            scheduledExecutorService);

        ArgumentCaptor<Runnable> refreshLimitRunnableCaptor = ArgumentCaptor
            .forClass(Runnable.class);
        verify(scheduledExecutorService)
            .scheduleAtFixedRate(
                refreshLimitRunnableCaptor.capture(),
                eq(config.getLimitRefreshPeriod().toNanos()),
                eq(config.getLimitRefreshPeriod().toNanos()),
                eq(TimeUnit.NANOSECONDS)
            );

        Runnable refreshLimitRunnable = refreshLimitRunnableCaptor.getValue();

        then(limit.acquirePermission()).isTrue();
        then(limit.acquirePermission()).isTrue();
        then(limit.acquirePermission()).isFalse();

        Thread.sleep(REFRESH_PERIOD.toMillis() * 2);
        verify(configSpy, times(1)).getLimitForPeriod();

        refreshLimitRunnable.run();

        verify(configSpy, times(2)).getLimitForPeriod();

        then(limit.acquirePermission()).isTrue();
        then(limit.acquirePermission()).isTrue();
        then(limit.acquirePermission()).isFalse();
    }

    @Test
    void acquirePermissionAndMetrics() throws Exception {
        ScheduledExecutorService scheduledExecutorService = mock(ScheduledExecutorService.class);
        RateLimiterConfig configSpy = spy(config);
        SemaphoreBasedRateLimiter limit = new SemaphoreBasedRateLimiter("test", configSpy,
            scheduledExecutorService);
        RateLimiter.Metrics detailedMetrics = limit.getMetrics();

        SynchronousQueue<Object> synchronousQueue = new SynchronousQueue<>();
        Thread thread = new Thread(() -> run(() -> {
            for (int i = 0; i < LIMIT; i++) {
                synchronousQueue.put(O);
                limit.acquirePermission();
            }
            limit.acquirePermission();
        }));
        thread.setDaemon(true);
        thread.start();

        for (int i = 0; i < LIMIT; i++) {
            synchronousQueue.take();
        }

        awaitImpatiently()
            .atMost(100, TimeUnit.MILLISECONDS)
            .until(detailedMetrics::getAvailablePermissions, equalTo(0));
        awaitImpatiently()
            .atMost(2, TimeUnit.SECONDS).until(thread::getState, equalTo(TIMED_WAITING));
        then(detailedMetrics.getAvailablePermissions()).isZero();

        limit.refreshLimit();
        awaitImpatiently()
            .atMost(100, TimeUnit.MILLISECONDS)
            .until(detailedMetrics::getAvailablePermissions, equalTo(1));
        awaitImpatiently()
            .atMost(2, TimeUnit.SECONDS).until(thread::getState, equalTo(TERMINATED));
        then(detailedMetrics.getAvailablePermissions()).isEqualTo(1);

        limit.changeLimitForPeriod(3);
        limit.refreshLimit();
        then(detailedMetrics.getAvailablePermissions()).isEqualTo(3);
    }

    @Test
    void changeDefaultTimeoutDuration() {
        ScheduledExecutorService scheduledExecutorService = mock(ScheduledExecutorService.class);
        RateLimiter rateLimiter = new SemaphoreBasedRateLimiter("some", config,
            scheduledExecutorService);
        RateLimiterConfig rateLimiterConfig = rateLimiter.getRateLimiterConfig();
        then(rateLimiterConfig.getTimeoutDuration()).isEqualTo(TIMEOUT);
        then(rateLimiterConfig.getLimitForPeriod()).isEqualTo(LIMIT);
        then(rateLimiterConfig.getLimitRefreshPeriod()).isEqualTo(REFRESH_PERIOD);

        rateLimiter.changeTimeoutDuration(Duration.ofSeconds(1));
        then(rateLimiterConfig != rateLimiter.getRateLimiterConfig()).isTrue();
        rateLimiterConfig = rateLimiter.getRateLimiterConfig();
        then(rateLimiterConfig.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(1));
        then(rateLimiterConfig.getLimitForPeriod()).isEqualTo(LIMIT);
        then(rateLimiterConfig.getLimitRefreshPeriod()).isEqualTo(REFRESH_PERIOD);
    }

    @Test
    void changeLimitForPeriod() {
        ScheduledExecutorService scheduledExecutorService = mock(ScheduledExecutorService.class);
        RateLimiter rateLimiter = new SemaphoreBasedRateLimiter("some", config,
            scheduledExecutorService);
        RateLimiterConfig rateLimiterConfig = rateLimiter.getRateLimiterConfig();
        then(rateLimiterConfig.getTimeoutDuration()).isEqualTo(TIMEOUT);
        then(rateLimiterConfig.getLimitForPeriod()).isEqualTo(LIMIT);
        then(rateLimiterConfig.getLimitRefreshPeriod()).isEqualTo(REFRESH_PERIOD);

        rateLimiter.changeLimitForPeriod(LIMIT * 2);
        then(rateLimiterConfig != rateLimiter.getRateLimiterConfig()).isTrue();
        rateLimiterConfig = rateLimiter.getRateLimiterConfig();
        then(rateLimiterConfig.getTimeoutDuration()).isEqualTo(TIMEOUT);
        then(rateLimiterConfig.getLimitForPeriod()).isEqualTo(LIMIT * 2);
        then(rateLimiterConfig.getLimitRefreshPeriod()).isEqualTo(REFRESH_PERIOD);
    }

    @Test
    void acquirePermissionInterruption() {
        ScheduledExecutorService scheduledExecutorService = mock(ScheduledExecutorService.class);
        RateLimiterConfig configSpy = spy(config);
        SemaphoreBasedRateLimiter limit = new SemaphoreBasedRateLimiter("test", configSpy,
            scheduledExecutorService);
        assertThat(limit.getName()).isEqualTo("test");
        limit.acquirePermission();
        limit.acquirePermission();

        Thread thread = new Thread(() -> {
            limit.acquirePermission();
            while (true) {
                Function.identity().apply(1);
            }
        });
        thread.setDaemon(true);
        thread.start();

        awaitImpatiently()
            .atMost(2, TimeUnit.SECONDS).until(thread::getState, equalTo(TIMED_WAITING));

        thread.interrupt();

        awaitImpatiently()
            .atMost(2, TimeUnit.SECONDS).until(thread::getState, equalTo(RUNNABLE));
        awaitImpatiently()
            .atMost(100, TimeUnit.MILLISECONDS).until(thread::isInterrupted);
    }

    @Test
    void getName() {
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        SemaphoreBasedRateLimiter limit = new SemaphoreBasedRateLimiter("test", config, scheduler);
        then(limit.getName()).isEqualTo("test");
    }

    @Test
    void getMetrics() {
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        SemaphoreBasedRateLimiter limit = new SemaphoreBasedRateLimiter("test", config, scheduler);
        RateLimiter.Metrics metrics = limit.getMetrics();
        then(metrics.getNumberOfWaitingThreads()).isZero();
    }

    @Test
    void getRateLimiterConfig() {
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        SemaphoreBasedRateLimiter limit = new SemaphoreBasedRateLimiter("test", config, scheduler);
        then(limit.getRateLimiterConfig()).isEqualTo(config);
    }

    @Test
    void isUpperLimitedForPermissions() {
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        SemaphoreBasedRateLimiter limit = new SemaphoreBasedRateLimiter("test", config, scheduler);
        RateLimiter.Metrics metrics = limit.getMetrics();
        then(metrics.getAvailablePermissions()).isEqualTo(2);
        limit.refreshLimit();
        then(metrics.getAvailablePermissions()).isEqualTo(2);
    }

    @Test
    void getDetailedMetrics() {
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        SemaphoreBasedRateLimiter limit = new SemaphoreBasedRateLimiter("test", config, scheduler);
        RateLimiter.Metrics metrics = limit.getMetrics();
        then(metrics.getNumberOfWaitingThreads()).isZero();
        then(metrics.getAvailablePermissions()).isEqualTo(2);
    }

    @Test
    void constructionWithNullName() {
        assertThatThrownBy(() -> new SemaphoreBasedRateLimiter(null, config, (ScheduledExecutorService) null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage(NAME_MUST_NOT_BE_NULL);
    }

    @Test
    void constructionWithNullConfig() {
        assertThatThrownBy(() -> new SemaphoreBasedRateLimiter("test", null, (ScheduledExecutorService) null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage(CONFIG_MUST_NOT_BE_NULL);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shutdownRateLimiter() throws InterruptedException {
        ScheduledExecutorService scheduledExecutorService = mock(ScheduledExecutorService.class);
        RateLimiterConfig configSpy = spy(config);

        ScheduledFuture<?> future = mock(ScheduledFuture.class);

        doReturn(future).when(scheduledExecutorService).scheduleAtFixedRate(
            any(Runnable.class), any(Long.class), any(Long.class), any(TimeUnit.class));

        SemaphoreBasedRateLimiter limit = new SemaphoreBasedRateLimiter("test", configSpy,
            scheduledExecutorService);

        then(limit.acquirePermission(1)).isTrue();
        then(limit.acquirePermission(1)).isTrue();
        then(limit.acquirePermission(1)).isFalse();

        limit.shutdown();
        Thread.sleep(REFRESH_PERIOD.toMillis() * 2);
        verify(future, times(1)).isCancelled();
        then(limit.acquirePermission(1)).isFalse();
    }
}
