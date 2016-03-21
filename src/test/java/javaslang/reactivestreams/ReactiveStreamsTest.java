/*
 *
 *  Copyright 2015 Robert Winkler
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
package javaslang.reactivestreams;

import javaslang.circuitbreaker.CircuitBreaker;
import javaslang.circuitbreaker.CircuitBreakerConfig;
import javaslang.circuitbreaker.CircuitBreakerOpenException;
import javaslang.circuitbreaker.CircuitBreakerRegistry;
import javaslang.retry.HelloWorldService;
import javaslang.retry.Retry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.test.TestSubscriber;

import javax.xml.ws.WebServiceException;
import java.time.Duration;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class ReactiveStreamsTest {

    private static Logger LOG = LoggerFactory.getLogger(ReactiveStreamsTest.class);

    private CircuitBreakerRegistry circuitBreakerRegistry;
    private HelloWorldService helloWorldService;

    @Before
    public void setUp(){
        helloWorldService = mock(HelloWorldService.class);
        circuitBreakerRegistry = CircuitBreakerRegistry.of(new CircuitBreakerConfig.Builder()
                .failureRateThreshold(50)
                .ringBufferSizeInClosedState(2)
                .ringBufferSizeInHalfOpenState(2)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .build());
    }

    @Test
    public void shouldReturnSuccessfullyAfterSecondAttempt() {
        // Given the HelloWorldService throws an exception
        given(helloWorldService.returnHelloWorld()).willThrow(new WebServiceException("BAM!")).willReturn("Hello world");

        // Create a Retry with default configuration
        Retry retryContext = Retry.ofDefaults();
        // Decorate the supplier of the HelloWorldService with Retry functionality
        Supplier<String> retryableSupplier = Retry.decorateSupplier(helloWorldService::returnHelloWorld, retryContext);

        TestSubscriber<String> testSubscriber = new TestSubscriber<>();

        // When
        Publisher<String> monoPublisher = Mono.fromCallable(retryableSupplier::get)
                .map(value -> value + " from reactive streams");

        testSubscriber.bindTo(monoPublisher)
                .assertComplete()
                .assertValues("Hello world from reactive streams");

        // Then the helloWorldService should be invoked 2 times
        BDDMockito.then(helloWorldService).should(times(2)).returnHelloWorld();
    }

    @Test
    public void shouldReturnFailureWithCircuitBreakerOpenException() {
        // Given
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        // CircuitBreaker is initially CLOSED
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        circuitBreaker.recordFailure(new RuntimeException());
        // CircuitBreaker is still CLOSED, because at least two calls must be evaluated
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        circuitBreaker.recordFailure(new RuntimeException());
        // CircuitBreaker is OPEN, because failure rate is above 50%
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Decorate the supplier of the HelloWorldService with CircuitBreaker functionality
        Supplier<String> supplier = CircuitBreaker.decorateSupplier(helloWorldService::returnHelloWorld, circuitBreaker);

        TestSubscriber<String> testSubscriber = new TestSubscriber<>();

        //When
        Publisher<String> monoPublisher = Mono.fromCallable(supplier::get)
                .map(value -> value + " from reactive streams");

        //Then
        testSubscriber.bindTo(monoPublisher)
                .assertError(CircuitBreakerOpenException.class);
    }

    /*
    @Test
    public void testStreamCircuitBreaker() {
        // Given the HelloWorldService throws an exception
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        // Given
        TestSubscriber<String> testSubscriber = new TestSubscriber<>();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");

        Publisher<String> monoPublisher = Mono.fromCallable(helloWorldService::returnHelloWorld);

        Stream.from(monoPublisher).as(s -> new StreamCircuitBreaker<>(s, circuitBreaker))
            .subscribe(testSubscriber);

        testSubscriber.assertValueCount(1).assertValues("Hello world");
    }
    */
}
