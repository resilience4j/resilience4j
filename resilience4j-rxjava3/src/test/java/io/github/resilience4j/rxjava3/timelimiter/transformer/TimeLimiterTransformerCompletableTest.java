/*
 *  Copyright 2019 authors
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

package io.github.resilience4j.rxjava3.timelimiter.transformer;

import io.github.resilience4j.rxjava3.TestSchedulerRule;
import io.github.resilience4j.rxjava3.timelimiter.transformer.TimeLimiterTransformer;
import io.github.resilience4j.test.HelloWorldService;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import org.junit.Rule;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

public class TimeLimiterTransformerCompletableTest {

    @Rule
    public final TestSchedulerRule testSchedulerRule = new TestSchedulerRule();
    private final TestScheduler testScheduler = testSchedulerRule.getTestScheduler();
    private final TimeLimiter timeLimiter = mock(TimeLimiter.class);
    private final HelloWorldService helloWorldService = mock(HelloWorldService.class);

    @Test
    public void otherError() {
        given(helloWorldService.returnHelloWorld())
            .willThrow(new RuntimeException());
        given(timeLimiter.getTimeLimiterConfig())
            .willReturn(toConfig(Duration.ZERO));
        TestObserver<?> observer = Completable.fromRunnable(helloWorldService::returnHelloWorld)
            .compose(TimeLimiterTransformer.of(timeLimiter))
            .test();

        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES);

        observer.assertError(RuntimeException.class);
        then(timeLimiter).should()
            .onError(any(RuntimeException.class));
    }

    @Test
    public void timeout() {
        given(timeLimiter.getTimeLimiterConfig())
            .willReturn(toConfig(Duration.ZERO));
        TestObserver<?> observer = Maybe.timer(1, TimeUnit.MINUTES)
            .flatMapCompletable(t -> Completable.complete())
            .compose(TimeLimiterTransformer.of(timeLimiter))
            .test();

        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES);

        observer.assertError(TimeoutException.class);
        then(timeLimiter).should()
            .onError(any(TimeoutException.class));
    }

    @Test
    public void doNotTimeout() {
        given(helloWorldService.returnHelloWorld())
            .willReturn("hello");
        given(timeLimiter.getTimeLimiterConfig())
            .willReturn(toConfig(Duration.ofMinutes(1)));
        TestObserver<?> observer = Completable.fromRunnable(helloWorldService::returnHelloWorld)
            .compose(TimeLimiterTransformer.of(timeLimiter))
            .test();

        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES);

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
