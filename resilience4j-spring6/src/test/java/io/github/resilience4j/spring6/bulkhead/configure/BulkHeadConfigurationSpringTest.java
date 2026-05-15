/*
 * Copyright 2026 Mahmoud Romeh
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
package io.github.resilience4j.spring6.bulkhead.configure;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.common.bulkhead.configuration.CommonThreadPoolBulkheadConfigurationProperties;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.spring6.TestThreadLocalContextPropagator;
import io.github.resilience4j.spring6.fallback.FallbackExecutor;
import io.github.resilience4j.spring6.spelresolver.SpelResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    BulkHeadConfigurationSpringTest.ConfigWithOverrides.class
})
class BulkHeadConfigurationSpringTest {

    @Autowired
    private ConfigWithOverrides configWithOverrides;

    @Test
    void testAllCircuitBreakerConfigurationBeansOverridden() {
        assertThat(configWithOverrides.bulkheadRegistry).isNotNull();
        assertThat(configWithOverrides.bulkheadAspect).isNotNull();
        assertThat(configWithOverrides.bulkheadConfigurationProperties).isNotNull();
        assertThat(configWithOverrides.threadPoolBulkheadConfigurationProperties).isNotNull();
        assertThat(configWithOverrides.bulkheadEventEventConsumerRegistry).isNotNull();
        assertThat(configWithOverrides.threadPoolBulkheadRegistry).isNotNull();
        assertThat(configWithOverrides.threadPoolBulkheadConfigurationProperties.getConfigs().containsKey("sharedBackend"));
        assertThat(configWithOverrides.threadPoolBulkheadConfigurationProperties.getConfigs().get("sharedBackend").getContextPropagators()).isNotNull();
        assertThat(configWithOverrides.threadPoolBulkheadConfigurationProperties.getConfigs().get("sharedBackend").getContextPropagators().length).isEqualTo(1);
        assertThat(configWithOverrides.threadPoolBulkheadConfigurationProperties.getConfigs().get("sharedBackend").getContextPropagators()[0]).isEqualTo(TestThreadLocalContextPropagator.class);
        assertThat(configWithOverrides.bulkheadConfigurationProperties.getConfigs()).hasSize(1);
    }

    @Configuration
    @ComponentScan({"io.github.resilience4j.spring6.bulkhead", "io.github.resilience4j.spring6.fallback", "io.github.resilience4j.spring6.spelresolver"})
    static class ConfigWithOverrides {

        private BulkheadRegistry bulkheadRegistry;

        private ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry;

        private BulkheadAspect bulkheadAspect;

        private EventConsumerRegistry<BulkheadEvent> bulkheadEventEventConsumerRegistry;

        private BulkheadConfigurationProperties bulkheadConfigurationProperties;

        private CommonThreadPoolBulkheadConfigurationProperties threadPoolBulkheadConfigurationProperties;

        @Bean
        public ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry() {
            threadPoolBulkheadRegistry = ThreadPoolBulkheadRegistry.ofDefaults();
            return threadPoolBulkheadRegistry;
        }

        @Bean
        public BulkheadRegistry bulkheadRegistry() {
            bulkheadRegistry = BulkheadRegistry.ofDefaults();
            return bulkheadRegistry;
        }

        @Bean
        public BulkheadAspect bulkheadAspect(
            BulkheadRegistry bulkheadRegistry,
            ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry,
            @Autowired(required = false) List<BulkheadAspectExt> bulkheadAspectExts,
            FallbackExecutor fallbackExecutor,
            SpelResolver spelResolver
        ) {
            bulkheadAspect = new BulkheadAspect(bulkheadConfigurationProperties(),
                threadPoolBulkheadRegistry, bulkheadRegistry, bulkheadAspectExts,
                fallbackExecutor, spelResolver);
            return bulkheadAspect;
        }

        @Bean
        public EventConsumerRegistry<BulkheadEvent> eventConsumerRegistry() {
            bulkheadEventEventConsumerRegistry = new DefaultEventConsumerRegistry<>();
            return bulkheadEventEventConsumerRegistry;
        }

        @Bean
        public BulkheadConfigurationProperties bulkheadConfigurationProperties() {
            bulkheadConfigurationProperties = new BulkheadConfigurationPropertiesTest();
            return bulkheadConfigurationProperties;
        }

        @Bean
        public CommonThreadPoolBulkheadConfigurationProperties threadpoolBulkheadConfigurationProperties() {
            threadPoolBulkheadConfigurationProperties = new ThreadPoolBulkheadConfigurationPropertiesTest();
            return threadPoolBulkheadConfigurationProperties;
        }

        private class BulkheadConfigurationPropertiesTest extends BulkheadConfigurationProperties {

            BulkheadConfigurationPropertiesTest() {
                InstanceProperties instanceProperties = new InstanceProperties();
                instanceProperties.setBaseConfig("sharedConfig");
                instanceProperties.setMaxConcurrentCalls(3);
                getConfigs().put("sharedBackend", instanceProperties);
            }

        }

        private class ThreadPoolBulkheadConfigurationPropertiesTest extends CommonThreadPoolBulkheadConfigurationProperties {

            ThreadPoolBulkheadConfigurationPropertiesTest() {
                InstanceProperties instanceProperties = new InstanceProperties();
                instanceProperties.setContextPropagators(TestThreadLocalContextPropagator.class);
                getConfigs().put("sharedBackend", instanceProperties);
            }

        }
    }
}
