/*
 * Copyright 2019 lespinsideg, Mahmoud Romeh
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
package io.github.resilience4j.bulkhead;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.github.resilience4j.bulkhead.autoconfigure.BulkheadProperties;
import io.github.resilience4j.bulkhead.autoconfigure.ThreadPoolBulkheadProperties;
import io.github.resilience4j.bulkhead.configure.BulkheadAspect;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.common.bulkhead.monitoring.endpoint.BulkheadEndpointResponse;
import io.github.resilience4j.common.bulkhead.monitoring.endpoint.BulkheadEventDTO;
import io.github.resilience4j.common.bulkhead.monitoring.endpoint.BulkheadEventsEndpointResponse;
import io.github.resilience4j.service.test.DummyFeignClient;
import io.github.resilience4j.service.test.TestApplication;
import io.github.resilience4j.service.test.bulkhead.BulkheadDummyService;
import io.github.resilience4j.service.test.bulkhead.BulkheadReactiveDummyService;
import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.Ordered;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.CompletableFuture.runAsync;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = TestApplication.class)
public class BulkheadAutoConfigurationTest {

    @Autowired
    private BulkheadRegistry bulkheadRegistry;

    @Autowired
    private ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry;

    @Autowired
    private BulkheadProperties bulkheadProperties;

    @Autowired
    private ThreadPoolBulkheadProperties threadPoolBulkheadProperties;

    @Autowired
    private BulkheadAspect bulkheadAspect;

    @Autowired
    private BulkheadDummyService dummyService;

    @Autowired
    private BulkheadReactiveDummyService reactiveDummyService;


    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DummyFeignClient dummyFeignClient;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8090);


    /**
     * This test verifies that the combination of @FeignClient and @Bulkhead annotation works as same as @Bulkhead alone works with any normal service class
     */
    @Test
    public void testFeignClient() throws InterruptedException {
        BulkheadEventsEndpointResponse eventsBefore = getBulkheadEvents("/actuator/bulkheadevents").getBody();
        int expectedConcurrentCalls = 3;
        int expectedRejectedCalls = 2;
        int responseDelay = 1000;
        WireMock.stubFor(WireMock
                .get(WireMock.urlEqualTo("/sample/"))
                .willReturn(WireMock.aResponse().withStatus(200).withBody("This is successful call")
                        .withFixedDelay(responseDelay))
        );

        ExecutorService es = Executors.newFixedThreadPool(5);
        List<CompletableFuture<Void>> futures = new ArrayList<>(5);
        for (int i = 0; i < 5; i++) {
            futures.add(runAsync(this::callService, es));
        }

        Thread.sleep(responseDelay + 100);
        int actualSuccessfulCalls =
                (int) futures.stream().filter(f -> f.isDone() && !f.isCompletedExceptionally()).count();
        int actualRejectedCalls = 0;


        List<BulkheadEventDTO> bulkheadEvents = getBulkheadEvents("/actuator/bulkheadevents").getBody().getBulkheadEvents();
        bulkheadEvents = bulkheadEvents.subList(eventsBefore.getBulkheadEvents().size(), bulkheadEvents.size());
        for (BulkheadEventDTO eventDTO : bulkheadEvents) {
            if (eventDTO.getType().equals(BulkheadEvent.Type.CALL_REJECTED)) {
                actualRejectedCalls++;
            }
        }
        Bulkhead bulkhead = bulkheadRegistry.bulkhead("dummyFeignClient");

        assertThat(bulkhead).isNotNull();
        assertThat(bulkhead.getMetrics().getMaxAllowedConcurrentCalls()).isEqualTo(3);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(3);
        assertThat(actualSuccessfulCalls).isEqualTo(expectedConcurrentCalls);
        assertThat(actualRejectedCalls).isEqualTo(expectedRejectedCalls);
    }

    private void callService() {
        dummyFeignClient.doSomething(StringUtils.EMPTY);
    }

    /**
     * The test verifies that a Bulkhead instance is created and configured properly when the BulkheadDummyService is invoked and
     * that the Bulkhead records permitted and rejected calls.
     */
    @Test
    public void testBulkheadAutoConfigurationThreadPool() {
        ExecutorService es = Executors.newFixedThreadPool(5);

        assertThat(threadPoolBulkheadRegistry).isNotNull();
        assertThat(threadPoolBulkheadProperties).isNotNull();

        ThreadPoolBulkhead bulkhead = threadPoolBulkheadRegistry.bulkhead(BulkheadDummyService.BACKEND_C);
        assertThat(bulkhead).isNotNull();

        for (int i = 0; i < 4; i++) {
            es.submit(dummyService::doSomethingAsync);
        }

        await()
                .atMost(1, TimeUnit.SECONDS)
                .until(() -> bulkhead.getMetrics().getRemainingQueueCapacity() == 0);


        await()
                .atMost(1, TimeUnit.SECONDS)
                .until(() -> bulkhead.getMetrics().getQueueCapacity() == 1);
        // Test Actuator endpoints

        ResponseEntity<BulkheadEndpointResponse> bulkheadList = restTemplate.getForEntity("/actuator/bulkheads", BulkheadEndpointResponse.class);
        assertThat(bulkheadList.getBody().getBulkheads()).hasSize(5).containsExactly("backendA", "backendB", "backendB", "backendC", "dummyFeignClient");

        for (int i = 0; i < 5; i++) {
            es.submit(dummyService::doSomethingAsync);
        }

        ResponseEntity<BulkheadEventsEndpointResponse> bulkheadEventList = getBulkheadEvents("/actuator/bulkheadevents/backendC");
        List<BulkheadEventDTO> bulkheadEventsByBackend = bulkheadEventList.getBody().getBulkheadEvents();

        assertThat(bulkheadEventsByBackend.get(bulkheadEventsByBackend.size() - 1).getType()).isEqualTo(BulkheadEvent.Type.CALL_REJECTED);
        assertThat(bulkheadEventsByBackend).filteredOn(it -> it.getType() == BulkheadEvent.Type.CALL_REJECTED)
                .isNotEmpty();
        assertThat(bulkheadEventsByBackend.stream().filter(it -> it.getType() == BulkheadEvent.Type.CALL_PERMITTED).count() == 2);
        assertThat(bulkheadEventsByBackend.stream().filter(it -> it.getType() == BulkheadEvent.Type.CALL_FINISHED).count() == 1);

        assertThat(bulkheadAspect.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);

        es.shutdown();
    }


    /**
     * The test verifies that a Bulkhead instance is created and configured properly when the BulkheadDummyService is invoked and
     * that the Bulkhead records permitted and rejected calls.
     */
    @Test
    public void testBulkheadAutoConfiguration() {
        ExecutorService es = Executors.newFixedThreadPool(5);

        assertThat(bulkheadRegistry).isNotNull();
        assertThat(bulkheadProperties).isNotNull();

        Bulkhead bulkhead = bulkheadRegistry.bulkhead(BulkheadDummyService.BACKEND);
        assertThat(bulkhead).isNotNull();

        for (int i = 0; i < 4; i++) {
            es.submit(dummyService::doSomething);
        }

        await()
                .atMost(1, TimeUnit.SECONDS)
                .until(() -> bulkhead.getMetrics().getAvailableConcurrentCalls() == 0);

        assertThat(bulkhead.getBulkheadConfig().getMaxWaitDuration().toMillis()).isEqualTo(0);
        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(1);

        await()
                .atMost(1, TimeUnit.SECONDS)
                .until(() -> bulkhead.getMetrics().getAvailableConcurrentCalls() == 1);
        // Test Actuator endpoints

        ResponseEntity<BulkheadEndpointResponse> bulkheadList = restTemplate.getForEntity("/actuator/bulkheads", BulkheadEndpointResponse.class);
        assertThat(bulkheadList.getBody().getBulkheads()).hasSize(5).containsExactly("backendA", "backendB", "backendB", "backendC", "dummyFeignClient");

        for (int i = 0; i < 5; i++) {
            es.submit(dummyService::doSomething);
        }

        await()
                .atMost(1, TimeUnit.SECONDS)
                .until(() -> bulkhead.getMetrics().getAvailableConcurrentCalls() == 1);

        ResponseEntity<BulkheadEventsEndpointResponse> bulkheadEventList = getBulkheadEvents("/actuator/bulkheadevents");
        List<BulkheadEventDTO> bulkheadEvents = bulkheadEventList.getBody().getBulkheadEvents();

        assertThat(bulkheadEvents).isNotEmpty();
        assertThat(bulkheadEvents.get(bulkheadEvents.size() - 1).getType()).isEqualTo(BulkheadEvent.Type.CALL_FINISHED);
        assertThat(bulkheadEvents.get(bulkheadEvents.size() - 2).getType()).isEqualTo(BulkheadEvent.Type.CALL_REJECTED);

        bulkheadEventList = getBulkheadEvents("/actuator/bulkheadevents/backendA");
        List<BulkheadEventDTO> bulkheadEventsByBackend = bulkheadEventList.getBody().getBulkheadEvents();

        assertThat(bulkheadEventsByBackend.get(bulkheadEventsByBackend.size() - 1).getType()).isEqualTo(BulkheadEvent.Type.CALL_FINISHED);
        assertThat(bulkheadEventsByBackend).filteredOn(it -> it.getType() == BulkheadEvent.Type.CALL_REJECTED)
                .isNotEmpty();

        assertThat(bulkheadAspect.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);

        es.shutdown();
    }

    /**
     * The test verifies that a Bulkhead instance is created and configured properly when the BulkheadReactiveDummyService is invoked and
     * that the Bulkhead records permitted and rejected calls.
     */
    @Test
    public void testBulkheadAutoConfigurationRxJava2() {
        ExecutorService es = Executors.newFixedThreadPool(5);
        assertThat(bulkheadRegistry).isNotNull();
        assertThat(bulkheadProperties).isNotNull();

        Bulkhead bulkhead = bulkheadRegistry.bulkhead(BulkheadReactiveDummyService.BACKEND);
        assertThat(bulkhead).isNotNull();

        for (int i = 0; i < 5; i++) {
            es.submit(new Thread(() -> reactiveDummyService.doSomethingFlowable()
                    .subscribe(String::toUpperCase, throwable -> System.out.println("Bulkhead Exception received: " + throwable.getMessage()))));
        }
        await()
                .atMost(1200, TimeUnit.MILLISECONDS)
                .until(() -> bulkhead.getMetrics().getAvailableConcurrentCalls() == 0);

        await()
                .atMost(1000, TimeUnit.MILLISECONDS)
                .until(() -> bulkhead.getMetrics().getAvailableConcurrentCalls() == 2);

        for (int i = 0; i < 5; i++) {
            es.submit(new Thread(() -> reactiveDummyService.doSomethingFlowable()
                    .subscribe(String::toUpperCase, throwable -> System.out.println("Bulkhead Exception received: " + throwable.getMessage()))));
        }

        await()
                .atMost(1000, TimeUnit.MILLISECONDS)
                .until(() -> bulkhead.getMetrics().getAvailableConcurrentCalls() == 2);

        assertThat(bulkhead.getBulkheadConfig().getMaxWaitDuration().toMillis()).isEqualTo(10);
        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(2);
        commonAssertions();

        es.shutdown();
    }


    /**
     * The test verifies that a Bulkhead instance is created and configured properly when the BulkheadReactiveDummyService is invoked and
     * that the Bulkhead records permitted and rejected calls.
     */
    @Test
    public void testBulkheadAutoConfigurationReactor() {
        ExecutorService es = Executors.newFixedThreadPool(5);
        assertThat(bulkheadRegistry).isNotNull();
        assertThat(bulkheadProperties).isNotNull();

        Bulkhead bulkhead = bulkheadRegistry.bulkhead(BulkheadReactiveDummyService.BACKEND);
        assertThat(bulkhead).isNotNull();

        for (int i = 0; i < 5; i++) {
            es.submit(new Thread(() -> reactiveDummyService.doSomethingFlux()
                    .subscribe(String::toUpperCase, throwable -> System.out.println("Bulkhead Exception received: " + throwable.getMessage()))));
        }
        await()
                .atMost(1200, TimeUnit.MILLISECONDS)
                .until(() -> bulkhead.getMetrics().getAvailableConcurrentCalls() == 0);

        await()
                .atMost(1000, TimeUnit.MILLISECONDS)
                .until(() -> bulkhead.getMetrics().getAvailableConcurrentCalls() == 2);

        for (int i = 0; i < 5; i++) {
            es.submit(new Thread(() -> reactiveDummyService.doSomethingFlux()
                    .subscribe(String::toUpperCase, throwable -> System.out.println("Bulkhead Exception received: " + throwable.getMessage()))));
        }

        await()
                .atMost(1000, TimeUnit.MILLISECONDS)
                .until(() -> bulkhead.getMetrics().getAvailableConcurrentCalls() == 2);

        commonAssertions();
        assertThat(bulkhead.getBulkheadConfig().getMaxWaitDuration().toMillis()).isEqualTo(10);
        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(2);

        es.shutdown();
    }

    private void commonAssertions() {
        // Test Actuator endpoints

        ResponseEntity<BulkheadEndpointResponse> bulkheadList = restTemplate.getForEntity("/actuator/bulkheads", BulkheadEndpointResponse.class);
        assertThat(bulkheadList.getBody().getBulkheads()).hasSize(5).containsExactly("backendA", "backendB", "backendB", "backendC", "dummyFeignClient");

        ResponseEntity<BulkheadEventsEndpointResponse> bulkheadEventList = getBulkheadEvents("/actuator/bulkheadevents");
        List<BulkheadEventDTO> bulkheadEvents = bulkheadEventList.getBody().getBulkheadEvents();

        assertThat(bulkheadEvents).isNotEmpty();
        assertThat(bulkheadEvents.get(bulkheadEvents.size() - 1).getType()).isEqualTo(BulkheadEvent.Type.CALL_FINISHED);
        assertThat(bulkheadEvents.get(bulkheadEvents.size() - 2).getType()).isEqualTo(BulkheadEvent.Type.CALL_FINISHED);
        assertThat(bulkheadEvents.get(bulkheadEvents.size() - 3).getType()).isEqualTo(BulkheadEvent.Type.CALL_REJECTED);
        assertThat(bulkheadEvents.get(bulkheadEvents.size() - 4).getType()).isEqualTo(BulkheadEvent.Type.CALL_REJECTED);

        bulkheadEventList = getBulkheadEvents("/actuator/bulkheadevents/backendB");
        List<BulkheadEventDTO> bulkheadEventsByBackend = bulkheadEventList.getBody().getBulkheadEvents();

        assertThat(bulkheadEventsByBackend.get(bulkheadEventsByBackend.size() - 1).getType()).isEqualTo(BulkheadEvent.Type.CALL_FINISHED);
        assertThat(bulkheadEventsByBackend).filteredOn(it -> it.getType() == BulkheadEvent.Type.CALL_REJECTED)
                .isNotEmpty();

        assertThat(bulkheadAspect.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }

    private ResponseEntity<BulkheadEventsEndpointResponse> getBulkheadEvents(String s) {
        return restTemplate.getForEntity(s, BulkheadEventsEndpointResponse.class);
    }
}
