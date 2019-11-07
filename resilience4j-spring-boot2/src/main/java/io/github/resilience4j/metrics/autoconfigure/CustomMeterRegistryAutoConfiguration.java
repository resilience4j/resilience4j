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
package io.github.resilience4j.metrics.autoconfigure;

import io.github.resilience4j.bulkhead.autoconfigure.BulkheadMetricsAutoConfiguration;
import io.github.resilience4j.bulkhead.autoconfigure.ThreadPoolBulkheadMetricsAutoConfiguration;
import io.github.resilience4j.circuitbreaker.autoconfigure.CircuitBreakerMetricsAutoConfiguration;
import io.github.resilience4j.ratelimiter.autoconfigure.RateLimiterMetricsAutoConfiguration;
import io.github.resilience4j.retry.autoconfigure.RetryMetricsAutoConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Meter Registry AutoConfiguration for resilience4j-metrics when MetricsAutoConfiguration is disabled.
 */
@Configuration
@ConditionalOnMissingBean(MeterRegistry.class)
@AutoConfigureAfter(MetricsAutoConfiguration.class)
@AutoConfigureBefore({BulkheadMetricsAutoConfiguration.class, ThreadPoolBulkheadMetricsAutoConfiguration.class, CircuitBreakerMetricsAutoConfiguration.class, RateLimiterMetricsAutoConfiguration.class
, RetryMetricsAutoConfiguration.class})
public class CustomMeterRegistryAutoConfiguration {

    @Bean
    public MeterRegistry getMeterRegistry(){
        return new CompositeMeterRegistry();
    }

}
