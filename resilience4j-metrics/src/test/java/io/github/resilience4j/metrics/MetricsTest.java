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
import com.codahale.metrics.Timer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

import io.github.resilience4j.test.HelloWorldService;
import javaslang.collection.Stream;
import javaslang.control.Try;

import static com.codahale.metrics.MetricRegistry.name;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class MetricsTest {

    private HelloWorldService helloWorldService;

    private Timer timer;

    @Before
    public void setUp(){
        MetricRegistry metricRegistry = new MetricRegistry();
        timer = metricRegistry.timer(name("test"));
        helloWorldService = mock(HelloWorldService.class);
    }

    @Test
    public void shouldDecorateCheckedSupplier() throws Throwable {
        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");

        // And measure the time with Metrics
        Try.CheckedSupplier<String> timedSupplier = Metrics.decorateCheckedSupplier(timer, helloWorldService::returnHelloWorldWithException);

        String value = timedSupplier.get();

        assertThat(timer.getCount()).isEqualTo(1);

        assertThat(value).isEqualTo("Hello world");
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorldWithException();
    }

    @Test
    public void shouldDecorateCallable() throws Throwable {
        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");

        // And measure the time with Metrics
        Callable<String> timedSupplier = Metrics.decorateCallable(timer, helloWorldService::returnHelloWorldWithException);

        String value = timedSupplier.call();

        assertThat(timer.getCount()).isEqualTo(1);

        assertThat(value).isEqualTo("Hello world");
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorldWithException();
    }

    @Test
    public void shouldExecuteCallable() throws Throwable {
        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");

        // And measure the time with Metrics
        String value = Metrics.executeCallable(timer, helloWorldService::returnHelloWorldWithException);

        assertThat(timer.getCount()).isEqualTo(1);

        assertThat(value).isEqualTo("Hello world");
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorldWithException();
    }


    @Test
    public void shouldDecorateRunnable() throws Throwable {
        // And measure the time with Metrics
        Runnable timedRunnable = Metrics.decorateRunnable(timer, helloWorldService::sayHelloWorld);

        timedRunnable.run();

        assertThat(timer.getCount()).isEqualTo(1);

        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(1)).sayHelloWorld();
    }

    @Test
    public void shouldDecorateCheckedRunnableAndReturnWithSuccess() throws Throwable {
        // And measure the time with Metrics
        Try.CheckedRunnable timedRunnable = Metrics.decorateCheckedRunnable(timer, helloWorldService::sayHelloWorldWithException);

        timedRunnable.run();

        assertThat(timer.getCount()).isEqualTo(1);
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).sayHelloWorldWithException();
    }

    @Test
    public void shouldDecorateSupplierAndReturnWithException() throws Throwable {
        // And measure the time with Metrics
        BDDMockito.given(helloWorldService.returnHelloWorld()).willThrow(new RuntimeException("BAM!"));

        // And measure the time with Metrics
        Supplier<String> supplier = Metrics.decorateSupplier(timer, helloWorldService::returnHelloWorld);

        Try<String> result = Try.of(supplier::get);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);

        assertThat(timer.getCount()).isEqualTo(1);

        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorld();

    }

    @Test
    public void shouldDecorateSupplier() throws Throwable {
        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        // And measure the time with Metrics
        Supplier<String> timedSupplier = Metrics.decorateSupplier(timer, helloWorldService::returnHelloWorld);

        Stream.range(0,2).forEach((i) -> timedSupplier.get());

        assertThat(timer.getCount()).isEqualTo(2);
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(2)).returnHelloWorld();
    }

    @Test
    public void shouldExecuteSupplier() throws Throwable {
        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        // And measure the time with Metrics
        String result = Metrics.executeSupplier(timer, helloWorldService::returnHelloWorld);

        assertThat(timer.getCount()).isEqualTo(1);

        assertThat(result).isEqualTo("Hello world");

        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorld();
    }


    @Test
    public void shouldDecorateFunctionAndReturnWithSuccess() throws Throwable {
        // Given
        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorldWithName("Tom")).willReturn("Hello world Tom");

        //When
        Function<String, String> function = Metrics.decorateFunction(timer, helloWorldService::returnHelloWorldWithName);

        //Then
        assertThat(function.apply("Tom")).isEqualTo("Hello world Tom");

        assertThat(timer.getCount()).isEqualTo(1);
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorldWithName("Tom");
    }

    @Test
    public void shouldDecorateCheckedFunctionAndReturnWithSuccess() throws Throwable {
        // Given
        // Given the HelloWorldService returns Hello world
        BDDMockito.given(helloWorldService.returnHelloWorldWithNameWithException("Tom")).willReturn("Hello world Tom");

        //When
        Try.CheckedFunction<String, String> function = Metrics.decorateCheckedFunction(timer, helloWorldService::returnHelloWorldWithNameWithException);

        //Then
        assertThat(function.apply("Tom")).isEqualTo("Hello world Tom");

        assertThat(timer.getCount()).isEqualTo(1);
        // Then the helloWorldService should be invoked 1 time
        BDDMockito.then(helloWorldService).should(Mockito.times(1)).returnHelloWorldWithNameWithException("Tom");
    }
}
