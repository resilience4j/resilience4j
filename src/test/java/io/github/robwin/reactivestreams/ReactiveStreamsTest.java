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
package io.github.robwin.reactivestreams;

import io.github.robwin.circuitbreaker.CircuitBreaker;
import io.github.robwin.circuitbreaker.CircuitBreakerConfig;
import io.github.robwin.circuitbreaker.CircuitBreakerOpenException;
import io.github.robwin.circuitbreaker.CircuitBreakerRegistry;
import io.github.robwin.retry.HelloWorldService;
import io.github.robwin.retry.Retry;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.Environment;
import reactor.rx.Streams;

import javax.xml.ws.WebServiceException;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class ReactiveStreamsTest {

    private static Logger LOG = LoggerFactory.getLogger(ReactiveStreamsTest.class);

    private CircuitBreakerRegistry circuitBreakerRegistry;
    private HelloWorldService helloWorldService;

    @BeforeClass
    public static void initialize() {
        Environment.initialize();
    }

    @Before
    public void setUp(){
        helloWorldService = mock(HelloWorldService.class);
        circuitBreakerRegistry = CircuitBreakerRegistry.of(new CircuitBreakerConfig.Builder()
                .maxFailures(1)
                .waitInterval(1000)
                .build());
    }

    @Test
    public void shouldReturnSuccessfullyAfterSecondAttempt() {
        // Given the HelloWorldService throws an exception
        given(helloWorldService.returnHelloWorld()).willThrow(new WebServiceException("BAM!")).willReturn("Hello world");

        // Create a Retry with default configuration
        Retry retryContext = Retry.ofDefaults();
        // Decorate the supplier of the HelloWorldService with Retry functionality
        Supplier<String> retryableSupplier = Retry.retryableSupplier(helloWorldService::returnHelloWorld, retryContext);

        // When
        Streams.generate(retryableSupplier::get)
                .map(value -> value + " from reactive streams")
                .consume(value -> {
                    LOG.info(value);
                    assertThat(value).isEqualTo("Hello world from reactive streams");
                });

        // Then the helloWorldService should be invoked 2 times
        BDDMockito.then(helloWorldService).should(times(2)).returnHelloWorld();
    }

    @Test
    public void shouldReturnFailureWithCircuitBreakerOpenException() {
        // Given
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("testName");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED); // CircuitBreaker is initially CLOSED
        circuitBreaker.recordFailure(new RuntimeException());
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED); // CircuitBreaker is still CLOSED, because 1 failure is allowed
        circuitBreaker.recordFailure(new RuntimeException());
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN); // CircuitBreaker is OPEN, because maxFailures > 1

        // Decorate the supplier of the HelloWorldService with CircuitBreaker functionality
        Supplier<String> supplier = CircuitBreaker.decorateSupplier(helloWorldService::returnHelloWorld, circuitBreaker);

        //When
        Streams.generate(supplier::get)
                .map(value -> value + " from reactive streams")
                .consume(value -> {
                    LOG.info(value);
                }, exception -> {
                    LOG.info("Exception handled: " + exception.toString());
                    assertThat(exception).isInstanceOf(CircuitBreakerOpenException.class);
                });
    }

}
