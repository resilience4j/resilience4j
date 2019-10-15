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

public class CircuitBreakerMetricsPublisherTest extends AbstractCircuitBreakerMetricsTest {

    @Override
    protected CircuitBreaker givenMetricRegistry(String prefix, MetricRegistry metricRegistry) {
        CircuitBreakerRegistry circuitBreakerRegistry =
                CircuitBreakerRegistry.of(CircuitBreakerConfig.ofDefaults(), new CircuitBreakerMetricsPublisher(prefix, metricRegistry));

        return circuitBreakerRegistry.circuitBreaker("testName");
    }

    @Override
    protected CircuitBreaker givenMetricRegistry(MetricRegistry metricRegistry) {
        CircuitBreakerRegistry circuitBreakerRegistry =
                CircuitBreakerRegistry.of(CircuitBreakerConfig.ofDefaults(), new CircuitBreakerMetricsPublisher(metricRegistry));

        return circuitBreakerRegistry.circuitBreaker("testName");
    }
}