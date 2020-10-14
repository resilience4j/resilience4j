/*
 * Copyright 2019 Ingyu Hwang
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

package io.github.resilience4j.metrics.publisher;

import com.codahale.metrics.MetricRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.metrics.AbstractCircuitBreakerMetricsTest;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

public class CircuitBreakerMetricsPublisherTest extends AbstractCircuitBreakerMetricsTest {

    private CircuitBreakerRegistry circuitBreakerRegistry;
    @Override
    protected CircuitBreaker givenMetricRegistry(String prefix, MetricRegistry metricRegistry) {
        circuitBreakerRegistry = CircuitBreakerRegistry.of(CircuitBreakerConfig.ofDefaults(),
                new CircuitBreakerMetricsPublisher(prefix, metricRegistry));

        return circuitBreakerRegistry.circuitBreaker("testName");
    }

    @Override
    protected CircuitBreaker givenMetricRegistry(MetricRegistry metricRegistry) {
        circuitBreakerRegistry = CircuitBreakerRegistry.of(CircuitBreakerConfig.ofDefaults(),
                new CircuitBreakerMetricsPublisher(metricRegistry));

        return circuitBreakerRegistry.circuitBreaker("testName");
    }

    @Test
    public void shouldRemoveAllMetrics() {
        CircuitBreaker circuitBreaker = givenMetricRegistry("testPrefix", metricRegistry);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        String value = circuitBreaker.executeSupplier(helloWorldService::returnHelloWorld);

        assertThat(value).isEqualTo("Hello world");
        then(helloWorldService).should(times(1)).returnHelloWorld();
        assertThat(metricRegistry.getMetrics()).hasSize(10);

        circuitBreakerRegistry.remove(circuitBreaker.getName());
        assertThat(metricRegistry.getMetrics()).hasSize(0);
    }
}