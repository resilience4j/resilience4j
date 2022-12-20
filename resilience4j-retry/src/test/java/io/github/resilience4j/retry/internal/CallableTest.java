package io.github.resilience4j.retry.internal;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.test.HelloWorldException;
import io.github.resilience4j.test.HelloWorldService;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

public class CallableTest {

    private HelloWorldService helloWorldService;

    @Before
    public void setUp() {
        helloWorldService = mock(HelloWorldService.class);
    }

    @Test
    public void shouldPropagateLastExceptionWhenSleepFunctionThrowsException() throws IOException {
        willThrow(new HelloWorldException()).given(helloWorldService).returnHelloWorldWithException();
        RetryConfig config = RetryConfig.custom()
            .intervalFunction((a) -> -1L)
            .build();
        Retry retry = Retry.of("id", config);
        Callable<String> retryableCallable = Retry
            .decorateCallable(retry, helloWorldService::returnHelloWorldWithException);

        Try<Void> result = Try.run(retryableCallable::call);

        then(helloWorldService).should().returnHelloWorldWithException();
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(HelloWorldException.class);
    }

}
