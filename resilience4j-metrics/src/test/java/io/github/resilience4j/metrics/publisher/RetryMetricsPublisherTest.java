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
import io.github.resilience4j.metrics.AbstractRetryMetricsTest;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;

public class RetryMetricsPublisherTest extends AbstractRetryMetricsTest {

    @Override
    protected Retry givenMetricRegistry(String prefix, MetricRegistry metricRegistry) {
        RetryRegistry retryRegistry = RetryRegistry.of(RetryConfig.ofDefaults(), new RetryMetricsPublisher(prefix, metricRegistry));

        return retryRegistry.retry("testName");
    }

    @Override
    protected Retry givenMetricRegistry(MetricRegistry metricRegistry) {
        RetryRegistry retryRegistry = RetryRegistry.of(RetryConfig.ofDefaults(), new RetryMetricsPublisher(metricRegistry));

        return retryRegistry.retry("testName");
    }
}