/*
 *
 *  Copyright 2017: Robert Winkler
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

import io.github.resilience4j.test.HelloWorldException;
import io.github.resilience4j.test.HelloWorldService;
import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedRunnable;
import io.vavr.collection.Stream;
import io.vavr.control.Try;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class TimerTest {

    private HelloWorldService helloWorldService;

    private Timer timer;

    private MetricRegistry metricRegistry;

    @Before
    public void setUp(){
        metricRegistry = new MetricRegistry();
        timer = Timer.ofMetricRegistry(TimerTest.class.getName(), metricRegistry);
        helloWorldService = mock(HelloWorldService.class);
    }

    @Test
    public void shouldDecorateCheckedSupplier() throws Throwable {
        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");

        // And measure the call with  a Timer
        CheckedFunction0<String> timedSupplier = Timer.decorateCheckedSupplier(timer, helloWorldService::returnHelloWorldWithException);

        String value = timedSupplier.apply();

        assertThat(timer.getMetrics().getNumberOfTotalCalls()).isEqualTo(1);
        assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(timer.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);

        assertThat(metricRegistry.getCounters().size()).isEqualTo(2);
        assertThat(metricRegistry.getTimers().size()).isEqualTo(1);

        assertThat(value).isEqualTo("Hello world");
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorldWithException();
    }

    @Test
    public void shouldDecorateCallable() throws Throwable {
        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");

        // And measure the call with a Timer
        Callable<String> timedSupplier = Timer.decorateCallable(timer, helloWorldService::returnHelloWorldWithException);

        String value = timedSupplier.call();

        assertThat(timer.getMetrics().getNumberOfTotalCalls()).isEqualTo(1);
        assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(timer.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);

        assertThat(value).isEqualTo("Hello world");
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorldWithException();
    }

    @Test
    public void shouldExecuteCallable() throws Throwable {
        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");

        // And measure the call with a Timer
        String value = timer.executeCallable(helloWorldService::returnHelloWorldWithException);

        assertThat(timer.getMetrics().getNumberOfTotalCalls()).isEqualTo(1);
        assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(timer.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);

        assertThat(value).isEqualTo("Hello world");
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorldWithException();
    }


    @Test
    public void shouldDecorateRunnable() throws Throwable {
        // And measure the call with a Timer
        Runnable timedRunnable = Timer.decorateRunnable(timer, helloWorldService::sayHelloWorld);

        timedRunnable.run();

        assertThat(timer.getMetrics().getNumberOfTotalCalls()).isEqualTo(1);
        assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(timer.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);

        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(1)).sayHelloWorld();
    }

    @Test
    public void shouldExecuteRunnable() throws Throwable {
        // And measure the call with a Timer
        timer.executeRunnable(helloWorldService::sayHelloWorld);

        assertThat(timer.getMetrics().getNumberOfTotalCalls()).isEqualTo(1);
        assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(timer.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);

        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(1)).sayHelloWorld();
    }

    @Test
    public void shouldExecuteCompletionStageSupplier() throws Throwable {
        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        // And measure the call with a Timer
        Supplier<CompletionStage<String>> completionStageSupplier =
                () -> CompletableFuture.supplyAsync(helloWorldService::returnHelloWorld);

        CompletionStage<String> stringCompletionStage = timer.executeCompletionStageSupplier(completionStageSupplier);
        String value = stringCompletionStage.toCompletableFuture().get();

        assertThat(value).isEqualTo("Hello world");

        await().atMost(1, SECONDS)
                .until(() -> {
                    assertThat(timer.getMetrics().getNumberOfTotalCalls()).isEqualTo(1);
                    assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
                    assertThat(timer.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);
                });

        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorld();
    }

    @Test
    public void shouldExecuteCompletionStageAndReturnWithExceptionAtSyncStage() throws Throwable {

        Supplier<CompletionStage<String>> completionStageSupplier = () -> {
            throw new HelloWorldException();
        };

        Assertions.assertThatThrownBy(() -> timer.executeCompletionStageSupplier(completionStageSupplier))
                .isInstanceOf(HelloWorldException.class);

        assertThat(timer.getMetrics().getNumberOfTotalCalls()).isEqualTo(1);
        assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(timer.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
    }


    @Test
    public void shouldExecuteCompletionStageAndReturnWithExceptionAtASyncStage() throws Throwable {
        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorld()).willThrow(new HelloWorldException());
        // And measure the call with a Timer
        Supplier<CompletionStage<String>> completionStageSupplier =
                () -> CompletableFuture.supplyAsync(helloWorldService::returnHelloWorld);

        CompletionStage<String> stringCompletionStage = timer.executeCompletionStageSupplier(completionStageSupplier);

        Assertions.assertThatThrownBy(() -> stringCompletionStage.toCompletableFuture().get())
                .isInstanceOf(ExecutionException.class).hasCause(new HelloWorldException());

        assertThat(timer.getMetrics().getNumberOfTotalCalls()).isEqualTo(1);
        assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(timer.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);

        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorld();
    }


    @Test
    public void shouldDecorateCheckedRunnableAndReturnWithSuccess() throws Throwable {
        // And measure the call with a Timer
        CheckedRunnable timedRunnable = Timer.decorateCheckedRunnable(timer, helloWorldService::sayHelloWorldWithException);

        timedRunnable.run();

        assertThat(timer.getMetrics().getNumberOfTotalCalls()).isEqualTo(1);
        assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(timer.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).sayHelloWorldWithException();
    }

    @Test
    public void shouldDecorateSupplierAndReturnWithException() throws Throwable {
        BDDMockito.given(helloWorldService.returnHelloWorld()).willThrow(new RuntimeException("BAM!"));

        // And measure the call with a Timer
        Supplier<String> supplier = Timer.decorateSupplier(timer, helloWorldService::returnHelloWorld);

        Try<String> result = Try.of(supplier::get);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);

        assertThat(timer.getMetrics().getNumberOfTotalCalls()).isEqualTo(1);
        assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(timer.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);

        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorld();

    }

    @Test
    public void shouldDecorateSupplier() throws Throwable {
        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        // And measure the call with a Timer
        Supplier<String> timedSupplier = Timer.decorateSupplier(timer, helloWorldService::returnHelloWorld);

        Stream.range(0,2).forEach((i) -> timedSupplier.get());

        assertThat(timer.getMetrics().getNumberOfTotalCalls()).isEqualTo(2);
        assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(2);
        assertThat(timer.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(2)).returnHelloWorld();
    }

    @Test
    public void shouldExecuteSupplier() throws Throwable {
        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world").willThrow(new IllegalArgumentException("BAM!"));

        // And measure the call with a Timer
        Stream.range(0,2).forEach((i) -> {
            try{
                timer.executeSupplier(helloWorldService::returnHelloWorld);
            }catch (Exception e){
                Assertions.assertThat(e).isInstanceOf(IllegalArgumentException.class);
            }
        });

        assertThat(timer.getMetrics().getNumberOfTotalCalls()).isEqualTo(2);
        assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(timer.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);

        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(2)).returnHelloWorld();
    }


    @Test
    public void shouldDecorateFunctionAndReturnWithSuccess() throws Throwable {
        // Given
        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorldWithName("Tom")).willReturn("Hello world Tom");

        //When
        Function<String, String> function = Timer.decorateFunction(timer, helloWorldService::returnHelloWorldWithName);

        //Then
        assertThat(function.apply("Tom")).isEqualTo("Hello world Tom");

        assertThat(timer.getMetrics().getNumberOfTotalCalls()).isEqualTo(1);
        assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(timer.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorldWithName("Tom");
    }

    @Test
    public void shouldDecorateCheckedFunctionAndReturnWithSuccess() throws Throwable {
        // Given
        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorldWithNameWithException("Tom")).willReturn("Hello world Tom");

        //When
        CheckedFunction1<String, String> function = Timer.decorateCheckedFunction(timer, helloWorldService::returnHelloWorldWithNameWithException);

        //Then
        assertThat(function.apply("Tom")).isEqualTo("Hello world Tom");

        assertThat(timer.getMetrics().getNumberOfTotalCalls()).isEqualTo(1);
        assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(timer.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorldWithNameWithException("Tom");
    }
}
