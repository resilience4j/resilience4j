/*
 *
 *  Copyright 2025 krnsaurabh, Artur Havliukovskyi
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
package io.github.resilience4j.springboot.scheduled.threadpool.autoconfigure;

import io.github.resilience4j.core.ContextAwareScheduledThreadPoolExecutor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(value = "resilience4j.scheduled.executor.core-pool-size")
@EnableConfigurationProperties({ContextAwareScheduledThreadPoolProperties.class})
public class ContextAwareScheduledThreadPoolAutoConfiguration {

    @Bean
    public ContextAwareScheduledThreadPoolExecutor getContextAwareScheduledThreadPool(ContextAwareScheduledThreadPoolProperties contextAwareScheduledThreadPoolProperties) {
        return contextAwareScheduledThreadPoolProperties.build();
    }
}
