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
import io.github.resilience4j.common.retry.monitoring.endpoint.RetryEndpointResponse;
import io.github.resilience4j.common.retry.monitoring.endpoint.RetryEventsEndpointResponse;
import io.github.resilience4j.retry.autoconfigure.RetryProperties;
import io.github.resilience4j.service.test.RetryDummyService;
import io.github.resilience4j.service.test.TestApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.github.resilience4j.service.test.RetryDummyService.RETRY_BACKEND_A;
import static io.github.resilience4j.service.test.RetryDummyService.RETRY_BACKEND_B;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = TestApplication.class)
public class AsyncRetryAutoConfigurationTest {

    @Autowired
    RetryRegistry retryRegistry;

    @Autowired
    RetryProperties retryProperties;

    @Autowired
    RetryDummyService retryDummyService;

    @Autowired
    private TestRestTemplate restTemplate;


    /**
     * The test verifies that a Async Retry instance is created and configured properly when the
     * RetryDummyService is invoked and that the Async Retry logic is properly handled
     */
    @Test
    public void testRetryAutoConfigurationAsync() throws Throwable {
        assertThat(retryRegistry).isNotNull();

        try {
            final CompletionStage<String> stringCompletionStage = retryDummyService
                .doSomethingAsync(true);
            String result = awaitResult(stringCompletionStage, 5);
            assertThat(result).isNull();

        } catch (IOException ex) {
            // Do nothing. The IOException is recorded by the retry as it is one of failure exceptions
            assertThat(ex.getMessage()).contains("Test Message");
        }
        // The invocation is recorded by the CircuitBreaker as a success.
        String resultSuccess = awaitResult(retryDummyService.doSomethingAsync(false), 5);
        assertThat(resultSuccess).isNotEmpty();
        Retry retry = retryRegistry.retry(RETRY_BACKEND_B);
        assertThat(retry).isNotNull();

        // expect retry is configured as defined in application.yml
        assertThat(retry.getRetryConfig().getMaxAttempts()).isEqualTo(3);
        assertThat(retry.getName()).isEqualTo(RETRY_BACKEND_B);
        assertThat(retry.getRetryConfig().getExceptionPredicate().test(new IOException())).isTrue();

        assertThat(retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
        assertThat(retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(1);
        // it is failing only on travis build ?! need more investigation as locally from terminal and IDEA it is not failing
        //assertThat(retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt()).isEqualTo(1);
        assertThat(retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(0);

        // expect retry actuator endpoint contains both retries
        ResponseEntity<RetryEndpointResponse> retriesList = restTemplate
            .getForEntity("/retries", RetryEndpointResponse.class);
        assertThat(retriesList.getBody().getRetries()).hasSize(2)
            .containsOnly(RETRY_BACKEND_A, RETRY_BACKEND_B);

        // expect retry-event actuator endpoint recorded both events
        ResponseEntity<RetryEventsEndpointResponse> retryBackendEventList = restTemplate
            .getForEntity("/retries/events/" + RETRY_BACKEND_B, RetryEventsEndpointResponse.class);
        assertThat(retryBackendEventList.getBody().getRetryEvents()).hasSize(3);

        assertThat(retry.getRetryConfig().getExceptionPredicate().test(new IOException())).isTrue();
        assertThat(retry.getRetryConfig().getExceptionPredicate().test(new IgnoredException()))
            .isFalse();
    }

    private <T> T awaitResult(CompletionStage<T> completionStage, long timeoutSeconds)
        throws Throwable {
        try {
            return completionStage.toCompletableFuture().get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException e) {
            throw new AssertionError(e);
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }
}
