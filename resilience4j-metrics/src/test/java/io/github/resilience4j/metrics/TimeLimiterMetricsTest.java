/*
 *
 *  Copyright 2019 authors
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

import com.codahale.metrics.MetricRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static io.github.resilience4j.metrics.assertion.MetricRegistryAssert.assertThat;

public class TimeLimiterMetricsTest extends AbstractTimeLimiterMetricsTest {

    @Override
    protected TimeLimiter given(String prefix, MetricRegistry metricRegistry) {
        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter("testLimit");
        metricRegistry
            .registerAll(TimeLimiterMetrics.ofTimeLimiterRegistry(prefix, timeLimiterRegistry));

        return timeLimiter;
    }

    @Override
    protected TimeLimiter given(MetricRegistry metricRegistry) {
        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter("testLimit");
        metricRegistry.registerAll(TimeLimiterMetrics.ofTimeLimiterRegistry(timeLimiterRegistry));

        return timeLimiter;
    }

    @Test
    public void shouldRecordSuccesses() {
        TimeLimiter timeLimiter = TimeLimiter.of(TimeLimiterConfig.ofDefaults());
        metricRegistry.registerAll(TimeLimiterMetrics.ofTimeLimiter(timeLimiter));

        timeLimiter.onSuccess();
        timeLimiter.onSuccess();

        assertThat(metricRegistry).hasMetricsSize(3);
        assertThat(metricRegistry).counter(DEFAULT_PREFIX + SUCCESSFUL)
            .hasValue(2L);
        assertThat(metricRegistry).counter(DEFAULT_PREFIX + FAILED)
            .hasValue(0L);
        assertThat(metricRegistry).counter(DEFAULT_PREFIX + TIMEOUT)
            .hasValue(0L);
    }

    @Test
    public void shouldRecordErrors() {
        TimeLimiter timeLimiter = TimeLimiter.of(TimeLimiterConfig.ofDefaults());
        metricRegistry.registerAll(TimeLimiterMetrics.ofTimeLimiter(timeLimiter));

        timeLimiter.onError(new RuntimeException());
        timeLimiter.onError(new RuntimeException());

        assertThat(metricRegistry).hasMetricsSize(3);
        assertThat(metricRegistry).counter(DEFAULT_PREFIX + SUCCESSFUL)
            .hasValue(0L);
        assertThat(metricRegistry).counter(DEFAULT_PREFIX + FAILED)
            .hasValue(2L);
        assertThat(metricRegistry).counter(DEFAULT_PREFIX + TIMEOUT)
            .hasValue(0L);
    }

    @Test
    public void shouldRecordTimeouts() {
        TimeLimiter timeLimiter = TimeLimiter.of(TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ZERO)
            .build());
        metricRegistry.registerAll(TimeLimiterMetrics.ofTimeLimiter(timeLimiter));

        timeLimiter.onError(new TimeoutException());
        timeLimiter.onError(new TimeoutException());

        assertThat(metricRegistry).hasMetricsSize(3);
        assertThat(metricRegistry).counter(DEFAULT_PREFIX + SUCCESSFUL)
            .hasValue(0L);
        assertThat(metricRegistry).counter(DEFAULT_PREFIX + FAILED)
            .hasValue(0L);
        assertThat(metricRegistry).counter(DEFAULT_PREFIX + TIMEOUT)
            .hasValue(2L);
    }

}
