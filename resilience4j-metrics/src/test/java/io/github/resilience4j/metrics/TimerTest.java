/*
 *
 *  Copyright 2026: Robert Winkler
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
package io.github.resilience4j.metrics;

import com.codahale.metrics.MetricRegistry;
import io.github.resilience4j.core.functions.CheckedFunction;
import io.github.resilience4j.core.functions.CheckedRunnable;
import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.test.HelloWorldException;
import io.github.resilience4j.test.HelloWorldService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

class TimerTest {

    private HelloWorldService helloWorldService;

    private Timer timer;

    private MetricRegistry metricRegistry;

    @BeforeEach
    void setUp() {
        metricRegistry = new MetricRegistry();
        timer = Timer.ofMetricRegistry(TimerTest.class.getName(), metricRegistry);
        helloWorldService = mock(HelloWorldService.class);
    }

    @Test
    void shouldDecorateCheckedSupplier() throws Throwable {
        given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");
        CheckedSupplier<String> timedSupplier = Timer
            .decorateCheckedSupplier(timer, helloWorldService::returnHelloWorldWithException);

        String value = timedSupplier.get();

        assertThat(timer.getMetrics().getNumberOfTotalCalls()).isOne();
        assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isOne();
        assertThat(timer.getMetrics().getNumberOfFailedCalls()).isZero();
        assertThat(metricRegistry.getCounters()).hasSize(2);
        assertThat(metricRegistry.getTimers()).hasSize(1);
        assertThat(value).isEqualTo("Hello world");
        then(helloWorldService).should(times(1)).returnHelloWorldWithException();
    }

    @Test
    void shouldDecorateCallable() throws Throwable {
        given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");
        Callable<String> timedSupplier = Timer
            .decorateCallable(timer, helloWorldService::returnHelloWorldWithException);

        String value = timedSupplier.call();

        assertThat(timer.getMetrics().getNumberOfTotalCalls()).isOne();
        assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isOne();
        assertThat(timer.getMetrics().getNumberOfFailedCalls()).isZero();
        assertThat(value).isEqualTo("Hello world");
        then(helloWorldService).should(times(1)).returnHelloWorldWithException();
    }

    @Test
    void shouldExecuteCallable() throws Throwable {
        given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");

        String value = timer
            .executeCallable(helloWorldService::returnHelloWorldWithException);

        assertThat(timer.getMetrics().getNumberOfTotalCalls()).isOne();
        assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isOne();
        assertThat(timer.getMetrics().getNumberOfFailedCalls()).isZero();
        assertThat(value).isEqualTo("Hello world");
        then(helloWorldService).should(times(1)).returnHelloWorldWithException();
    }


    @Test
    void shouldDecorateRunnable() throws Throwable {
        Runnable timedRunnable = Timer.decorateRunnable(timer, helloWorldService::sayHelloWorld);

        timedRunnable.run();

        assertThat(timer.getMetrics().getNumberOfTotalCalls()).isOne();
        assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isOne();
        assertThat(timer.getMetrics().getNumberOfFailedCalls()).isZero();
        then(helloWorldService).should(times(1)).sayHelloWorld();
    }

    @Test
    void shouldExecuteRunnable() throws Throwable {
        timer.executeRunnable(helloWorldService::sayHelloWorld);

        assertThat(timer.getMetrics().getNumberOfTotalCalls()).isOne();
        assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isOne();
        assertThat(timer.getMetrics().getNumberOfFailedCalls()).isZero();
        then(helloWorldService).should(times(1)).sayHelloWorld();
    }

    @Test
    void shouldExecuteCompletionStageSupplier() throws Throwable {
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        Supplier<CompletionStage<String>> completionStageSupplier =
            () -> CompletableFuture.supplyAsync(helloWorldService::returnHelloWorld);
        CompletionStage<String> stringCompletionStage = timer
            .executeCompletionStageSupplier(completionStageSupplier);

        String value = stringCompletionStage.toCompletableFuture().get();
        assertThat(value).isEqualTo("Hello world");
        await().atMost(1, SECONDS)
            .untilAsserted(() -> {
            assertThat(timer.getMetrics().getNumberOfTotalCalls()).isOne();
            assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isOne();
                assertThat(timer.getMetrics().getNumberOfFailedCalls()).isZero();
            });
        then(helloWorldService).should(times(1)).returnHelloWorld();
    }

    @Test
    void shouldExecuteCompletionStageAndReturnWithExceptionAtSyncStage() throws Throwable {
        Supplier<CompletionStage<String>> completionStageSupplier = () -> {
            throw new HelloWorldException();
        };

        assertThatThrownBy(() -> timer.executeCompletionStageSupplier(completionStageSupplier))
            .isInstanceOf(HelloWorldException.class);

        assertThat(timer.getMetrics().getNumberOfTotalCalls()).isOne();
        assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isZero();
        assertThat(timer.getMetrics().getNumberOfFailedCalls()).isOne();
    }


    @Test
    void shouldExecuteCompletionStageAndReturnWithExceptionAtASyncStage() throws Throwable {
        given(helloWorldService.returnHelloWorld()).willThrow(new HelloWorldException());
        Supplier<CompletionStage<String>> completionStageSupplier =
            () -> CompletableFuture.supplyAsync(helloWorldService::returnHelloWorld);
        CompletionStage<String> stringCompletionStage = timer
            .executeCompletionStageSupplier(completionStageSupplier);

        assertThatThrownBy(() -> stringCompletionStage.toCompletableFuture().get())
            .isInstanceOf(ExecutionException.class).hasCause(new HelloWorldException());

        assertThat(timer.getMetrics().getNumberOfTotalCalls()).isOne();
        assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isZero();
        assertThat(timer.getMetrics().getNumberOfFailedCalls()).isOne();
        then(helloWorldService).should().returnHelloWorld();
    }


    @Test
    void shouldDecorateCheckedRunnableAndReturnWithSuccess() throws Throwable {
        CheckedRunnable timedRunnable = Timer
            .decorateCheckedRunnable(timer, helloWorldService::sayHelloWorldWithException);

        timedRunnable.run();

        assertThat(timer.getMetrics().getNumberOfTotalCalls()).isOne();
        assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isOne();
        assertThat(timer.getMetrics().getNumberOfFailedCalls()).isZero();
        then(helloWorldService).should().sayHelloWorldWithException();
    }

    @Test
    void shouldDecorateSupplierAndReturnWithException() throws Throwable {
        given(helloWorldService.returnHelloWorld()).willThrow(new RuntimeException("BAM!"));
        Supplier<String> supplier = Timer
            .decorateSupplier(timer, helloWorldService::returnHelloWorld);

        assertThatThrownBy(supplier::get)
            .isInstanceOf(RuntimeException.class);

        assertThat(timer.getMetrics().getNumberOfTotalCalls()).isOne();
        assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isZero();
        assertThat(timer.getMetrics().getNumberOfFailedCalls()).isOne();
        then(helloWorldService).should(times(1)).returnHelloWorld();

    }

    @Test
    void shouldDecorateSupplier() throws Throwable {
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        Supplier<String> timedSupplier = Timer
            .decorateSupplier(timer, helloWorldService::returnHelloWorld);

        IntStream.range(0, 2).forEach((i) -> timedSupplier.get());

        assertThat(timer.getMetrics().getNumberOfTotalCalls()).isEqualTo(2);
        assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(2);
        assertThat(timer.getMetrics().getNumberOfFailedCalls()).isZero();
        then(helloWorldService).should(times(2)).returnHelloWorld();
    }

    @Test
    void shouldExecuteSupplier() throws Throwable {
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world")
            .willThrow(new IllegalArgumentException("BAM!"));

        IntStream.range(0, 2).forEach((i) -> {
            try {
                timer.executeSupplier(helloWorldService::returnHelloWorld);
            } catch (Exception e) {
                assertThat(e).isInstanceOf(IllegalArgumentException.class);
            }
        });

        assertThat(timer.getMetrics().getNumberOfTotalCalls()).isEqualTo(2);
        assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isOne();
        assertThat(timer.getMetrics().getNumberOfFailedCalls()).isOne();
        then(helloWorldService).should(times(2)).returnHelloWorld();
    }


    @Test
    void shouldDecorateFunctionAndReturnWithSuccess() throws Throwable {
        given(helloWorldService.returnHelloWorldWithName("Tom")).willReturn("Hello world Tom");
        Function<String, String> function = Timer
            .decorateFunction(timer, helloWorldService::returnHelloWorldWithName);

        String result = function.apply("Tom");

        assertThat(result).isEqualTo("Hello world Tom");
        assertThat(timer.getMetrics().getNumberOfTotalCalls()).isOne();
        assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isOne();
        assertThat(timer.getMetrics().getNumberOfFailedCalls()).isZero();
        then(helloWorldService).should().returnHelloWorldWithName("Tom");
    }

    @Test
    void shouldDecorateCheckedFunctionAndReturnWithSuccess() throws Throwable {
        given(helloWorldService.returnHelloWorldWithNameWithException("Tom"))
            .willReturn("Hello world Tom");
        CheckedFunction<String, String> function = Timer.decorateCheckedFunction(timer,
            helloWorldService::returnHelloWorldWithNameWithException);

        String result = function.apply("Tom");

        assertThat(result).isEqualTo("Hello world Tom");
        assertThat(timer.getMetrics().getNumberOfTotalCalls()).isOne();
        assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isOne();
        assertThat(timer.getMetrics().getNumberOfFailedCalls()).isZero();
        then(helloWorldService).should().returnHelloWorldWithNameWithException("Tom");
    }
}
