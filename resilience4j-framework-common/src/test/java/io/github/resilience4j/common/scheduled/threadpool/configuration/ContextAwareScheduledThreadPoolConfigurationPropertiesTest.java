/*
 *
 *  Copyright 2020 krnsaurabh
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
package io.github.resilience4j.common.scheduled.threadpool.configuration;

import io.github.resilience4j.core.ContextAwareScheduledThreadPoolExecutor;
import io.github.resilience4j.test.TestContextPropagators.TestThreadLocalContextPropagatorWithHolder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ContextAwareScheduledThreadPoolConfigurationPropertiesTest {

    @Test
    public void buildPropertiesWithValidArguments() {
        ContextAwareScheduledThreadPoolConfigurationProperties poolConfigurationProperties = new ContextAwareScheduledThreadPoolConfigurationProperties();
        poolConfigurationProperties.setContextPropagators(TestThreadLocalContextPropagatorWithHolder.class);
        poolConfigurationProperties.setCorePoolSize(10);

        final ContextAwareScheduledThreadPoolExecutor contextAwareScheduledThreadPoolExecutor = poolConfigurationProperties.build();
        assertThat(contextAwareScheduledThreadPoolExecutor.getCorePoolSize()).isEqualTo(10);
        assertThat(contextAwareScheduledThreadPoolExecutor.getContextPropagators()).hasSize(1).hasOnlyElementsOfTypes(TestThreadLocalContextPropagatorWithHolder.class);
    }

    @Test
    public void shouldThrowErrorWhenCorePoolSizeIsLessThanOne() {
        ContextAwareScheduledThreadPoolConfigurationProperties poolConfigurationProperties = new ContextAwareScheduledThreadPoolConfigurationProperties();
        poolConfigurationProperties.setContextPropagators(TestThreadLocalContextPropagatorWithHolder.class);
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> poolConfigurationProperties.setCorePoolSize(0));
    }

    @Test
    public void buildPropertiesWithNoContextPropagator() {
        ContextAwareScheduledThreadPoolConfigurationProperties poolConfigurationProperties = new ContextAwareScheduledThreadPoolConfigurationProperties();
        poolConfigurationProperties.setCorePoolSize(10);

        final ContextAwareScheduledThreadPoolExecutor contextAwareScheduledThreadPoolExecutor = poolConfigurationProperties.build();
        assertThat(contextAwareScheduledThreadPoolExecutor.getCorePoolSize()).isEqualTo(10);
        assertThat(contextAwareScheduledThreadPoolExecutor.getContextPropagators()).isEmpty();
    }

    @Test
    public void shouldThrowErrorIfPropertiesNotSet() {
        ContextAwareScheduledThreadPoolConfigurationProperties poolConfigurationProperties = new ContextAwareScheduledThreadPoolConfigurationProperties();
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(poolConfigurationProperties::build);
    }

}
