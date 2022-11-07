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
package io.github.resilience4j.micrometer;

import com.jayway.awaitility.Awaitility;
import io.github.resilience4j.core.functions.CheckedFunction;
import io.github.resilience4j.core.functions.CheckedRunnable;
import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.test.HelloWorldException;
import io.github.resilience4j.test.HelloWorldService;
import io.micrometer.observation.Observation;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.jayway.awaitility.Awaitility.await;
import static io.micrometer.observation.tck.TestObservationRegistryAssert.assertThat;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class ObservationsTest {

    private HelloWorldService helloWorldService;

    private TestObservationRegistry observationRegistry;

    private Observation observation;

    @Before
    public void setUp() {
        observationRegistry = TestObservationRegistry.create();
        observation = Observations.ofObservationRegistry(ObservationsTest.class.getName(), observationRegistry);
        helloWorldService = mock(HelloWorldService.class);
    }

    @Test
    public void shouldDecorateCheckedSupplier() throws Throwable {
        given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");
        CheckedSupplier<String> timedSupplier = Observations
            .decorateCheckedSupplier(observation, helloWorldService::returnHelloWorldWithException);

        String value = timedSupplier.get();

        assertThatObservationWasStartedAndFinishedWithoutErrors();
        assertThat(value).isEqualTo("Hello world");
        then(helloWorldService).should(times(1)).returnHelloWorldWithException();
    }

    @Test
    public void shouldDecorateCallable() throws Throwable {
        given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");
        Callable<String> timedSupplier = Observations
            .decorateCallable(observation, helloWorldService::returnHelloWorldWithException);

        String value = timedSupplier.call();

        assertThatObservationWasStartedAndFinishedWithoutErrors();
        assertThat(value).isEqualTo("Hello world");
        then(helloWorldService).should(times(1)).returnHelloWorldWithException();
    }

    @Test
    public void shouldExecuteCallable() throws Throwable {
        given(helloWorldService.returnHelloWorldWithException()).willReturn("Hello world");

        String value = Observations
            .executeCallable(observation, helloWorldService::returnHelloWorldWithException);

        assertThatObservationWasStartedAndFinishedWithoutErrors();
        assertThat(value).isEqualTo("Hello world");
        then(helloWorldService).should(times(1)).returnHelloWorldWithException();
    }


    @Test
    public void shouldDecorateRunnable() throws Throwable {
        Runnable timedRunnable = Observations.decorateRunnable(observation, helloWorldService::sayHelloWorld);

        timedRunnable.run();

        assertThatObservationWasStartedAndFinishedWithoutErrors();
        then(helloWorldService).should(times(1)).sayHelloWorld();
    }

    @Test
    public void shouldExecuteRunnable() throws Throwable {
        Observations.executeRunnable(observation, helloWorldService::sayHelloWorld);

        assertThatObservationWasStartedAndFinishedWithoutErrors();
        then(helloWorldService).should(times(1)).sayHelloWorld();
    }

    @Test
    public void shouldExecuteCompletionStageSupplier() throws Throwable {
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        Supplier<CompletionStage<String>> completionStageSupplier =
            () -> CompletableFuture.supplyAsync(helloWorldService::returnHelloWorld);
        CompletionStage<String> stringCompletionStage = Observations
            .executeCompletionStageSupplier(observation, completionStageSupplier);

        String value = stringCompletionStage.toCompletableFuture().get();
        assertThat(value).isEqualTo("Hello world");
        await().atMost(1, SECONDS)
            .until(() -> {
                assertThatObservationWasStartedAndFinishedWithoutErrors();
            });
        then(helloWorldService).should(times(1)).returnHelloWorld();
    }

    @Test
    public void shouldExecuteCompletionStageAndReturnWithExceptionAtSyncStage() throws Throwable {
        Supplier<CompletionStage<String>> completionStageSupplier = () -> {
            throw new HelloWorldException();
        };

        assertThatThrownBy(() -> Observations.executeCompletionStageSupplier(observation, completionStageSupplier))
            .isInstanceOf(HelloWorldException.class);

        assertThat(observationRegistry)
            .hasSingleObservationThat()
            .hasNameEqualTo(ObservationsTest.class.getName())
            .hasBeenStarted()
            .hasBeenStopped()
            .assertThatError()
            .isInstanceOf(HelloWorldException.class);
    }


    @Test
    public void shouldExecuteCompletionStageAndReturnWithExceptionAtASyncStage() throws Throwable {
        given(helloWorldService.returnHelloWorld()).willThrow(new HelloWorldException());
        Supplier<CompletionStage<String>> completionStageSupplier =
            () -> CompletableFuture.supplyAsync(helloWorldService::returnHelloWorld);
        CompletionStage<String> stringCompletionStage = Observations
            .executeCompletionStageSupplier(observation, completionStageSupplier);

        assertThatThrownBy(() -> stringCompletionStage.toCompletableFuture().get())
            .isInstanceOf(ExecutionException.class).hasCause(new HelloWorldException());

        Awaitility.await()
            .until(() -> assertThat(observationRegistry)
                .hasSingleObservationThat()
                .hasNameEqualTo(ObservationsTest.class.getName())
                .hasBeenStarted()
                .hasBeenStopped()
                .assertThatError()
                .isInstanceOf(CompletionException.class).hasCause(new HelloWorldException()));
        then(helloWorldService).should().returnHelloWorld();
    }


    @Test
    public void shouldDecorateCheckedRunnableAndReturnWithSuccess() throws Throwable {
        CheckedRunnable timedRunnable = Observations
            .decorateCheckedRunnable(observation, helloWorldService::sayHelloWorldWithException);

        timedRunnable.run();

        assertThatObservationWasStartedAndFinishedWithoutErrors();
        then(helloWorldService).should().sayHelloWorldWithException();
    }

    @Test
    public void shouldDecorateSupplierAndReturnWithException() throws Throwable {
        given(helloWorldService.returnHelloWorld()).willThrow(new RuntimeException("BAM!"));
        Supplier<String> supplier = Observations
            .decorateSupplier(observation, helloWorldService::returnHelloWorld);

        Try<String> result = Try.of(supplier::get);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(RuntimeException.class);
        assertThat(observationRegistry)
            .hasSingleObservationThat()
            .hasNameEqualTo(ObservationsTest.class.getName())
            .hasBeenStarted()
            .hasBeenStopped()
            .assertThatError()
            .isInstanceOf(RuntimeException.class);
        then(helloWorldService).should(times(1)).returnHelloWorld();

    }

    @Test
    public void shouldDecorateSupplier() throws Throwable {
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        Supplier<String> timedSupplier = Observations
            .decorateSupplier(observation, helloWorldService::returnHelloWorld);

        timedSupplier.get();

        assertThatObservationWasStartedAndFinishedWithoutErrors();
        then(helloWorldService).should().returnHelloWorld();
    }

    @Test
    public void shouldExecuteSupplier() throws Throwable {
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world")
            .willThrow(new IllegalArgumentException("BAM!"));

        try {
            Observations.executeSupplier(observation, helloWorldService::returnHelloWorld);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalArgumentException.class);
        }

       assertThatObservationWasStartedAndFinishedWithoutErrors();
        then(helloWorldService).should().returnHelloWorld();
    }


    @Test
    public void shouldDecorateFunctionAndReturnWithSuccess() throws Throwable {
        given(helloWorldService.returnHelloWorldWithName("Tom")).willReturn("Hello world Tom");
        Function<String, String> function = Observations
            .decorateFunction(observation, helloWorldService::returnHelloWorldWithName);

        String result = function.apply("Tom");

        assertThat(result).isEqualTo("Hello world Tom");
        assertThatObservationWasStartedAndFinishedWithoutErrors();
        then(helloWorldService).should().returnHelloWorldWithName("Tom");
    }

    @Test
    public void shouldDecorateCheckedFunctionAndReturnWithSuccess() throws Throwable {
        given(helloWorldService.returnHelloWorldWithNameWithException("Tom"))
            .willReturn("Hello world Tom");
        CheckedFunction<String, String> function = Observations.decorateCheckedFunction(observation,
            helloWorldService::returnHelloWorldWithNameWithException);

        String result = function.apply("Tom");

        assertThat(result).isEqualTo("Hello world Tom");
        assertThatObservationWasStartedAndFinishedWithoutErrors();
        then(helloWorldService).should().returnHelloWorldWithNameWithException("Tom");
    }

    private void assertThatObservationWasStartedAndFinishedWithoutErrors() {
        assertThat(observationRegistry)
            .hasSingleObservationThat()
            .hasNameEqualTo(ObservationsTest.class.getName())
            .hasBeenStarted()
            .hasBeenStopped()
            .assertThatError()
            .isNull();
    }
}
