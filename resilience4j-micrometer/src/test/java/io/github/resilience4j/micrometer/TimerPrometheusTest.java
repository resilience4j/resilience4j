/*
 * Copyright 2026 authors
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
package io.github.resilience4j.micrometer;

import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.BDDAssertions.then;

class TimerPrometheusTest {

    private final List<String> registrationFailures = new ArrayList<>();
    private PrometheusMeterRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        registry.config().onMeterRegistrationFailed((id, reason) -> registrationFailures.add(reason));
    }

    @AfterEach
    void tearDown() {
        registry.close();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldPublishSuccessfulAndFailedCallsInEitherOrder(boolean successFirst) {
        Timer timer = Timer.of("backendA", registry);

        recordCalls(timer, timer, successFirst);
        recordCalls(timer, timer, successFirst);

        then(registry.scrape()).contains(
            "resilience4j_timer_calls_seconds_count{failure=\"\",kind=\"successful\",name=\"backendA\"} 2\n",
            "resilience4j_timer_calls_seconds_count{failure=\"IllegalStateException\",kind=\"failed\",name=\"backendA\"} 2\n");
        then(registrationFailures).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldPublishDifferentTimersWithTheSameMetricName(boolean successFirst) {
        Timer successfulTimer = Timer.of("backendA", registry);
        Timer failedTimer = Timer.of("backendB", registry);

        recordCalls(successfulTimer, failedTimer, successFirst);

        then(registry.scrape()).contains(
            "resilience4j_timer_calls_seconds_count{failure=\"\",kind=\"successful\",name=\"backendA\"} 1\n",
            "resilience4j_timer_calls_seconds_count{failure=\"IllegalStateException\",kind=\"failed\",name=\"backendB\"} 1\n");
        then(registrationFailures).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldPreserveCustomTagsAndFailureClassification(boolean successFirst) {
        AtomicInteger resolvedFailures = new AtomicInteger();
        TimerConfig config = TimerConfig.custom()
            .metricNames("custom.timer.calls")
            .onFailureTagResolver(throwable -> {
                resolvedFailures.incrementAndGet();
                return "custom-" + throwable.getClass().getSimpleName();
            })
            .build();
        Timer timer = Timer.of("backendA", registry, config, Map.of("region", "eu"));

        recordCalls(timer, timer, successFirst);

        then(registry.scrape()).contains(
            "custom_timer_calls_seconds_count{failure=\"\",kind=\"successful\",name=\"backendA\",region=\"eu\"} 1\n",
            "custom_timer_calls_seconds_count{failure=\"custom-IllegalStateException\",kind=\"failed\",name=\"backendA\",region=\"eu\"} 1\n");
        then(resolvedFailures.get()).isEqualTo(1);
        then(registrationFailures).isEmpty();
    }

    private void recordCalls(Timer successfulTimer, Timer failedTimer, boolean successFirst) {
        Runnable successfulCall = () -> successfulTimer.createContext().onSuccess();
        Runnable failedCall = () -> failedTimer.createContext().onFailure(new IllegalStateException());
        if (successFirst) {
            successfulCall.run();
            failedCall.run();
        } else {
            failedCall.run();
            successfulCall.run();
        }
    }
}
