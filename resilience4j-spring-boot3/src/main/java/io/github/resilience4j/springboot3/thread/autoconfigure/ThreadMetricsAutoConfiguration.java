/*
 * Copyright 2025
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
package io.github.resilience4j.springboot3.thread.autoconfigure;

import io.github.resilience4j.micrometer.tagged.ThreadMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * {@link AutoConfiguration Auto-configuration} for resilience4j thread metrics.
 * 
 * @author kanghyun.yang
 * @since 3.0.0
 */
@AutoConfiguration(after = {
    MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class,
    Resilience4jThreadAutoConfiguration.class
})
@ConditionalOnClass({MeterRegistry.class, ThreadMetrics.class})
@ConditionalOnBean(MeterRegistry.class)
@ConditionalOnProperty(value = "resilience4j.thread.metrics.enabled", matchIfMissing = true)
public class ThreadMetricsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ThreadMetrics threadMetrics(MeterRegistry meterRegistry) {
        return ThreadMetrics.ofMeterRegistry(meterRegistry);
    }
}