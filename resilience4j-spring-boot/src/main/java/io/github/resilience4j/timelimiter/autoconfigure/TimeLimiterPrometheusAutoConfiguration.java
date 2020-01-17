/*
 * Copyright 2020 Ingyu Hwang
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

package io.github.resilience4j.timelimiter.autoconfigure;

import io.github.resilience4j.prometheus.collectors.TimeLimiterMetricsCollector;
import io.github.resilience4j.prometheus.publisher.TimeLimiterMetricsPublisher;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.prometheus.client.GaugeMetricFamily;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass({GaugeMetricFamily.class, TimeLimiter.class, TimeLimiterMetricsPublisher.class})
@ConditionalOnProperty(value = "resilience4j.timelimiter.metrics.enabled", matchIfMissing = true)
public class TimeLimiterPrometheusAutoConfiguration {

    @Bean
    @ConditionalOnProperty(value = "resilience4j.timelimiter.metrics.legacy.enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public TimeLimiterMetricsCollector timeLimiterPrometheusCollector(
        TimeLimiterRegistry timeLimiterRegistry) {
        TimeLimiterMetricsCollector collector = TimeLimiterMetricsCollector
            .ofTimeLimiterRegistry(timeLimiterRegistry);
        collector.register();
        return collector;
    }

    @Bean
    @ConditionalOnProperty(value = "resilience4j.timelimiter.metrics.legacy.enabled", havingValue = "false", matchIfMissing = true)
    @ConditionalOnMissingBean
    public TimeLimiterMetricsPublisher timeLimiterPrometheusPublisher() {
        return new TimeLimiterMetricsPublisher();
    }

}
