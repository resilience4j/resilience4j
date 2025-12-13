/*
 * Copyright 2025 Bohdan Storozhuk, Artur Havliukovskyi
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
package io.github.resilience4j.springboot.ratelimiter.autoconfigure;

import io.github.resilience4j.micrometer.tagged.TaggedRateLimiterMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedRateLimiterMetricsPublisher;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration Auto-configuration} for
 * resilience4j-metrics.
 */
@AutoConfiguration(afterName = { "org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration",
        "org.springframework.boot.micrometer.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration" })
@ConditionalOnClass({MeterRegistry.class, RateLimiter.class,
    TaggedRateLimiterMetricsPublisher.class})
@ConditionalOnProperty(value = "resilience4j.ratelimiter.metrics.enabled", matchIfMissing = true)
public class RateLimiterMetricsAutoConfiguration {

    @Bean
    @ConditionalOnProperty(value = "resilience4j.ratelimiter.metrics.legacy.enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public TaggedRateLimiterMetrics registerRateLimiterMetrics(
        RateLimiterRegistry rateLimiterRegistry) {
        return TaggedRateLimiterMetrics.ofRateLimiterRegistry(rateLimiterRegistry);
    }

    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnProperty(value = "resilience4j.ratelimiter.metrics.legacy.enabled", havingValue = "false", matchIfMissing = true)
    @ConditionalOnMissingBean
    public TaggedRateLimiterMetricsPublisher taggedRateLimiterMetricsPublisher(
        MeterRegistry meterRegistry) {
        return new TaggedRateLimiterMetricsPublisher(meterRegistry);
    }

}
