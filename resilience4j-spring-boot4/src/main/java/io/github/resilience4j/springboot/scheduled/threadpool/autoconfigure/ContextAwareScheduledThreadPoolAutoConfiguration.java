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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.task.ThreadPoolTaskSchedulerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ScheduledThreadPoolExecutor;

@AutoConfiguration
@ConditionalOnProperty(value = "resilience4j.scheduled.executor.core-pool-size")
@ConditionalOnMissingBean(name = ContextAwareScheduledThreadPoolAutoConfiguration.EXECUTOR_NAME, value = ScheduledThreadPoolExecutor.class)
@EnableConfigurationProperties({ContextAwareScheduledThreadPoolProperties.class})
public class ContextAwareScheduledThreadPoolAutoConfiguration {

    public static final String EXECUTOR_NAME = "resilience4jTaskExecutor";
    public static final String SCHEDULER_NAME = "resilience4jThreadPoolTaskScheduler";

    @Bean(name = EXECUTOR_NAME, defaultCandidate = false)
    @ConditionalOnProperty(value = "resilience4j.scheduled.executor.type", havingValue = "resilience4j", matchIfMissing = true)
    public ScheduledThreadPoolExecutor getContextAwareScheduledThreadPool(ContextAwareScheduledThreadPoolProperties contextAwareScheduledThreadPoolProperties) {
        return contextAwareScheduledThreadPoolProperties.build();
    }

    @Configuration
    @ConditionalOnBean(ThreadPoolTaskSchedulerBuilder.class)
    @ConditionalOnMissingBean(name = EXECUTOR_NAME, value = ScheduledThreadPoolExecutor.class)
    @ConditionalOnProperty(value = "resilience4j.scheduled.executor.type", havingValue = "spring")
    public static class SpringManagedScheduledThreadPoolConfiguration {

        @Bean(name = SCHEDULER_NAME, defaultCandidate = false)
        @ConditionalOnMissingBean(name = SCHEDULER_NAME)
        public ThreadPoolTaskScheduler getScheduledThreadPoolExecutor(
                ContextAwareScheduledThreadPoolProperties poolProperties,
                ObjectProvider<Resilience4JThreadPoolTaskSchedulerBuilderCustomizer> customizers,
                ThreadPoolTaskSchedulerBuilder builder) {
            ThreadPoolTaskSchedulerBuilder actualBuilder = builder.threadNamePrefix("resilience4j-").poolSize(poolProperties.getCorePoolSize());
            for (Resilience4JThreadPoolTaskSchedulerBuilderCustomizer customizer : customizers) {
                actualBuilder = customizer.customize(builder);
            }
            return actualBuilder.build();
        }

        @Bean(name = EXECUTOR_NAME, defaultCandidate = false)
        public ScheduledThreadPoolExecutor getSpringManagedScheduledThreadPoolExecutor(@Qualifier(ContextAwareScheduledThreadPoolAutoConfiguration.SCHEDULER_NAME) ThreadPoolTaskScheduler threadPoolTaskScheduler) {
            return threadPoolTaskScheduler.getScheduledThreadPoolExecutor();
        }
    }
}
