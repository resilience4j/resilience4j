/*
 * Copyright 2025 Robert Winkler, Artur Havliukovskyi
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
package io.github.resilience4j.springboot.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.springboot.circuitbreaker.autoconfigure.CircuitBreakerProperties;
import io.github.resilience4j.spring6.circuitbreaker.configure.CircuitBreakerAspect;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerEventDTO;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerEventsEndpointResponse;
import io.github.resilience4j.springboot.service.test.DummyService;
import io.github.resilience4j.springboot.service.test.ReactiveDummyService;
import io.github.resilience4j.springboot.service.test.TestApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = TestApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestRestTemplate
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

        List<CircuitBreakerEventDTO> circuitBreakerEventsBefore = getCircuitBreakersEvents();
        List<CircuitBreakerEventDTO> circuitBreakerEventsForBBefore = getCircuitBreakerEvents(ReactiveDummyService.BACKEND);

        try {
            reactiveDummyService.doSomethingFlowable(true).blockingSubscribe(String::toUpperCase,
                throwable -> System.out.println("Exception received:" + throwable.getMessage()));
        } catch (Exception ex) {
            // Do nothing. The IOException is recorded by the CircuitBreaker as part of the setRecordFailurePredicate as a failure.
        }
        // The invocation is recorded by the CircuitBreaker as a success.
        reactiveDummyService.doSomethingFlowable(false).blockingSubscribe(String::toUpperCase,
            throwable -> System.out.println("Exception received:" + throwable.getMessage()));

        // expect circuitbreaker-event actuator endpoint recorded both events
        assertThat(getCircuitBreakersEvents())
            .hasSize(circuitBreakerEventsBefore.size() + 2);
        assertThat(getCircuitBreakerEvents(ReactiveDummyService.BACKEND))
            .hasSize(circuitBreakerEventsForBBefore.size() + 2);

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

    private List<CircuitBreakerEventDTO> getCircuitBreakersEvents() {
        return getEventsFrom("/actuator/circuitbreakerevents");
    }

    private List<CircuitBreakerEventDTO> getCircuitBreakerEvents(String name) {
        return getEventsFrom("/actuator/circuitbreakerevents/" + name);
    }

    private List<CircuitBreakerEventDTO> getEventsFrom(String path) {
        return restTemplate.getForEntity(path, CircuitBreakerEventsEndpointResponse.class)
            .getBody().getCircuitBreakerEvents();
    }
}
