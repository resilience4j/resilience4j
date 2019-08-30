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

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.vavr.control.Try;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;

import com.codahale.metrics.MetricRegistry;

import static io.github.resilience4j.metrics.assertion.MetricRegistryAssert.assertThat;
import static org.assertj.core.api.BDDAssertions.then;

public class TimeLimiterMetricsTest {

    private static final String DEFAULT_PREFIX = "resilience4j.timelimiter.UNDEFINED.";
    private static final String SUCCESSFUL = "successful";
    private static final String FAILED = "failed";
    private static final String TIMEOUT = "timeout";

    private MetricRegistry metricRegistry;

    @Before
    public void setUp() {
        metricRegistry = new MetricRegistry();
    }

    @Test
    public void shouldRegisterMetrics() throws Exception {
        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter("testLimit");
        metricRegistry.registerAll(TimeLimiterMetrics.ofTimeLimiterRegistry(timeLimiterRegistry));
        String expectedPrefix = "resilience4j.timelimiter.testLimit.";
        Supplier<CompletableFuture<String>> futureSupplier = () ->
                CompletableFuture.completedFuture("Hello world");

        String result = timeLimiter.decorateFutureSupplier(futureSupplier).call();

        then(result).isEqualTo("Hello world");
        assertThat(metricRegistry).hasMetricsSize(3);
        assertThat(metricRegistry).counter(expectedPrefix + SUCCESSFUL)
                .hasValue(1L);
        assertThat(metricRegistry).counter(expectedPrefix + FAILED)
                .hasValue(0L);
        assertThat(metricRegistry).counter(expectedPrefix + TIMEOUT)
                .hasValue(0L);
    }

    @Test
    public void shouldUseCustomPrefix() throws Exception {
        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter("testLimit");
        metricRegistry.registerAll(TimeLimiterMetrics.ofIterable("testPre", timeLimiterRegistry.getAllTimeLimiters()));
        String expectedPrefix = "testPre.testLimit.";
        Supplier<CompletableFuture<String>> futureSupplier = () ->
                CompletableFuture.completedFuture("Hello world");

        String result = timeLimiter.decorateFutureSupplier(futureSupplier).call();

        then(result).isEqualTo("Hello world");
        assertThat(metricRegistry).hasMetricsSize(3);
        assertThat(metricRegistry).counter(expectedPrefix + SUCCESSFUL)
                .hasValue(1L);
        assertThat(metricRegistry).counter(expectedPrefix + FAILED)
                .hasValue(0L);
        assertThat(metricRegistry).counter(expectedPrefix + TIMEOUT)
                .hasValue(0L);
    }

    @Test
    public void shouldRecordSuccesses() {
        TimeLimiter timeLimiter = TimeLimiter.of(TimeLimiterConfig.ofDefaults());
        metricRegistry.registerAll(TimeLimiterMetrics.ofTimeLimiter(timeLimiter));
        Supplier<CompletableFuture<String>> futureSupplier = () ->
                CompletableFuture.completedFuture("Hello world");

        Try.ofCallable(timeLimiter.decorateFutureSupplier(futureSupplier));
        Try.ofCallable(timeLimiter.decorateFutureSupplier(futureSupplier));

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
        Supplier<CompletableFuture<String>> futureSupplier = () ->
                CompletableFuture.supplyAsync(this::fail);

        Try.ofCallable(timeLimiter.decorateFutureSupplier(futureSupplier));
        Try.ofCallable(timeLimiter.decorateFutureSupplier(futureSupplier));

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
        Supplier<CompletableFuture<String>> futureSupplier = () ->
                CompletableFuture.supplyAsync(this::fail);

        Try.ofCallable(timeLimiter.decorateFutureSupplier(futureSupplier));
        Try.ofCallable(timeLimiter.decorateFutureSupplier(futureSupplier));

        assertThat(metricRegistry).hasMetricsSize(3);
        assertThat(metricRegistry).counter(DEFAULT_PREFIX + SUCCESSFUL)
                .hasValue(0L);
        assertThat(metricRegistry).counter(DEFAULT_PREFIX + FAILED)
                .hasValue(0L);
        assertThat(metricRegistry).counter(DEFAULT_PREFIX + TIMEOUT)
                .hasValue(2L);
    }

    private String fail() {
        throw new RuntimeException();
    }

}
