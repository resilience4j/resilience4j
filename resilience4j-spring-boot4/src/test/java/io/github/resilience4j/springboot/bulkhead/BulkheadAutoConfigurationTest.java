/*
 * Copyright 2025 lespinsideg, Mahmoud Romeh, Artur Havliukovskyi
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
package io.github.resilience4j.springboot.bulkhead;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.ReflectionTestUtils.getField;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.github.resilience4j.springboot.TestThreadLocalContextPropagator;
import io.github.resilience4j.springboot.TestThreadLocalContextPropagator.TestThreadLocalContextHolder;
import io.github.resilience4j.springboot.bulkhead.autoconfigure.BulkheadProperties;
import io.github.resilience4j.springboot.bulkhead.autoconfigure.ThreadPoolBulkheadProperties;
import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.bulkhead.configuration.BulkheadConfigCustomizer;
import io.github.resilience4j.common.bulkhead.configuration.ThreadPoolBulkheadConfigCustomizer;
import io.github.resilience4j.common.bulkhead.monitoring.endpoint.BulkheadEndpointResponse;
import io.github.resilience4j.springboot.service.test.BeanContextPropagator;
import io.github.resilience4j.springboot.service.test.DummyFeignClient;
import io.github.resilience4j.springboot.service.test.TestApplication;
import io.github.resilience4j.springboot.service.test.bulkhead.BulkheadDummyService;
import io.github.resilience4j.springboot.service.test.bulkhead.BulkheadReactiveDummyService;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = TestApplication.class)
@AutoConfigureTestRestTemplate
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
        WireMock.stubFor(WireMock
            .get(WireMock.urlEqualTo("/sample/"))
            .willReturn(WireMock.aResponse().withStatus(200).withBody("This is successful call"))
        );

        Bulkhead bulkhead = bulkheadRegistry.bulkhead("dummyFeignClient");
        AtomicInteger finished = new AtomicInteger(0);
        AtomicInteger permitted = new AtomicInteger(0);
        AtomicInteger rejected = new AtomicInteger(0);

        bulkhead.getEventPublisher().onCallFinished(event -> finished.incrementAndGet());
        bulkhead.getEventPublisher().onCallPermitted(event -> permitted.incrementAndGet());
        bulkhead.getEventPublisher().onCallRejected(event -> rejected.incrementAndGet());

        dummyFeignClient.doSomething(StringUtils.EMPTY);

        ResponseEntity<BulkheadEndpointResponse> bulkheadList = restTemplate
            .getForEntity("/actuator/bulkheads", BulkheadEndpointResponse.class);
        assertThat(bulkheadList.getBody().getBulkheads()).contains("dummyFeignClient");

        assertThat(finished).hasValue(1);
        assertThat(permitted).hasValue(1);
        assertThat(rejected).hasValue(0);
    }
    /**
     * The test verifies that a Bulkhead instance is created and configured properly when the
     * BulkheadDummyService is invoked and that the Bulkhead records permitted and rejected calls.
     */
    @Test
    public void testBulkheadAutoConfigurationThreadPool() throws InterruptedException, ExecutionException {

        assertThat(threadPoolBulkheadRegistry).isNotNull();
        assertThat(threadPoolBulkheadProperties).isNotNull();

        ThreadPoolBulkhead bulkhead = threadPoolBulkheadRegistry
            .bulkhead(BulkheadDummyService.BACKEND_C);
        assertThat(bulkhead).isNotNull();

        AtomicInteger finished = new AtomicInteger(0);
        AtomicInteger permitted = new AtomicInteger(0);
        AtomicInteger rejected = new AtomicInteger(0);

        bulkhead.getEventPublisher().onCallFinished(event -> finished.incrementAndGet());
        bulkhead.getEventPublisher().onCallPermitted(event -> permitted.incrementAndGet());
        bulkhead.getEventPublisher().onCallRejected(event -> rejected.incrementAndGet());

        assertThat(dummyService.doSomethingAsync().get()).isEqualTo("test");

        ResponseEntity<BulkheadEndpointResponse> bulkheadList = restTemplate
            .getForEntity("/actuator/bulkheads", BulkheadEndpointResponse.class);
        assertThat(bulkheadList.getBody().getBulkheads()).contains(BulkheadDummyService.BACKEND_C);

        assertThat(finished).hasValue(1);
        assertThat(permitted).hasValue(1);
        assertThat(rejected).hasValue(0);
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
            .bulkhead(BulkheadDummyService.BACKEND_E);

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
    }


    /**
     * The test verifies that a Bulkhead instance is created and configured properly when the
     * BulkheadDummyService is invoked and that the Bulkhead records permitted and rejected calls.
     */
    @Test
    public void testBulkheadAutoConfiguration() {

        assertThat(bulkheadRegistry).isNotNull();
        assertThat(bulkheadProperties).isNotNull();

        Bulkhead bulkhead = bulkheadRegistry.bulkhead(BulkheadDummyService.BACKEND);
        assertThat(bulkhead).isNotNull();

        AtomicInteger finished = new AtomicInteger(0);
        AtomicInteger permitted = new AtomicInteger(0);
        AtomicInteger rejected = new AtomicInteger(0);

        bulkhead.getEventPublisher().onCallFinished(event -> finished.incrementAndGet());
        bulkhead.getEventPublisher().onCallPermitted(event -> permitted.incrementAndGet());
        bulkhead.getEventPublisher().onCallRejected(event -> rejected.incrementAndGet());

        dummyService.doSomething();

        ResponseEntity<BulkheadEndpointResponse> bulkheadList = restTemplate
            .getForEntity("/actuator/bulkheads", BulkheadEndpointResponse.class);
        assertThat(bulkheadList.getBody().getBulkheads()).contains(BulkheadDummyService.BACKEND);

        assertThat(finished).hasValue(1);
        assertThat(permitted).hasValue(1);
        assertThat(rejected).hasValue(0);
    }

    /**
     * The test verifies that a Bulkhead instance is created and configured properly when the
     * BulkheadReactiveDummyService is invoked and that the Bulkhead records permitted and rejected
     * calls.
     */
    @Test
    public void testBulkheadAutoConfigurationRxJava2() throws InterruptedException {
        assertThat(bulkheadRegistry).isNotNull();
        assertThat(bulkheadProperties).isNotNull();

        Bulkhead bulkhead = bulkheadRegistry.bulkhead(BulkheadReactiveDummyService.BACKEND);
        assertThat(bulkhead).isNotNull();

        AtomicInteger finished = new AtomicInteger(0);
        AtomicInteger permitted = new AtomicInteger(0);
        AtomicInteger rejected = new AtomicInteger(0);

        bulkhead.getEventPublisher().onCallFinished(event -> finished.incrementAndGet());
        bulkhead.getEventPublisher().onCallPermitted(event -> permitted.incrementAndGet());
        bulkhead.getEventPublisher().onCallRejected(event -> rejected.incrementAndGet());

        assertThat(reactiveDummyService.doSomethingFlowable().blockingSingle()).isEqualTo("test");

        ResponseEntity<BulkheadEndpointResponse> bulkheadList = restTemplate
            .getForEntity("/actuator/bulkheads", BulkheadEndpointResponse.class);
        assertThat(bulkheadList.getBody().getBulkheads()).contains(BulkheadReactiveDummyService.BACKEND);

        assertThat(finished).hasValue(1);
        assertThat(permitted).hasValue(1);
        assertThat(rejected).hasValue(0);
    }


    /**
     * The test verifies that a Bulkhead instance is created and configured properly when the
     * BulkheadReactiveDummyService is invoked and that the Bulkhead records permitted and rejected
     * calls.
     */
    @Test
    public void testBulkheadAutoConfigurationReactor() {
        assertThat(bulkheadRegistry).isNotNull();
        assertThat(bulkheadProperties).isNotNull();

        Bulkhead bulkhead = bulkheadRegistry.bulkhead(BulkheadReactiveDummyService.BACKEND);
        assertThat(bulkhead).isNotNull();

        AtomicInteger finished = new AtomicInteger(0);
        AtomicInteger permitted = new AtomicInteger(0);
        AtomicInteger rejected = new AtomicInteger(0);

        bulkhead.getEventPublisher().onCallFinished(event -> finished.incrementAndGet());
        bulkhead.getEventPublisher().onCallPermitted(event -> permitted.incrementAndGet());
        bulkhead.getEventPublisher().onCallRejected(event -> rejected.incrementAndGet());

        assertThat(reactiveDummyService.doSomethingFlux().blockFirst()).isEqualTo("test");

        ResponseEntity<BulkheadEndpointResponse> bulkheadList = restTemplate
            .getForEntity("/actuator/bulkheads", BulkheadEndpointResponse.class);
        assertThat(bulkheadList.getBody().getBulkheads()).contains(BulkheadReactiveDummyService.BACKEND);

        assertThat(finished).hasValue(1);
        assertThat(permitted).hasValue(1);
        assertThat(rejected).hasValue(0);
    }
}
