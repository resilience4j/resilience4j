/*
 * Copyright 2026 Bobae Kim
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
package io.github.resilience4j.spring6.httpservice;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.spring6.httpservice.test.TestHttpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Tests the integration of the {@link Resilience4jHttpService} with {@link Bulkhead}
 */
@WireMockTest
class Resilience4jHttpServiceBulkheadTest {

    private HttpServiceProxyFactory factory;
    private TestHttpService testService;
    private Bulkhead bulkhead;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        RestClient restClient = RestClient.builder()
                .baseUrl(wmRuntimeInfo.getHttpBaseUrl())
                .build();
        factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();

        bulkhead = spy(Bulkhead.of("bulkheadTest", BulkheadConfig.ofDefaults()));
        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withBulkhead(bulkhead)
                .build();
        testService = Resilience4jHttpService.builder(decorators)
                .factory(factory)
                .build(TestHttpService.class);
    }

    @Test
    void testSuccessfulCall() {
        givenResponse(200);

        testService.greeting();

        verify(1, getRequestedFor(urlPathEqualTo("/api/greeting")));
        verify(bulkhead).acquirePermission();
    }

    @Test
    void testSuccessfulCallWithDefaultMethod() {
        givenResponse(200);

        testService.defaultGreeting();

        verify(1, getRequestedFor(urlPathEqualTo("/api/greeting")));
        verify(bulkhead).acquirePermission();
    }

    @Test
    void testBulkheadFull() {
        givenResponse(200);

        when(bulkhead.tryAcquirePermission()).thenReturn(false);

        assertThatThrownBy(() -> testService.greeting())
                .isInstanceOf(BulkheadFullException.class);

        verify(0, getRequestedFor(urlPathEqualTo("/api/greeting")));
    }

    @Test
    void testFailedCall() {
        givenResponse(500);

        assertThatThrownBy(() -> testService.greeting())
                .isInstanceOf(HttpServerErrorException.class);

        verify(bulkhead).acquirePermission();
    }

    @Test
    void testBulkheadReleasesPermissionAfterSuccess() {
        givenResponse(200);

        Bulkhead realBulkhead = Bulkhead.of("test", BulkheadConfig.custom()
                .maxConcurrentCalls(1)
                .build());

        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withBulkhead(realBulkhead)
                .build();
        TestHttpService service = Resilience4jHttpService.builder(decorators)
                .factory(factory)
                .build(TestHttpService.class);

        service.greeting();

        Bulkhead.Metrics metrics = realBulkhead.getMetrics();
        assertThat(metrics.getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    void testBulkheadReleasesPermissionAfterFailure() {
        givenResponse(500);

        Bulkhead realBulkhead = Bulkhead.of("test", BulkheadConfig.custom()
                .maxConcurrentCalls(1)
                .build());

        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withBulkhead(realBulkhead)
                .build();
        TestHttpService service = Resilience4jHttpService.builder(decorators)
                .factory(factory)
                .build(TestHttpService.class);

        try {
            service.greeting();
        } catch (Exception ignored) {
            // ignore
        }

        Bulkhead.Metrics metrics = realBulkhead.getMetrics();
        assertThat(metrics.getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    void testBulkheadConcurrentCalls() throws InterruptedException {
        Bulkhead realBulkhead = Bulkhead.of("test", BulkheadConfig.custom()
                .maxConcurrentCalls(2)
                .maxWaitDuration(Duration.ofMillis(100))
                .build());

        HttpServiceDecorators decorators = HttpServiceDecorators.builder()
                .withBulkhead(realBulkhead)
                .build();
        TestHttpService service = Resilience4jHttpService.builder(decorators)
                .factory(factory)
                .build(TestHttpService.class);

        // Stub slow response
        stubFor(get(urlPathEqualTo("/api/greeting"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("Response")
                        .withFixedDelay(500)));

        try (ExecutorService executor = Executors.newFixedThreadPool(3)) {
            CountDownLatch latch = new CountDownLatch(3);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger rejectedCount = new AtomicInteger(0);

            for (int i = 0; i < 3; i++) {
                executor.submit(() -> {
                    try {
                        service.greeting();
                        successCount.incrementAndGet();
                    } catch (BulkheadFullException e) {
                        rejectedCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean completed = latch.await(5, TimeUnit.SECONDS);
            assertThat(completed).isTrue();

            // At least one call should be rejected due to bulkhead limit
            assertThat(rejectedCount.get()).isGreaterThanOrEqualTo(1);
        }
    }

    private void givenResponse(int responseCode) {
        stubFor(get(urlPathEqualTo("/api/greeting"))
                .willReturn(aResponse()
                        .withStatus(responseCode)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("hello world")));
    }
}
