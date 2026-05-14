/*
 *  Copyright 2026 authors
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

package io.github.resilience4j.timelimiter.transformer;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.TestScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

class TimeLimiterTransformerObservableTest {

    private TestScheduler testScheduler;

    @BeforeEach
    void setUpScheduler() {
        testScheduler = new TestScheduler();
        RxJavaPlugins.setIoSchedulerHandler(scheduler -> testScheduler);
        RxJavaPlugins.setComputationSchedulerHandler(scheduler -> testScheduler);
        RxJavaPlugins.setNewThreadSchedulerHandler(scheduler -> testScheduler);
    }

    @AfterEach
    void resetScheduler() {
        RxJavaPlugins.reset();
    }
    private final TimeLimiter timeLimiter = mock(TimeLimiter.class);

    @Test
    void otherError() {
        given(timeLimiter.getTimeLimiterConfig())
            .willReturn(toConfig(Duration.ZERO));
        TestObserver<?> observer = Observable.error(new RuntimeException())
            .compose(TimeLimiterTransformer.of(timeLimiter))
            .test();

        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES);

        observer.assertError(RuntimeException.class);
        then(timeLimiter).should()
            .onError(any(RuntimeException.class));
    }

    @Test
    void timeout() {
        given(timeLimiter.getTimeLimiterConfig())
            .willReturn(toConfig(Duration.ZERO));
        TestObserver<?> observer = Observable.interval(1, TimeUnit.MINUTES)
            .compose(TimeLimiterTransformer.of(timeLimiter))
            .test();

        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES);

        observer.assertError(TimeoutException.class);
        then(timeLimiter).should()
            .onError(any(TimeoutException.class));
    }

    @Test
    void timeoutEmpty() {
        given(timeLimiter.getTimeLimiterConfig())
            .willReturn(toConfig(Duration.ZERO));
        TestObserver<?> observer = Observable.empty()
            .delay(1, TimeUnit.MINUTES)
            .compose(TimeLimiterTransformer.of(timeLimiter))
            .test();

        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES);

        observer.assertError(TimeoutException.class);
        then(timeLimiter).should()
            .onError(any(TimeoutException.class));
    }

    @Test
    void doNotTimeout() {
        given(timeLimiter.getTimeLimiterConfig())
            .willReturn(toConfig(Duration.ofMinutes(1)));
        TestObserver<?> observer = Observable.interval(1, TimeUnit.SECONDS)
            .take(2)
            .compose(TimeLimiterTransformer.of(timeLimiter))
            .test();

        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES);

        observer.assertValueCount(2)
            .assertComplete();
        then(timeLimiter).should(times(3))
            .onSuccess();
    }

    @Test
    void timeoutAfterInitial() throws InterruptedException {
        int timeout = 2;
        int initialDelay = 1;
        int periodDelay = 3;
        given(timeLimiter.getTimeLimiterConfig())
            .willReturn(toConfig(Duration.ofSeconds(timeout)));
        TestObserver<?> observer = Observable.interval(initialDelay, periodDelay, TimeUnit.SECONDS)
            .compose(TimeLimiterTransformer.of(timeLimiter))
            .test();

        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES);

        observer.await()
            .assertValueCount(1)
            .assertError(TimeoutException.class);
        then(timeLimiter).should()
            .onSuccess();
        then(timeLimiter).should()
            .onError(any(TimeoutException.class));
    }

    @Test
    void doNotTimeoutEmpty() {
        given(timeLimiter.getTimeLimiterConfig())
            .willReturn(toConfig(Duration.ofMinutes(1)));

        TestObserver<?> observer = Observable.empty()
            .compose(TimeLimiterTransformer.of(timeLimiter))
            .test();

        observer.assertComplete();
        then(timeLimiter).should()
            .onSuccess();
    }

    private TimeLimiterConfig toConfig(Duration timeout) {
        return TimeLimiterConfig.custom()
            .timeoutDuration(timeout)
            .build();
    }

}
