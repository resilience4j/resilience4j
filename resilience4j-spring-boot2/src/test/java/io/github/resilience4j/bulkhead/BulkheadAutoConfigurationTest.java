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
import io.github.resilience4j.TestThreadLocalContextPropagator;
import io.github.resilience4j.TestThreadLocalContextPropagator.TestThreadLocalContextHolder;
import io.github.resilience4j.bulkhead.autoconfigure.BulkheadProperties;
import io.github.resilience4j.bulkhead.autoconfigure.ThreadPoolBulkheadProperties;
import io.github.resilience4j.bulkhead.configure.BulkheadAspect;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.bulkhead.configuration.BulkheadConfigCustomizer;
import io.github.resilience4j.common.bulkhead.configuration.ThreadPoolBulkheadConfigCustomizer;
import io.github.resilience4j.common.bulkhead.monitoring.endpoint.BulkheadEndpointResponse;
import io.github.resilience4j.common.bulkhead.monitoring.endpoint.BulkheadEventDTO;
import io.github.resilience4j.common.bulkhead.monitoring.endpoint.BulkheadEventsEndpointResponse;
import io.github.resilience4j.service.test.BeanContextPropagator;
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.CompletableFuture.runAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.ReflectionTestUtils.getField;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = TestApplication.class)
public class BulkheadAutoConfigurationTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8090);
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
    @Autowired
    private CompositeCustomizer<ThreadPoolBulkheadConfigCustomizer> compositeThreadPoolBulkheadCustomizer;
    @Autowired
    private CompositeCustomizer<BulkheadConfigCustomizer> compositeBulkheadCustomizer;

    @Test
    public void testThreadPoolBulkheadCustomizer() {
        Map<String, ThreadPoolBulkheadConfigCustomizer> customizerMap = (Map<String, ThreadPoolBulkheadConfigCustomizer>) getField(
            compositeThreadPoolBulkheadCustomizer, "customizerMap");
        assertThat(customizerMap).isNotNull().hasSize(1).containsKeys("backendC");

        //ContextPropagator set by properties
        ThreadPoolBulkhead bulkheadD = threadPoolBulkheadRegistry
            .bulkhead(BulkheadDummyService.BACKEND_D);

        assertThat(bulkheadD).isNotNull();
        assertThat(bulkheadD.getBulkheadConfig()).isNotNull();
        assertThat(bulkheadD.getBulkheadConfig().getContextPropagator()).isNotNull();
        assertThat(bulkheadD.getBulkheadConfig().getContextPropagator().size()).isEqualTo(1);
        assertThat(bulkheadD.getBulkheadConfig().getContextPropagator().get(0).getClass())
            .isEqualTo(TestThreadLocalContextPropagator.class);

        //ContextPropagator set by bean using Registry Customizer
        ThreadPoolBulkhead bulkheadC = threadPoolBulkheadRegistry
            .bulkhead(BulkheadDummyService.BACKEND_C);

        assertThat(bulkheadC).isNotNull();
        assertThat(bulkheadC.getBulkheadConfig()).isNotNull();
        assertThat(bulkheadC.getBulkheadConfig().getContextPropagator()).isNotNull();
        assertThat(bulkheadC.getBulkheadConfig().getContextPropagator().size()).isEqualTo(1);
        assertThat(bulkheadC.getBulkheadConfig().getContextPropagator().get(0).getClass())
            .isEqualTo(BeanContextPropagator.class);
    }

    @Test
    public void testBulkheadCustomizer() {
        Map<String, BulkheadConfigCustomizer> customizerMap = (Map<String, BulkheadConfigCustomizer>) getField(
            compositeBulkheadCustomizer, "customizerMap");
        assertThat(customizerMap).isNotNull().hasSize(2).containsKeys("backendCustomizer", "backendD");

        Bulkhead backendCustomizer = bulkheadRegistry.bulkhead("backendCustomizer");

        assertThat(backendCustomizer).isNotNull();
        assertThat(backendCustomizer.getBulkheadConfig()).isNotNull();
        assertThat(backendCustomizer.getBulkheadConfig().getMaxWaitDuration()).isEqualTo(Duration.ofMillis(100));
        assertThat(backendCustomizer.getBulkheadConfig().isWritableStackTraceEnabled()).isTrue();

        //updated by Customizer
        assertThat(backendCustomizer.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(20);
    }

    /**
     * This test verifies that the combination of @FeignClient and @Bulkhead annotation works as
     * same as @Bulkhead alone works with any normal service class
     */
    @Test
    public void testFeignClient() throws InterruptedException {
        BulkheadEventsEndpointResponse eventsBefore = getBulkheadEvents("/actuator/bulkheadevents")
            .getBody();
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

        List<BulkheadEventDTO> bulkheadEvents = getBulkheadEvents("/actuator/bulkheadevents")
            .getBody().getBulkheadEvents();
        bulkheadEvents = bulkheadEvents
            .subList(eventsBefore.getBulkheadEvents().size(), bulkheadEvents.size());
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
     * The test verifies that a Bulkhead instance is created and configured properly when the
     * BulkheadDummyService is invoked and that the Bulkhead records permitted and rejected calls.
     */
    @Test
    public void testBulkheadAutoConfigurationThreadPool() {
        ExecutorService es = Executors.newFixedThreadPool(5);

        assertThat(threadPoolBulkheadRegistry).isNotNull();
        assertThat(threadPoolBulkheadProperties).isNotNull();

        ThreadPoolBulkhead bulkhead = threadPoolBulkheadRegistry
            .bulkhead(BulkheadDummyService.BACKEND_C);
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

        ResponseEntity<BulkheadEndpointResponse> bulkheadList = restTemplate
            .getForEntity("/actuator/bulkheads", BulkheadEndpointResponse.class);
        assertThat(bulkheadList.getBody().getBulkheads()).hasSize(8)
            .containsExactlyInAnyOrder("backendA", "backendB", "backendB", "backendC", "backendCustomizer", "backendD", "backendD",
                "dummyFeignClient");

        for (int i = 0; i < 5; i++) {
            es.submit(dummyService::doSomethingAsync);
        }

        ResponseEntity<BulkheadEventsEndpointResponse> bulkheadEventList = getBulkheadEvents(
            "/actuator/bulkheadevents/backendC");
        List<BulkheadEventDTO> bulkheadEventsByBackend = bulkheadEventList.getBody()
            .getBulkheadEvents();

        assertThat(bulkheadEventsByBackend.get(bulkheadEventsByBackend.size() - 1).getType())
            .isEqualTo(BulkheadEvent.Type.CALL_REJECTED);
        assertThat(bulkheadEventsByBackend)
            .filteredOn(it -> it.getType() == BulkheadEvent.Type.CALL_REJECTED)
            .isNotEmpty();
        assertThat(bulkheadEventsByBackend.stream()
            .filter(it -> it.getType() == BulkheadEvent.Type.CALL_PERMITTED).count() == 2);
        assertThat(bulkheadEventsByBackend.stream()
            .filter(it -> it.getType() == BulkheadEvent.Type.CALL_FINISHED).count() == 1);

        assertThat(bulkheadAspect.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);

        es.shutdown();
        // test thread pool customizer
        final ThreadPoolBulkhead backendD = threadPoolBulkheadRegistry.bulkhead("backendD");
        assertThat(backendD.getBulkheadConfig().getMaxThreadPoolSize()).isEqualTo(1);
    }

    /**
     * The test verifies that a Bulkhead instance is created and configured properly and is able to
     * transfer context from ThreadLocal
     */
    @Test
    public void testBulkheadAutoConfigurationThreadPoolContextPropagation()
        throws InterruptedException, TimeoutException, ExecutionException {
        assertThat(threadPoolBulkheadRegistry).isNotNull();
        assertThat(threadPoolBulkheadProperties).isNotNull();

        TestThreadLocalContextHolder.put("SurviveThreadBoundary");

        ThreadPoolBulkhead bulkhead = threadPoolBulkheadRegistry
            .bulkhead(BulkheadDummyService.BACKEND_D);

        assertThat(bulkhead).isNotNull();
        assertThat(bulkhead.getBulkheadConfig()).isNotNull();
        assertThat(bulkhead.getBulkheadConfig().getContextPropagator()).isNotNull();
        assertThat(bulkhead.getBulkheadConfig().getContextPropagator().size()).isEqualTo(1);
        assertThat(bulkhead.getBulkheadConfig().getContextPropagator().get(0).getClass())
            .isEqualTo(TestThreadLocalContextPropagator.class);

        CompletableFuture<Object> future = dummyService
            .doSomethingAsyncWithThreadLocal();

        Object value = future.get(5, TimeUnit.SECONDS);

        assertThat(value).isEqualTo("SurviveThreadBoundary");
        // Test Actuator endpoints

        ResponseEntity<BulkheadEventsEndpointResponse> bulkheadEventList = getBulkheadEvents("/actuator/bulkheadevents");
        List<BulkheadEventDTO> bulkheadEventsByBackend = bulkheadEventList.getBody()
            .getBulkheadEvents().stream()
            .filter(b -> "backendD".equals(b.getBulkheadName()))
            .collect(Collectors.toList());

        assertThat(bulkheadEventsByBackend).isNotNull();
        assertThat(bulkheadEventsByBackend.size()).isEqualTo(2);
        assertThat(bulkheadEventsByBackend.stream()
            .filter(it -> it.getType() == BulkheadEvent.Type.CALL_PERMITTED))
            .hasSize(1);
        assertThat(bulkheadEventsByBackend.stream()
            .filter(it -> it.getType() == BulkheadEvent.Type.CALL_FINISHED))
            .hasSize(1);
    }


    /**
     * The test verifies that a Bulkhead instance is created and configured properly when the
     * BulkheadDummyService is invoked and that the Bulkhead records permitted and rejected calls.
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

        ResponseEntity<BulkheadEndpointResponse> bulkheadList = restTemplate
            .getForEntity("/actuator/bulkheads", BulkheadEndpointResponse.class);
        assertThat(bulkheadList.getBody().getBulkheads()).hasSize(8)
            .containsExactlyInAnyOrder("backendA", "backendB", "backendB", "backendC", "backendCustomizer",
                "dummyFeignClient", "backendD", "backendD");

        for (int i = 0; i < 5; i++) {
            es.submit(dummyService::doSomething);
        }

        await()
            .atMost(1, TimeUnit.SECONDS)
            .until(() -> bulkhead.getMetrics().getAvailableConcurrentCalls() == 1);

        ResponseEntity<BulkheadEventsEndpointResponse> bulkheadEventList = getBulkheadEvents(
            "/actuator/bulkheadevents");
        List<BulkheadEventDTO> bulkheadEvents = bulkheadEventList.getBody().getBulkheadEvents();

        assertThat(bulkheadEvents).isNotEmpty();
        assertThat(bulkheadEvents.get(bulkheadEvents.size() - 1).getType())
            .isEqualTo(BulkheadEvent.Type.CALL_FINISHED);
        assertThat(bulkheadEvents.get(bulkheadEvents.size() - 2).getType())
            .isEqualTo(BulkheadEvent.Type.CALL_REJECTED);

        bulkheadEventList = getBulkheadEvents("/actuator/bulkheadevents/backendA");
        List<BulkheadEventDTO> bulkheadEventsByBackend = bulkheadEventList.getBody()
            .getBulkheadEvents();

        assertThat(bulkheadEventsByBackend.get(bulkheadEventsByBackend.size() - 1).getType())
            .isEqualTo(BulkheadEvent.Type.CALL_FINISHED);
        assertThat(bulkheadEventsByBackend)
            .filteredOn(it -> it.getType() == BulkheadEvent.Type.CALL_REJECTED)
            .isNotEmpty();

        assertThat(bulkheadAspect.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);

        es.shutdown();
        // test customizer effect
        final Bulkhead backendD = bulkheadRegistry.bulkhead("backendD");
        assertThat(backendD.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(3);
    }

    /**
     * The test verifies that a Bulkhead instance is created and configured properly when the
     * BulkheadReactiveDummyService is invoked and that the Bulkhead records permitted and rejected
     * calls.
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
                .subscribe(String::toUpperCase, throwable -> System.out
                    .println("Bulkhead Exception received: " + throwable.getMessage()))));
        }
        await()
            .atMost(1200, TimeUnit.MILLISECONDS)
            .until(() -> bulkhead.getMetrics().getAvailableConcurrentCalls() == 0);

        await()
            .atMost(1000, TimeUnit.MILLISECONDS)
            .until(() -> bulkhead.getMetrics().getAvailableConcurrentCalls() == 2);

        for (int i = 0; i < 5; i++) {
            es.submit(new Thread(() -> reactiveDummyService.doSomethingFlowable()
                .subscribe(String::toUpperCase, throwable -> System.out
                    .println("Bulkhead Exception received: " + throwable.getMessage()))));
        }

        await()
            .atMost(1000, TimeUnit.MILLISECONDS)
            .until(() -> bulkhead.getMetrics().getAvailableConcurrentCalls() == 2);

        assertThat(bulkhead.getBulkheadConfig().getMaxWaitDuration().toMillis()).isEqualTo(10);
        assertThat(bulkhead.getBulkheadConfig().isWritableStackTraceEnabled()).isFalse();
        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(2);
        commonAssertions();

        es.shutdown();
    }


    /**
     * The test verifies that a Bulkhead instance is created and configured properly when the
     * BulkheadReactiveDummyService is invoked and that the Bulkhead records permitted and rejected
     * calls.
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
                .subscribe(String::toUpperCase, throwable -> System.out
                    .println("Bulkhead Exception received: " + throwable.getMessage()))));
        }
        await()
            .atMost(1200, TimeUnit.MILLISECONDS)
            .until(() -> bulkhead.getMetrics().getAvailableConcurrentCalls() == 0);

        await()
            .atMost(1000, TimeUnit.MILLISECONDS)
            .until(() -> bulkhead.getMetrics().getAvailableConcurrentCalls() == 2);

        for (int i = 0; i < 5; i++) {
            es.submit(new Thread(() -> reactiveDummyService.doSomethingFlux()
                .subscribe(String::toUpperCase, throwable -> System.out
                    .println("Bulkhead Exception received: " + throwable.getMessage()))));
        }

        await()
            .atMost(1000, TimeUnit.MILLISECONDS)
            .until(() -> bulkhead.getMetrics().getAvailableConcurrentCalls() == 2);

        commonAssertions();
        assertThat(bulkhead.getBulkheadConfig().getMaxWaitDuration().toMillis()).isEqualTo(10);
        assertThat(bulkhead.getBulkheadConfig().isWritableStackTraceEnabled()).isFalse();
        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(2);

        es.shutdown();
    }

    private void commonAssertions() {
        // Test Actuator endpoints

        ResponseEntity<BulkheadEndpointResponse> bulkheadList = restTemplate
            .getForEntity("/actuator/bulkheads", BulkheadEndpointResponse.class);
        assertThat(bulkheadList.getBody().getBulkheads()).hasSize(8)
            .containsExactlyInAnyOrder("backendA", "backendB", "backendB", "backendC", "backendCustomizer", "backendD", "backendD",
                "dummyFeignClient");

        ResponseEntity<BulkheadEventsEndpointResponse> bulkheadEventList = getBulkheadEvents(
            "/actuator/bulkheadevents");
        List<BulkheadEventDTO> bulkheadEvents = bulkheadEventList.getBody().getBulkheadEvents();

        assertThat(bulkheadEvents).isNotEmpty();
        assertThat(bulkheadEvents.get(bulkheadEvents.size() - 1).getType())
            .isEqualTo(BulkheadEvent.Type.CALL_FINISHED);
        assertThat(bulkheadEvents.get(bulkheadEvents.size() - 2).getType())
            .isEqualTo(BulkheadEvent.Type.CALL_FINISHED);
        assertThat(bulkheadEvents.get(bulkheadEvents.size() - 3).getType())
            .isEqualTo(BulkheadEvent.Type.CALL_REJECTED);
        assertThat(bulkheadEvents.get(bulkheadEvents.size() - 4).getType())
            .isEqualTo(BulkheadEvent.Type.CALL_REJECTED);

        bulkheadEventList = getBulkheadEvents("/actuator/bulkheadevents/backendB");
        List<BulkheadEventDTO> bulkheadEventsByBackend = bulkheadEventList.getBody()
            .getBulkheadEvents();

        assertThat(bulkheadEventsByBackend.get(bulkheadEventsByBackend.size() - 1).getType())
            .isEqualTo(BulkheadEvent.Type.CALL_FINISHED);
        assertThat(bulkheadEventsByBackend)
            .filteredOn(it -> it.getType() == BulkheadEvent.Type.CALL_REJECTED)
            .isNotEmpty();

        assertThat(bulkheadAspect.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }

    private ResponseEntity<BulkheadEventsEndpointResponse> getBulkheadEvents(String s) {
        return restTemplate.getForEntity(s, BulkheadEventsEndpointResponse.class);
    }
}
