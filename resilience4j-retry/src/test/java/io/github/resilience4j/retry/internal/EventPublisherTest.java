/*
 *
 *  Copyright 2016: Robert Winkler
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
package io.github.resilience4j.retry.internal;

import io.github.resilience4j.core.functions.CheckedRunnable;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.event.RetryEvent;
import io.github.resilience4j.test.HelloWorldException;
import io.github.resilience4j.test.HelloWorldService;
import io.reactivex.subscribers.TestSubscriber;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static io.github.resilience4j.adapter.RxJava2Adapter.toFlowable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class EventPublisherTest {

    private HelloWorldService helloWorldService;
    private long sleptTime = 0L;

    @Before
    public void setUp() {
        helloWorldService = mock(HelloWorldService.class);
        RetryImpl.setSleepFunction(sleep -> sleptTime += sleep);
    }

    @Test
    public void shouldReturnAfterThreeAttempts() {
        willThrow(new HelloWorldException()).given(helloWorldService).sayHelloWorld();
        Retry retry = Retry.ofDefaults("id");
        TestSubscriber<RetryEvent.Type> testSubscriber = toFlowable(retry.getEventPublisher())
            .map(RetryEvent::getEventType)
            .test();
        CheckedRunnable retryableRunnable = Retry
            .decorateCheckedRunnable(retry, helloWorldService::sayHelloWorld);

        Try<Void> result = Try.run(() -> retryableRunnable.run());

        then(helloWorldService).should(times(3)).sayHelloWorld();
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(HelloWorldException.class);
        assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION * 2);
        testSubscriber.assertValueCount(3)
            .assertValues(RetryEvent.Type.RETRY, RetryEvent.Type.RETRY, RetryEvent.Type.ERROR);
    }

    @Test
    public void shouldReturnAfterTwoAttempts() {
        willThrow(new HelloWorldException()).willDoNothing().given(helloWorldService)
            .sayHelloWorld();
        Retry retry = Retry.ofDefaults("id");
        TestSubscriber<RetryEvent.Type> testSubscriber = toFlowable(retry.getEventPublisher())
            .map(RetryEvent::getEventType)
            .test();
        CheckedRunnable retryableRunnable = Retry
            .decorateCheckedRunnable(retry, helloWorldService::sayHelloWorld);

        Try<Void> result = Try.run(() -> retryableRunnable.run());

        then(helloWorldService).should(times(2)).sayHelloWorld();
        assertThat(result.isSuccess()).isTrue();
        assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION);
        testSubscriber.assertValueCount(2)
            .assertValues(RetryEvent.Type.RETRY, RetryEvent.Type.SUCCESS);
    }

    @Test
    public void shouldIgnoreError() {
        willThrow(new HelloWorldException()).willDoNothing().given(helloWorldService)
            .sayHelloWorld();
        RetryConfig config = RetryConfig.custom()
            .retryOnException(t -> t instanceof IOException)
            .maxAttempts(3).build();
        Retry retry = Retry.of("id", config);
        TestSubscriber<RetryEvent.Type> testSubscriber = toFlowable(retry.getEventPublisher())
            .map(RetryEvent::getEventType)
            .test();
        CheckedRunnable retryableRunnable = Retry
            .decorateCheckedRunnable(retry, helloWorldService::sayHelloWorld);

        Try<Void> result = Try.run(() -> retryableRunnable.run());

        then(helloWorldService).should().sayHelloWorld();
        assertThat(result.isFailure()).isTrue();
        assertThat(sleptTime).isEqualTo(0);
        testSubscriber.assertValueCount(1).assertValues(RetryEvent.Type.IGNORED_ERROR);
    }
}
