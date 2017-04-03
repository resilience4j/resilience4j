/*
 *
 *  Copyright 2017: Robert Winkler
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;

import java.util.Map;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import javaslang.collection.Array;
import javaslang.collection.Seq;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * An adapter which exports {@link CircuitBreaker.Metrics} as Dropwizard Metrics Gauges.
 */
public class CircuitBreakerMetrics implements MetricSet{

    private final MetricRegistry metricRegistry = new MetricRegistry();

    private CircuitBreakerMetrics(Seq<CircuitBreaker> circuitBreakers){
        circuitBreakers.forEach(circuitBreaker -> {
            String name = circuitBreaker.getName();
            CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

            metricRegistry.register(name("resilience4j.circuitbreaker", name, "successful"),
                    (Gauge<Integer>) metrics::getNumberOfSuccessfulCalls);
            metricRegistry.register(name("resilience4j.circuitbreaker", name, "failed"),
                    (Gauge<Integer>) metrics::getNumberOfFailedCalls);
            metricRegistry.register(name("resilience4j.circuitbreaker", name, "not_permitted"),
                    (Gauge<Long>) metrics::getNumberOfNotPermittedCalls);
            metricRegistry.register(name("resilience4j.circuitbreaker", name, "buffered"),
                    (Gauge<Integer>) metrics::getNumberOfBufferedCalls);
            metricRegistry.register(name("resilience4j.circuitbreaker", name, "buffered_max"),
                    (Gauge<Integer>) metrics::getMaxNumberOfBufferedCalls);
            }
        );
    }

    public static CircuitBreakerMetrics of(CircuitBreakerRegistry circuitBreakerRegistry) {
        return new CircuitBreakerMetrics(circuitBreakerRegistry.getAllCircuitBreakers());
    }

    public static CircuitBreakerMetrics of(Seq<CircuitBreaker> circuitBreakers) {
        return new CircuitBreakerMetrics(circuitBreakers);
    }

    public static CircuitBreakerMetrics of(CircuitBreaker circuitBreaker) {
        return new CircuitBreakerMetrics(Array.of(circuitBreaker));
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return metricRegistry.getMetrics();
    }
}
