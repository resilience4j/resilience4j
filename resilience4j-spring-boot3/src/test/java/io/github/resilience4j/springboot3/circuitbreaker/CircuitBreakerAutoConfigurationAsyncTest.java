/*
 * Copyright 2026 Robert Winkler
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
package io.github.resilience4j.springboot3.circuitbreaker;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerEventDTO;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerEventsEndpointResponse;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerProperties;
import io.github.resilience4j.springboot3.service.test.DummyService;
import io.github.resilience4j.springboot3.service.test.TestApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = TestApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CircuitBreakerAutoConfigurationAsyncTest {

    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
            .options(wireMockConfig().port(8090))
            .build();
    @Autowired
    CircuitBreakerRegistry circuitBreakerRegistry;
    @Autowired
    CircuitBreakerProperties circuitBreakerProperties;
    @Autowired
    DummyService dummyService;

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * The test verifies that a CircuitBreaker instance is created and configured properly when the
     * DummyService is invoked and that the CircuitBreaker records successful and failed calls.
     */
    @Test
    void testCircuitBreakerAutoConfigurationAsync()
        throws IOException, ExecutionException, InterruptedException {
        assertThat(circuitBreakerRegistry).isNotNull();
        assertThat(circuitBreakerProperties).isNotNull();
        List<CircuitBreakerEventDTO> circuitBreakerEventsBefore = getCircuitBreakersEvents();
        List<CircuitBreakerEventDTO> circuitBreakerEventsForABefore = getCircuitBreakerEvents(DummyService.BACKEND);

        try {
            dummyService.doSomethingAsync(true);
        } catch (IOException ex) {
            // Do nothing. The IOException is recorded by the CircuitBreaker as part of the setRecordFailurePredicate as a failure.
        }
        // The invocation is recorded by the CircuitBreaker as a success.
        final CompletableFuture<String> stringCompletionStage = dummyService
            .doSomethingAsync(false);
        assertThat(stringCompletionStage.get()).isEqualTo("Test result");

        // expect circuitbreaker-event actuator endpoint recorded both events
        assertThat(getCircuitBreakersEvents())
            .hasSize(circuitBreakerEventsBefore.size() + 2);
        assertThat(getCircuitBreakerEvents(DummyService.BACKEND))
            .hasSize(circuitBreakerEventsForABefore.size() + 2);
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
