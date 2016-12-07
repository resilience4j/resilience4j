/*
 *
 *  Copyright 2016 Robert Winkler
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
package io.github.robwin.retry.internal;

import io.github.robwin.consumer.CircularEventConsumer;
import io.github.robwin.retry.Retry;
import io.github.robwin.retry.RetryConfig;
import io.github.robwin.retry.event.RetryEvent;
import io.github.robwin.test.HelloWorldService;
import javaslang.control.Try;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;

import javax.xml.ws.WebServiceException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class CircularEventConsumerTest {


    private HelloWorldService helloWorldService;
    private long sleptTime = 0L;

    @Before
    public void setUp(){
        helloWorldService = mock(HelloWorldService.class);
        RetryContext.sleepFunction = sleep -> sleptTime += sleep;
    }

    @Test
    public void shouldReturnAfterThreeAttempts() {
        // Given the HelloWorldService throws an exception
        willThrow(new WebServiceException("BAM!")).given(helloWorldService).sayHelloWorld();

        CircularEventConsumer<RetryEvent> ringBuffer = new CircularEventConsumer<>(2);
        // Create a Retry with default configuration
        RetryContext retryContext = (RetryContext) Retry.ofDefaults("id");
        retryContext.getEventStream()
                .subscribe(ringBuffer);
        // Decorate the invocation of the HelloWorldService
        Try.CheckedRunnable retryableRunnable = Retry.decorateCheckedRunnable(retryContext, helloWorldService::sayHelloWorld);

        // When
        Try<Void> result = Try.run(retryableRunnable);

        // Then the helloWorldService should be invoked 3 times
        BDDMockito.then(helloWorldService).should(times(3)).sayHelloWorld();
        // and the result should be a failure
        assertThat(result.isFailure()).isTrue();
        // and the returned exception should be of type RuntimeException
        assertThat(result.failed().get()).isInstanceOf(WebServiceException.class);
        assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION*2);

        assertThat(ringBuffer.getBufferedEvents()).hasSize(1);
        assertThat(ringBuffer.getBufferedEvents()).extracting("eventType")
                .containsExactly(RetryEvent.Type.ERROR);
    }

    @Test
    public void shouldReturnAfterTwoAttempts() {
        // Given the HelloWorldService throws an exception
        willThrow(new WebServiceException("BAM!")).willNothing().given(helloWorldService).sayHelloWorld();

        CircularEventConsumer<RetryEvent> ringBuffer = new CircularEventConsumer<>(2);
        // Create a Retry with default configuration
        RetryContext retryContext = (RetryContext) Retry.ofDefaults("id");
        retryContext.getEventStream()
                .subscribe(ringBuffer);
        // Decorate the invocation of the HelloWorldService
        Try.CheckedRunnable retryableRunnable = Retry.decorateCheckedRunnable(retryContext, helloWorldService::sayHelloWorld);

        // When
        Try<Void> result = Try.run(retryableRunnable);

        // Then the helloWorldService should be invoked 2 times
        BDDMockito.then(helloWorldService).should(times(2)).sayHelloWorld();
        // and the result should be a sucess
        assertThat(result.isSuccess()).isTrue();
        assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION);

        assertThat(ringBuffer.getBufferedEvents()).hasSize(1);
        assertThat(ringBuffer.getBufferedEvents()).extracting("eventType")
                .containsExactly(RetryEvent.Type.SUCCESS);
    }
}
