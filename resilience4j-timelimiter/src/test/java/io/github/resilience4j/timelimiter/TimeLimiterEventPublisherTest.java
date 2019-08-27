/*
 *
 *  Copyright 2019 authors
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
package io.github.resilience4j.timelimiter;

import io.github.resilience4j.timelimiter.event.TimeLimiterEvent;
import io.github.resilience4j.timelimiter.event.TimeLimiterOnErrorEvent;
import io.vavr.control.Try;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.junit.Test;
import org.slf4j.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;


public class TimeLimiterEventPublisherTest {

    private static final Duration NEVER = Duration.ZERO;

    private Logger logger = mock(Logger.class);

    @Test
    public void shouldReturnTheSameConsumer() {
        TimeLimiter timeLimiter = TimeLimiter.of(NEVER);

        TimeLimiter.EventPublisher eventPublisher = timeLimiter.getEventPublisher();
        TimeLimiter.EventPublisher eventPublisher2 = timeLimiter.getEventPublisher();

        assertThat(eventPublisher).isEqualTo(eventPublisher2);
    }

    @Test
    public void shouldConsumeOnSuccessEvent() throws Exception {
        TimeLimiter timeLimiter = TimeLimiter.of(NEVER);
        timeLimiter.getEventPublisher()
                .onSuccess(event -> logger.info(event.getEventType().toString()));
        Supplier<CompletableFuture<String>> futureSupplier = () ->
                CompletableFuture.completedFuture("Hello world");

        String result = timeLimiter.decorateFutureSupplier(futureSupplier).call();

        assertThat(result).isEqualTo("Hello world");
        then(logger).should(times(1)).info("SUCCESS");
    }

    @Test
    public void shouldConsumeOnTimeoutEvent() {
        TimeLimiter timeLimiter = TimeLimiter.of(NEVER);
        timeLimiter.getEventPublisher()
                .onTimeout(event -> logger.info(event.getEventType().toString()));
        Supplier<CompletableFuture<String>> futureSupplier = () ->
                CompletableFuture.supplyAsync(this::timeout);

        Try.ofCallable(timeLimiter.decorateFutureSupplier(futureSupplier));

        then(logger).should(times(1)).info("TIMEOUT");
    }

    @Test
    public void shouldConsumeOnErrorEvent() {
        TimeLimiter timeLimiter = TimeLimiter.of(Duration.ofSeconds(1));
        timeLimiter.getEventPublisher()
                .onError(event -> logger.info(event.getEventType().toString() + " " + event.getThrowable().toString()));
        Supplier<CompletableFuture<String>> futureSupplier = () ->
                CompletableFuture.supplyAsync(this::fail);

        Try.ofCallable(timeLimiter.decorateFutureSupplier(futureSupplier));

        then(logger).should(times(1)).info("ERROR java.lang.RuntimeException");
    }

    private String fail() {
        throw new RuntimeException();
    }

    private String timeout() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // nothing
        }
        return "timeout";
    }
}