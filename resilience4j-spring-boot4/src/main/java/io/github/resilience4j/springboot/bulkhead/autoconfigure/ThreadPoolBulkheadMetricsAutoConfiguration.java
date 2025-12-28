/*
 * Copyright 2025 Ingyu Hwang, Artur Havliukovskyi
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

package io.github.resilience4j.springboot.bulkhead.autoconfigure;

import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedThreadPoolBulkheadMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedThreadPoolBulkheadMetricsPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(afterName = { "org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration",
        "org.springframework.boot.micrometer.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration" })
@ConditionalOnClass({MeterRegistry.class, ThreadPoolBulkhead.class,
        TaggedThreadPoolBulkheadMetricsPublisher.class})
@ConditionalOnProperty(value = "resilience4j.thread-pool-bulkhead.metrics.enabled", matchIfMissing = true)
public class ThreadPoolBulkheadMetricsAutoConfiguration {

    @Bean
    @ConditionalOnProperty(value = "resilience4j.thread-pool-bulkhead.metrics.legacy.enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public TaggedThreadPoolBulkheadMetrics registerThreadPoolBulkheadMetrics(
        ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry) {
        return TaggedThreadPoolBulkheadMetrics
            .ofThreadPoolBulkheadRegistry(threadPoolBulkheadRegistry);
    }

    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnProperty(value = "resilience4j.thread-pool-bulkhead.metrics.legacy.enabled", havingValue = "false", matchIfMissing = true)
    @ConditionalOnMissingBean
    public TaggedThreadPoolBulkheadMetricsPublisher taggedThreadPoolBulkheadMetricsPublisher(
        MeterRegistry meterRegistry) {
        return new TaggedThreadPoolBulkheadMetricsPublisher(meterRegistry);
    }

}
