/*
 * Copyright 2019 lespinsideg
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

import io.github.resilience4j.bulkhead.autoconfigure.BulkheadProperties;
import io.github.resilience4j.bulkhead.configure.BulkheadAspect;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.common.bulkhead.monitoring.endpoint.BulkheadEndpointResponse;
import io.github.resilience4j.common.bulkhead.monitoring.endpoint.BulkheadEventDTO;
import io.github.resilience4j.common.bulkhead.monitoring.endpoint.BulkheadEventsEndpointResponse;
import io.github.resilience4j.service.test.BulkheadDummyService;
import io.github.resilience4j.service.test.TestApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.Ordered;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = TestApplication.class)
public class BulkheadAutoConfigurationTest {

    @Autowired
    private BulkheadRegistry bulkheadRegistry;

    @Autowired
    private BulkheadProperties bulkheadProperties;

    @Autowired
    private BulkheadAspect bulkheadAspect;

    @Autowired
    private BulkheadDummyService dummyService;

    @Autowired
    private TestRestTemplate restTemplate;

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

        es.submit(dummyService::doSomething);
        es.submit(dummyService::doSomething);
        es.submit(dummyService::doSomething);
        es.submit(dummyService::doSomething);

        await()
            .atMost(1, TimeUnit.SECONDS)
            .until(() -> bulkhead.getMetrics().getAvailableConcurrentCalls() == 0);

        assertThat(bulkhead.getBulkheadConfig().getMaxWaitDuration().toMillis()).isEqualTo(0);
        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(1);

        Callable<Boolean> booleanCallable = () ->
            bulkhead.getMetrics().getAvailableConcurrentCalls() == 1;
        await()
            .atMost(2, TimeUnit.SECONDS)
            .until(booleanCallable);
        // Test Actuator endpoints

        ResponseEntity<BulkheadEndpointResponse> bulkheadList = restTemplate
            .getForEntity("/bulkhead", BulkheadEndpointResponse.class);
        assertThat(bulkheadList.getBody().getBulkheads()).hasSize(2)
            .containsExactly("backendA", "backendB");

        for (int i = 0; i < 5; i++) {
            es.submit(dummyService::doSomething);
        }

        await()
            .atMost(2, TimeUnit.SECONDS)
            .until(booleanCallable);

        ResponseEntity<BulkheadEventsEndpointResponse> bulkheadEventList = restTemplate
            .getForEntity("/bulkhead/events", BulkheadEventsEndpointResponse.class);
        List<BulkheadEventDTO> bulkheadEvents = bulkheadEventList.getBody().getBulkheadEvents();
        assertThat(bulkheadEvents).isNotEmpty();
        assertThat(bulkheadEvents.get(bulkheadEvents.size() - 1).getType())
            .isEqualTo(BulkheadEvent.Type.CALL_FINISHED);
        assertThat(bulkheadEvents)
            .filteredOn(it -> it.getType() == BulkheadEvent.Type.CALL_REJECTED)
            .isNotEmpty();

        assertThat(bulkheadAspect.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);

        es.shutdown();
    }
}
