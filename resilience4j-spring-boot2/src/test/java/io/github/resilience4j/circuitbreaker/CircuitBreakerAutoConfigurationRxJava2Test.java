/*
 * Copyright 2017 Robert Winkler
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
package io.github.resilience4j.circuitbreaker;

import io.github.resilience4j.circuitbreaker.autoconfigure.CircuitBreakerProperties;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerAspect;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerEventsEndpointResponse;
import io.github.resilience4j.service.test.DummyService;
import io.github.resilience4j.service.test.ReactiveDummyService;
import io.github.resilience4j.service.test.TestApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = TestApplication.class)
public class CircuitBreakerAutoConfigurationRxJava2Test {

    @Autowired
    CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    CircuitBreakerProperties circuitBreakerProperties;

    @Autowired
    CircuitBreakerAspect circuitBreakerAspect;

    @Autowired
    DummyService dummyService;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ReactiveDummyService reactiveDummyService;


    /**
     * The test verifies that a CircuitBreaker instance is created and configured properly when the
     * DummyService is invoked and that the CircuitBreaker records successful and failed calls.
     */
    @Test
    public void testCircuitBreakerAutoConfigurationReactiveRxJava2() throws IOException {
        assertThat(circuitBreakerRegistry).isNotNull();
        assertThat(circuitBreakerProperties).isNotNull();
        CircuitBreakerEventsEndpointResponse circuitBreakerEventListBefore = circuitBreakerEvents(
            "/actuator/circuitbreakerevents");
        CircuitBreakerEventsEndpointResponse circuitBreakerEventListForBBefore = circuitBreakerEvents(
            "/actuator/circuitbreakerevents/backendB");

        try {
            reactiveDummyService.doSomethingFlowable(true).blockingSubscribe(String::toUpperCase,
                throwable -> System.out.println("Exception received:" + throwable.getMessage()));
        } catch (Exception ex) {
            // Do nothing. The IOException is recorded by the CircuitBreaker as part of the setRecordFailurePredicate as a failure.
        }
        // The invocation is recorded by the CircuitBreaker as a success.
        reactiveDummyService.doSomethingFlowable(false).blockingSubscribe(String::toUpperCase,
            throwable -> System.out.println("Exception received:" + throwable.getMessage()));

        CircuitBreaker circuitBreaker = circuitBreakerRegistry
            .circuitBreaker(ReactiveDummyService.BACKEND);

        assertThat(circuitBreaker).isNotNull();

        // expect circuitbreaker-event actuator endpoint recorded both events
        CircuitBreakerEventsEndpointResponse circuitBreakerEventList = circuitBreakerEvents(
            "/actuator/circuitbreakerevents");
        assertThat(circuitBreakerEventList.getCircuitBreakerEvents())
            .hasSize(circuitBreakerEventListBefore.getCircuitBreakerEvents().size() + 2);

        circuitBreakerEventList = circuitBreakerEvents("/actuator/circuitbreakerevents/backendB");
        assertThat(circuitBreakerEventList.getCircuitBreakerEvents())
            .hasSize(circuitBreakerEventListForBBefore.getCircuitBreakerEvents().size() + 2);

        // Observable test
        try {
            reactiveDummyService.doSomethingObservable(true)
                .blockingSubscribe(String::toUpperCase, Throwable::getCause);
        } catch (IOException ex) {
            // Do nothing. The IOException is recorded by the CircuitBreaker as part of the setRecordFailurePredicate as a failure.
        }
        // The invocation is recorded by the CircuitBreaker as a success.
        reactiveDummyService.doSomethingObservable(false)
            .blockingSubscribe(String::toUpperCase, Throwable::getCause);

        // Maybe test
        try {
            reactiveDummyService.doSomethingMaybe(true).blockingGet("goo");
        } catch (Exception ex) {
            // Do nothing. The IOException is recorded by the CircuitBreaker as part of the setRecordFailurePredicate as a failure.
        }
        // The invocation is recorded by the CircuitBreaker as a success.
        reactiveDummyService.doSomethingMaybe(false).blockingGet();

        // single test
        try {
            reactiveDummyService.doSomethingSingle(true).blockingGet();
        } catch (Exception ex) {
            // Do nothing. The IOException is recorded by the CircuitBreaker as part of the setRecordFailurePredicate as a failure.
        }
        // The invocation is recorded by the CircuitBreaker as a success.
        reactiveDummyService.doSomethingSingle(false).blockingGet();

        // Completable test

        try {
            reactiveDummyService.doSomethingCompletable(true).blockingAwait();
        } catch (Exception ex) {
            // Do nothing. The IOException is recorded by the CircuitBreaker as part of the setRecordFailurePredicate as a failure.
        }
        // The invocation is recorded by the CircuitBreaker as a success.
        reactiveDummyService.doSomethingCompletable(false).blockingAwait();
    }

    private CircuitBreakerEventsEndpointResponse circuitBreakerEvents(String s) {
        return restTemplate.getForEntity(s, CircuitBreakerEventsEndpointResponse.class).getBody();
    }
}
