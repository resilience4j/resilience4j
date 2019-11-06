/*
 * Copyright 2019 Mahmoud Romeh
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
package io.github.resilience4j.bulkhead.configure;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.fallback.FallbackDecorators;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    BulkHeadConfigurationSpringTest.ConfigWithOverrides.class
})
public class BulkHeadConfigurationSpringTest {

    @Autowired
    private ConfigWithOverrides configWithOverrides;

    @Test
    public void testAllCircuitBreakerConfigurationBeansOverridden() {
        assertNotNull(configWithOverrides.bulkheadRegistry);
        assertNotNull(configWithOverrides.bulkheadAspect);
        assertNotNull(configWithOverrides.bulkheadConfigurationProperties);
        assertNotNull(configWithOverrides.bulkheadEventEventConsumerRegistry);
        assertNotNull(configWithOverrides.threadPoolBulkheadRegistry);
        assertTrue(configWithOverrides.bulkheadConfigurationProperties.getConfigs().size() == 1);
    }

    @Configuration
    @ComponentScan({"io.github.resilience4j.bulkhead", "io.github.resilience4j.fallback"})
    public static class ConfigWithOverrides {

        private BulkheadRegistry bulkheadRegistry;

        private ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry;

        private BulkheadAspect bulkheadAspect;

        private EventConsumerRegistry<BulkheadEvent> bulkheadEventEventConsumerRegistry;

        private BulkheadConfigurationProperties bulkheadConfigurationProperties;

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
        public BulkheadAspect bulkheadAspect(BulkheadRegistry bulkheadRegistry,
            ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry,
            @Autowired(required = false) List<BulkheadAspectExt> bulkheadAspectExts,
            FallbackDecorators fallbackDecorators) {
            bulkheadAspect = new BulkheadAspect(bulkheadConfigurationProperties(),
                threadPoolBulkheadRegistry, bulkheadRegistry, bulkheadAspectExts,
                fallbackDecorators);
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

        private class BulkheadConfigurationPropertiesTest extends BulkheadConfigurationProperties {

            BulkheadConfigurationPropertiesTest() {
                InstanceProperties instanceProperties = new InstanceProperties();
                instanceProperties.setBaseConfig("sharedConfig");
                instanceProperties.setMaxConcurrentCalls(3);
                getConfigs().put("sharedBackend", instanceProperties);
            }

        }
    }


}