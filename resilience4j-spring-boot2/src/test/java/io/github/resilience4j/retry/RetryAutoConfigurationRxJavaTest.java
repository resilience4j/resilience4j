/*
 * Copyright 2019 Mahmoud Romeh
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
package io.github.resilience4j.retry;

import io.github.resilience4j.circuitbreaker.IgnoredException;
import io.github.resilience4j.common.retry.monitoring.endpoint.RetryEventsEndpointResponse;
import io.github.resilience4j.core.IntervalBiFunction;
import io.github.resilience4j.retry.autoconfigure.RetryProperties;
import io.github.resilience4j.retry.configure.RetryAspect;
import io.github.resilience4j.service.test.TestApplication;
import io.github.resilience4j.service.test.retry.ReactiveRetryDummyService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.function.Function;

import static io.github.resilience4j.service.test.retry.ReactiveRetryDummyService.BACKEND_C;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = TestApplication.class)
public class RetryAutoConfigurationRxJavaTest {

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
    public void testRetryAutoConfigurationRxJava2() throws IOException {
        assertThat(retryRegistry).isNotNull();
        assertThat(retryProperties).isNotNull();
        RetryEventsEndpointResponse retryEventListBefore = getRetryEventsBody(
            "/actuator/retryevents");
        RetryEventsEndpointResponse retryEventsListOfCBefore = getRetryEventsBody(
            "/actuator/retryevents/" + BACKEND_C);

        Retry retry = retryRegistry.retry(BACKEND_C);
        assertThat(retry).isNotNull();
        Retry.Metrics metricsBefore = retry.getMetrics();

        retryDummyService.doSomethingFlowable(true).blockingSubscribe(String::toUpperCase,
            throwable -> System.out.println("Exception received:" + throwable.getMessage()));

        // The invocation is recorded by the CircuitBreaker as a success.
        retryDummyService.doSomethingFlowable(false).blockingSubscribe(String::toUpperCase,
            throwable -> System.out.println("Exception received:" + throwable.getMessage()));

        // expect retry is configured as defined in application.yml
        assertThat(retry.getRetryConfig().getMaxAttempts()).isEqualTo(3);
        assertThat(retry.getName()).isEqualTo(BACKEND_C);
        assertThat(retry.getRetryConfig().getExceptionPredicate().test(new IOException())).isTrue();

        // expect retry-event actuator endpoint recorded both events
        RetryEventsEndpointResponse retryEventList = getRetryEventsBody("/actuator/retryevents");
        assertThat(retryEventList.getRetryEvents())
            .hasSize(retryEventListBefore.getRetryEvents().size() + 3);

        retryEventList = getRetryEventsBody("/actuator/retryevents/" + BACKEND_C);
        assertThat(retryEventList.getRetryEvents())
            .hasSize(retryEventsListOfCBefore.getRetryEvents().size() + 3);

        assertThat(
            retry.getRetryConfig().getExceptionPredicate().test(new IllegalArgumentException()))
            .isTrue();
        assertThat(retry.getRetryConfig().getExceptionPredicate().test(new IgnoredException()))
            .isFalse();

        IntervalBiFunction<?> exponentialBackoff = retry.getRetryConfig().getIntervalBiFunction();
        assertThat(exponentialBackoff.apply(1,null)).isEqualTo(100);
        assertThat(exponentialBackoff.apply(2,null)).isEqualTo(200);
        assertThat(exponentialBackoff.apply(3,null)).isEqualTo(222);

        assertThat(retryAspect.getOrder()).isEqualTo(399);

        assertThat(retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt())
            .isEqualTo(metricsBefore.getNumberOfFailedCallsWithoutRetryAttempt());
        assertThat(retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt())
            .isGreaterThanOrEqualTo(metricsBefore.getNumberOfFailedCallsWithRetryAttempt());
        assertThat(retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt())
            .isGreaterThanOrEqualTo(metricsBefore.getNumberOfSuccessfulCallsWithoutRetryAttempt());
        assertThat(retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt())
            .isEqualTo(metricsBefore.getNumberOfSuccessfulCallsWithRetryAttempt());
    }

    private RetryEventsEndpointResponse getRetryEventsBody(String s) {
        return restTemplate.getForEntity(s, RetryEventsEndpointResponse.class).getBody();
    }

}
