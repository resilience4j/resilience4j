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
import org.junit.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.util.AbstractMap.SimpleEntry;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BulkheadHealthIndicatorTest {
    @Test
    public void health() throws Exception {
        // given
        BulkheadConfig config = mock(BulkheadConfig.class);
        Bulkhead.Metrics metrics = mock(Bulkhead.Metrics.class);
        Bulkhead bulkhead = mock(Bulkhead.class);
        BulkheadHealthIndicator healthIndicator = new BulkheadHealthIndicator(bulkhead);

        //when
        when(config.getMaxWaitTime()).thenReturn(2L);

        when(metrics.getMaxAllowedConcurrentCalls()).thenReturn(2);
        when(metrics.getAvailableConcurrentCalls()).thenReturn(1);

        when(bulkhead.getBulkheadConfig()).thenReturn(config);
        when(bulkhead.getMetrics()).thenReturn(metrics);

        // then
        Health health = healthIndicator.health();
        then(health.getStatus()).isEqualTo(Status.UP);

        then(health.getDetails())
            .contains(
                entry("maxAllowedConcurrentCall", 2),
                entry("maxWaitTime", "2ms"),
                entry("availableConcurrentCall", 1)
            );
    }

    private SimpleEntry<String, ?> entry(String key, Object value) {
        return new SimpleEntry<>(key, value);
    }
}