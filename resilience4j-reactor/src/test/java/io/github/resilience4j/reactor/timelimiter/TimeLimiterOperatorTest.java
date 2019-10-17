/*
 * Copyright 2019 authors
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

package io.github.resilience4j.reactor.timelimiter;

import io.github.resilience4j.test.HelloWorldService;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;


public class TimeLimiterOperatorTest {

    private final TimeLimiter timeLimiter = mock(TimeLimiter.class);
    private final HelloWorldService helloWorldService = mock(HelloWorldService.class);

    @Test
    public void doNotTimeoutUsingMono() {
        given(timeLimiter.getTimeLimiterConfig())
            .willReturn(toConfig(Duration.ofMinutes(1)));
        given(helloWorldService.returnHelloWorld())
            .willReturn("Hello world");

        Mono<?> mono = Mono.fromCallable(helloWorldService::returnHelloWorld)
            .compose(TimeLimiterOperator.of(timeLimiter));

        StepVerifier.create(mono)
            .expectNextCount(1)
            .verifyComplete();
        then(timeLimiter).should(times(2))
            .onSuccess();
    }

    @Test
    public void timeoutUsingMono() {
        given(timeLimiter.getTimeLimiterConfig())
            .willReturn(toConfig(Duration.ofMillis(1)));

        Mono<?> mono = Mono.delay(Duration.ofMinutes(1))
            .compose(TimeLimiterOperator.of(timeLimiter));

        StepVerifier.create(mono)
            .expectError(TimeoutException.class)
            .verify(Duration.ofMinutes(1));
        then(timeLimiter).should()
            .onError(any(TimeoutException.class));
    }

    @Test
    public void timeoutNeverUsingMono() {
        given(timeLimiter.getTimeLimiterConfig())
            .willReturn(toConfig(Duration.ofMillis(1)));

        Mono<?> flux = Mono.never()
            .compose(TimeLimiterOperator.of(timeLimiter));

        StepVerifier.create(flux)
            .expectError(TimeoutException.class)
            .verify(Duration.ofMinutes(1));
        then(timeLimiter).should()
            .onError(any(TimeoutException.class));
    }

    @Test
    public void otherErrorUsingMono() {
        given(timeLimiter.getTimeLimiterConfig())
            .willReturn(toConfig(Duration.ofMinutes(1)));
        given(helloWorldService.returnHelloWorld())
            .willThrow(new Error("BAM!"));

        Mono<?> mono = Mono.fromCallable(helloWorldService::returnHelloWorld)
            .compose(TimeLimiterOperator.of(timeLimiter));

        StepVerifier.create(mono)
            .expectError(Error.class)
            .verify(Duration.ofMinutes(1));
        then(timeLimiter).should()
            .onError(any(Error.class));
    }

    @Test
    public void doNotTimeoutUsingFlux() {
        given(timeLimiter.getTimeLimiterConfig())
            .willReturn(toConfig(Duration.ofMinutes(1)));

        Flux<?> flux = Flux.interval(Duration.ofMillis(1))
            .take(2)
            .compose(TimeLimiterOperator.of(timeLimiter));

        StepVerifier.create(flux)
            .expectNextCount(2)
            .verifyComplete();
        then(timeLimiter).should(times(3))
            .onSuccess();
    }

    @Test
    public void timeoutUsingFlux() {
        given(timeLimiter.getTimeLimiterConfig())
            .willReturn(toConfig(Duration.ofMillis(1)));

        Flux<?> flux = Flux.interval(Duration.ofSeconds(1))
            .compose(TimeLimiterOperator.of(timeLimiter));

        StepVerifier.create(flux)
            .expectError(TimeoutException.class)
            .verify(Duration.ofMinutes(1));
        then(timeLimiter).should()
            .onError(any(TimeoutException.class));
    }

    @Test
    public void timeoutNeverUsingFlux() {
        given(timeLimiter.getTimeLimiterConfig())
            .willReturn(toConfig(Duration.ofMillis(1)));

        Flux<?> flux = Flux.never()
            .compose(TimeLimiterOperator.of(timeLimiter));

        StepVerifier.create(flux)
            .expectError(TimeoutException.class)
            .verify(Duration.ofMinutes(1));
        then(timeLimiter).should()
            .onError(any(TimeoutException.class));
    }

    @Test
    public void otherErrorUsingFlux() {
        given(timeLimiter.getTimeLimiterConfig())
            .willReturn(toConfig(Duration.ofMinutes(1)));

        Flux<?> flux = Flux.error(new Error("BAM!"))
            .compose(TimeLimiterOperator.of(timeLimiter));

        StepVerifier.create(flux)
            .expectError(Error.class)
            .verify(Duration.ofMinutes(1));
        then(timeLimiter).should()
            .onError(any(Error.class));
    }

    private TimeLimiterConfig toConfig(Duration timeout) {
        return TimeLimiterConfig.custom()
            .timeoutDuration(timeout)
            .build();
    }
}
