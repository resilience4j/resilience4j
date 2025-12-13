/*
 * Copyright 2025 Mahmoud Romeh, Artur Havliukovskyi
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
package io.github.resilience4j.springboot.retry;

import io.github.resilience4j.springboot.circuitbreaker.IgnoredException;
import io.github.resilience4j.common.retry.monitoring.endpoint.RetryEventsEndpointResponse;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.springboot.retry.autoconfigure.RetryProperties;
import io.github.resilience4j.spring6.retry.configure.RetryAspect;
import io.github.resilience4j.springboot.service.test.TestApplication;
import io.github.resilience4j.springboot.service.test.retry.ReactiveRetryDummyService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = TestApplication.class)
@AutoConfigureTestRestTemplate
public class RetryAutoConfigurationReactorTest {

    @Autowired
    RetryRegistry retryRegistry;

    @Autowired
    RetryProperties retryProperties;

    @Autowired
    RetryAspect retryAspect;

    @Autowired
    ReactiveRetryDummyService retryDummyService;

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * The test verifies that a Retry instance is created and configured properly when the
     * RetryReactiveDummyService is invoked and that the Retry logic is properly handled
     */
    @Test
    public void testRetryAutoConfigurationReactor() throws IOException {
        assertThat(retryRegistry).isNotNull();
        assertThat(retryProperties).isNotNull();

        RetryEventsEndpointResponse retryEventListBefore = retryEventListBody(
            "/actuator/retryevents");
        RetryEventsEndpointResponse retryEventListForCBefore =
            retryEventListBody("/actuator/retryevents/" + ReactiveRetryDummyService.BACKEND_C);

        try {
            retryDummyService.doSomethingFlux(true)
                .doOnError(throwable -> System.out.println("Exception received:" + throwable.getMessage()))
                .blockLast();
        } catch (IllegalArgumentException ex) {
            // Do nothing. The IllegalArgumentException is recorded by the retry as it is one of failure exceptions
        }
        // The invocation is recorded by the CircuitBreaker as a success.
        retryDummyService.doSomethingFlux(false)
            .doOnError(throwable -> System.out.println("Exception received:" + throwable.getMessage()))
            .blockLast();

        Retry retry = retryRegistry.retry(ReactiveRetryDummyService.BACKEND_C);
        assertThat(retry).isNotNull();

        // expect retry is configured as defined in application.yml
        assertThat(retry.getRetryConfig().getMaxAttempts()).isEqualTo(3);
        assertThat(retry.getName()).isEqualTo(ReactiveRetryDummyService.BACKEND_C);
        assertThat(retry.getRetryConfig().getExceptionPredicate().test(new IOException())).isTrue();

        // expect retry-event actuator endpoint recorded both events
        RetryEventsEndpointResponse retryEventList = retryEventListBody("/actuator/retryevents");
        assertThat(retryEventList.getRetryEvents())
            .hasSize(retryEventListBefore.getRetryEvents().size() + 3);

        retryEventList = retryEventListBody("/actuator/retryevents/" + ReactiveRetryDummyService.BACKEND_C);
        assertThat(retryEventList.getRetryEvents())
            .hasSize(retryEventListForCBefore.getRetryEvents().size() + 3);

        assertThat(
            retry.getRetryConfig().getExceptionPredicate().test(new IllegalArgumentException()))
            .isTrue();
        assertThat(retry.getRetryConfig().getExceptionPredicate().test(new IgnoredException()))
            .isFalse();

        assertThat(retryAspect.getOrder()).isEqualTo(399);
    }

    private RetryEventsEndpointResponse retryEventListBody(String url) {
        return restTemplate.getForEntity(url, RetryEventsEndpointResponse.class).getBody();
    }

}
