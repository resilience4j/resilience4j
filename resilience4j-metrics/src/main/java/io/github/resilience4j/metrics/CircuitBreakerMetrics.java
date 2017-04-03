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
            metricRegistry.register(name("resilience4j.circuitbreaker", circuitBreaker.getName(), "successful"),
                    (Gauge<Integer>) () -> circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
            metricRegistry.register(name("resilience4j.circuitbreaker", circuitBreaker.getName(), "failed"),
                    (Gauge<Integer>) () -> circuitBreaker.getMetrics().getNumberOfFailedCalls());
            metricRegistry.register(name("resilience4j.circuitbreaker", circuitBreaker.getName(), "not_permitted"),
                    (Gauge<Long>) () -> circuitBreaker.getMetrics().getNumberOfNotPermittedCalls());
            metricRegistry.register(name("resilience4j.circuitbreaker", circuitBreaker.getName(), "buffered"),
                    (Gauge<Integer>) () -> circuitBreaker.getMetrics().getNumberOfBufferedCalls());
            metricRegistry.register(name("resilience4j.circuitbreaker", circuitBreaker.getName(), "buffered_max"),
                    (Gauge<Integer>) () -> circuitBreaker.getMetrics().getMaxNumberOfBufferedCalls());
            }
        );
    }

    public static CircuitBreakerMetrics of(CircuitBreakerRegistry circuitBreakerRegistry) {
        return new CircuitBreakerMetrics(circuitBreakerRegistry.getAllCircuitBreakers());
    }

    public static CircuitBreakerMetrics of(CircuitBreaker circuitBreaker) {
        return new CircuitBreakerMetrics(Array.of(circuitBreaker));
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return metricRegistry.getMetrics();
    }
}
