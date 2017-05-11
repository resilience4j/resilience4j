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
import com.codahale.metrics.SharedMetricRegistries;
import io.github.resilience4j.test.HelloWorldService;
import javaslang.collection.Stream;
import javaslang.control.Try;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class TimerTest {

    private HelloWorldService helloWorldService;

    private Timer timer;

    private MetricRegistry metricRegistry;

    @Before
    public void setUp(){
        metricRegistry = SharedMetricRegistries.getOrCreate("MyRegistry");
        timer = Timer.ofMetricRegistry(TimerTest.class.getName(), metricRegistry);
        helloWorldService = mock(HelloWorldService.class);
    }

    @Test
    public void shouldDecorateCheckedSupplier() throws Throwable {
        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");

        // And measure the time with Metrics
        Try.CheckedSupplier<String> timedSupplier = Timer.decorateCheckedSupplier(timer, helloWorldService::returnHelloWorldWithException);

        String value = timedSupplier.get();

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

        // And measure the time with Metrics
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

        // And measure the time with Metrics
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
        // And measure the time with Metrics
        Runnable timedRunnable = Timer.decorateRunnable(timer, helloWorldService::sayHelloWorld);

        timedRunnable.run();

        assertThat(timer.getMetrics().getNumberOfTotalCalls()).isEqualTo(1);
        assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(timer.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);

        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(1)).sayHelloWorld();
    }

    @Test
    public void shouldDecorateCheckedRunnableAndReturnWithSuccess() throws Throwable {
        // And measure the time with Metrics
        Try.CheckedRunnable timedRunnable = Timer.decorateCheckedRunnable(timer, helloWorldService::sayHelloWorldWithException);

        timedRunnable.run();

        assertThat(timer.getMetrics().getNumberOfTotalCalls()).isEqualTo(1);
        assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(timer.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).sayHelloWorldWithException();
    }

    @Test
    public void shouldDecorateSupplierAndReturnWithException() throws Throwable {
        // And measure the time with Metrics
        BDDMockito.given(helloWorldService.returnHelloWorld()).willThrow(new RuntimeException("BAM!"));

        // And measure the time with Metrics
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

        // And measure the time with Metrics
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

        // And measure the time with Metrics
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
        Try.CheckedFunction<String, String> function = Timer.decorateCheckedFunction(timer, helloWorldService::returnHelloWorldWithNameWithException);

        //Then
        assertThat(function.apply("Tom")).isEqualTo("Hello world Tom");

        assertThat(timer.getMetrics().getNumberOfTotalCalls()).isEqualTo(1);
        assertThat(timer.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(timer.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorldWithNameWithException("Tom");
    }
}
