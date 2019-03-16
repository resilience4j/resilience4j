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
package io.github.resilience4j.bulkhead.monitoring.health;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.util.Optional;

/**
 * A Spring Boot health indicators which adds the state of a Bulkhead and it's metrics to the health endpoints
 */
public class BulkheadHealthIndicator implements HealthIndicator {

    private static final String AVAILABLE_CONCURRENT_CALL = "availableConcurrentCall";
    private static final String MAX_ALLOWED_CONCURRENT_CALL = "maxAllowedConcurrentCall";
    private static final String MAX_WAIT_TIME = "maxWaitTime";
    private Bulkhead bulkhead;

    public BulkheadHealthIndicator(Bulkhead bulkhead) {
        this.bulkhead = bulkhead;
    }

    @Override
    public Health health() {
        return Optional.of(bulkhead)
                .map(this::mapBackendMonitorState)
                .orElse(Health.up().build());
    }

    private Health mapBackendMonitorState(Bulkhead bulkhead) {
        return addDetails(Health.up(), bulkhead).build();
    }

    private Health.Builder addDetails(Health.Builder builder, Bulkhead bulkhead) {
        Bulkhead.Metrics metrics = bulkhead.getMetrics();
        BulkheadConfig config = bulkhead.getBulkheadConfig();
        builder.withDetail(AVAILABLE_CONCURRENT_CALL, metrics.getAvailableConcurrentCalls())
            .withDetail(MAX_ALLOWED_CONCURRENT_CALL, metrics.getMaxAllowedConcurrentCalls())
            .withDetail(MAX_WAIT_TIME, config.getMaxWaitTime() + "ms");
        return builder;
    }
}
