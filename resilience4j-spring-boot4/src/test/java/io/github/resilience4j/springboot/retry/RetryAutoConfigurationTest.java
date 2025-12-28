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

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.github.resilience4j.springboot.circuitbreaker.IgnoredException;
import io.github.resilience4j.common.retry.monitoring.endpoint.RetryEndpointResponse;
import io.github.resilience4j.common.retry.monitoring.endpoint.RetryEventsEndpointResponse;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.springboot.retry.autoconfigure.RetryProperties;
import io.github.resilience4j.spring6.retry.configure.RetryAspect;
import io.github.resilience4j.springboot.service.test.TestApplication;
import io.github.resilience4j.springboot.service.test.retry.RetryDummyFeignClient;
import io.github.resilience4j.springboot.service.test.retry.RetryDummyService;
import io.github.resilience4j.springboot.service.test.retry.ReactiveRetryDummyService;
import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = TestApplication.class)
@AutoConfigureTestRestTemplate
public class RetryAutoConfigurationTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8090);
    @Autowired
    RetryRegistry retryRegistry;
    @Autowired
    RetryProperties retryProperties;
    @Autowired
    RetryAspect retryAspect;
    @Autowired
    RetryDummyService retryDummyService;
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private RetryDummyFeignClient retryDummyFeignClient;

    /**
     * This test verifies that the combination of @FeignClient and @Retry annotation works as same
     * as @Retry alone works with any normal service class
     */
    @Test
    public void testFeignClient() {

        WireMock.stubFor(WireMock
            .get(WireMock.urlEqualTo("/retry/"))
            .willReturn(WireMock.aResponse().withStatus(200).withBody("This is successful call"))
        );
        WireMock.stubFor(WireMock.get(WireMock.urlMatching("^.*\\/retry\\/error.*$"))
            .willReturn(WireMock.aResponse().withStatus(400).withBody("This is error")));

        assertThat(retryRegistry).isNotNull();
        assertThat(retryProperties).isNotNull();

        RetryEventsEndpointResponse retryEventListBefore = retryEvents("/actuator/retryevents");
        RetryEventsEndpointResponse retryEventsEndpointFeignListBefore = retryEvents(
            "/actuator/retryevents/" + RetryDummyFeignClient.RETRY_DUMMY_FEIGN_CLIENT_NAME);

        try {
            retryDummyFeignClient.doSomething("error");
        } catch (Exception ex) {
            // Do nothing. The IOException is recorded by the retry as it is one of failure exceptions
        }
        // The invocation is recorded by the CircuitBreaker as a success.
        retryDummyFeignClient.doSomething(StringUtils.EMPTY);

        Retry retry = retryRegistry.retry(RetryDummyFeignClient.RETRY_DUMMY_FEIGN_CLIENT_NAME);
        assertThat(retry).isNotNull();

        // expect retry is configured as defined in application.yml
        assertThat(retry.getRetryConfig().getMaxAttempts()).isEqualTo(4);
        assertThat(retry.getName()).isEqualTo(RetryDummyFeignClient.RETRY_DUMMY_FEIGN_CLIENT_NAME);
        assertThat(retry.getRetryConfig().getExceptionPredicate().test(new IOException())).isTrue();

        assertThat(retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt()).isZero();
        assertThat(retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(1);
        assertThat(retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt()).isEqualTo(1);
        assertThat(retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt()).isZero();

        // expect retry actuator endpoint contains both retries
        ResponseEntity<RetryEndpointResponse> retriesList = restTemplate
            .getForEntity("/actuator/retries", RetryEndpointResponse.class);
        assertThat(new HashSet<>(retriesList.getBody().getRetries()))
            .contains(RetryDummyService.RETRY_BACKEND_A, RetryDummyService.RETRY_BACKEND_B,
                ReactiveRetryDummyService.BACKEND_C, RetryDummyFeignClient.RETRY_DUMMY_FEIGN_CLIENT_NAME);

        // expect retry-event actuator endpoint recorded both events
        RetryEventsEndpointResponse retryEventList = retryEvents("/actuator/retryevents");
        assertThat(retryEventList.getRetryEvents())
            .hasSize(retryEventListBefore.getRetryEvents().size() + 4);

        retryEventList = retryEvents("/actuator/retryevents/" + RetryDummyFeignClient.RETRY_DUMMY_FEIGN_CLIENT_NAME);
        assertThat(retryEventList.getRetryEvents())
            .hasSize(retryEventsEndpointFeignListBefore.getRetryEvents().size() + 4);

        assertThat(retry.getRetryConfig().getExceptionPredicate().test(new IOException())).isTrue();
        assertThat(retry.getRetryConfig().getExceptionPredicate().test(new IgnoredException()))
            .isFalse();
        assertThat(retryAspect.getOrder()).isEqualTo(399);
    }

    /**
     * The test verifies that a Retry instance is created and configured properly when the
     * RetryDummyService is invoked and that the Retry logic is properly handled
     */
    @Test
    public void testRetryAutoConfiguration() throws IOException {
        assertThat(retryRegistry).isNotNull();
        assertThat(retryProperties).isNotNull();

        RetryEventsEndpointResponse retryEventListBefore = retryEvents("/actuator/retryevents");
        RetryEventsEndpointResponse retryEventsAListBefore = retryEvents(
            "/actuator/retryevents/" + RetryDummyService.RETRY_BACKEND_A);

        try {
            retryDummyService.doSomething(true);
        } catch (IOException ex) {
            // Do nothing. The IOException is recorded by the retry as it is one of failure exceptions
        }
        // The invocation is recorded by the CircuitBreaker as a success.
        retryDummyService.doSomething(false);

        Retry retry = retryRegistry.retry(RetryDummyService.RETRY_BACKEND_A);
        assertThat(retry).isNotNull();

        // expect retry is configured as defined in application.yml
        assertThat(retry.getRetryConfig().getMaxAttempts()).isEqualTo(4);
        assertThat(retry.getName()).isEqualTo(RetryDummyService.RETRY_BACKEND_A);
        assertThat(retry.getRetryConfig().getExceptionPredicate().test(new IOException())).isTrue();

        assertThat(retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt()).isZero();
        assertThat(retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(1);
        assertThat(retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt()).isEqualTo(1);
        assertThat(retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt()).isZero();

        // expect retry actuator endpoint contains both retries
        ResponseEntity<RetryEndpointResponse> retriesList = restTemplate
            .getForEntity("/actuator/retries", RetryEndpointResponse.class);
        assertThat(new HashSet<>(retriesList.getBody().getRetries()))
            .contains(RetryDummyService.RETRY_BACKEND_A, RetryDummyService.RETRY_BACKEND_B,
                ReactiveRetryDummyService.BACKEND_C, RetryDummyFeignClient.RETRY_DUMMY_FEIGN_CLIENT_NAME);

        // expect retry-event actuator endpoint recorded both events
        RetryEventsEndpointResponse retryEventList = retryEvents("/actuator/retryevents");
        assertThat(retryEventList.getRetryEvents())
            .hasSize(retryEventListBefore.getRetryEvents().size() + 4);

        retryEventList = retryEvents("/actuator/retryevents/" + RetryDummyService.RETRY_BACKEND_A);
        assertThat(retryEventList.getRetryEvents())
            .hasSize(retryEventsAListBefore.getRetryEvents().size() + 4);

        assertThat(retry.getRetryConfig().getExceptionPredicate().test(new IOException())).isTrue();
        assertThat(retry.getRetryConfig().getExceptionPredicate().test(new IgnoredException()))
            .isFalse();
        assertThat(retryAspect.getOrder()).isEqualTo(399);

        // test Customizer effect
        Retry retryCustom = retryRegistry.retry("retryBackendD");
        assertThat(retryCustom.getRetryConfig().getMaxAttempts()).isEqualTo(4);

    }

    private RetryEventsEndpointResponse retryEvents(String s) {
        return restTemplate.getForEntity(s, RetryEventsEndpointResponse.class).getBody();
    }
}
