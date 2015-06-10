/*
 *
 *  Copyright 2015 Robert Winkler
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
package io.github.robwin.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import io.github.robwin.circuitbreaker.CircuitBreaker;
import io.github.robwin.circuitbreaker.CircuitBreakerRegistry;
import javaslang.control.Try;
import org.junit.Test;

import static com.codahale.metrics.MetricRegistry.name;
import static org.assertj.core.api.BDDAssertions.assertThat;

public class MetricsTest {

    @Test
    public void shouldMeasureTime() throws Throwable {
        // Given
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("uniqueName");
        MetricRegistry metricRegistry = new MetricRegistry();
        Timer timer = metricRegistry.timer(name("test"));

        // When I create a long running supplier
        Try.CheckedSupplier<String> supplier = () -> {
            Thread.sleep(2000);
            return "Hello world";
        };

        // And measure the time with Metrics
        Try.CheckedSupplier<String> timedSupplier = Metrics.timedCheckedSupplier(supplier, timer);

        // And decorate it with a CircuitBreaker
        Try.CheckedSupplier<String> circuitBreakerAndTimedSupplier = CircuitBreaker
                .decorateCheckedSupplier(timedSupplier, circuitBreaker);

        String value = circuitBreakerAndTimedSupplier.get();

        // Then the counter of metrics should be one and the
        assertThat(timer.getCount()).isEqualTo(1);
        // and the mean time should be greater than 2
        assertThat(timer.getSnapshot().getMean()).isGreaterThan(2);

        assertThat(value).isEqualTo("Hello world");
    }
}
