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
package io.github.resilience4j.retry.configure;

import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.ContextAwareScheduledThreadPoolExecutor;
import io.github.resilience4j.fallback.FallbackDecorators;
import io.github.resilience4j.fallback.configure.FallbackConfiguration;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.event.RetryEvent;
import io.github.resilience4j.spelresolver.SpelResolver;
import io.github.resilience4j.spelresolver.configure.SpelResolverConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    RetryConfigurationSpringTest.ConfigWithOverrides.class
})
public class RetryConfigurationSpringTest {

    @Autowired
    private ConfigWithOverrides configWithOverrides;


    @Test
    public void testAllCircuitBreakerConfigurationBeansOverridden() {
        assertNotNull(configWithOverrides.retryRegistry);
        assertNotNull(configWithOverrides.retryAspect);
        assertNotNull(configWithOverrides.retryEventEventConsumerRegistry);
        assertNotNull(configWithOverrides.retryConfigurationProperties);
        assertTrue(configWithOverrides.retryConfigurationProperties().getConfigs().size() == 1);
    }

    @Configuration
    @Import({FallbackConfiguration.class, SpelResolverConfiguration.class})
    public static class ConfigWithOverrides {

        private RetryRegistry retryRegistry;

        private RetryAspect retryAspect;

        private EventConsumerRegistry<RetryEvent> retryEventEventConsumerRegistry;

        private RetryConfigurationProperties retryConfigurationProperties;

        private ContextAwareScheduledThreadPoolExecutor contextAwareScheduledThreadPoolExecutor;

        @Bean
        public RetryRegistry retryRegistry() {
            retryRegistry = RetryRegistry.ofDefaults();
            return retryRegistry;
        }

        @Bean
        public RetryAspect retryAspect(
            RetryRegistry retryRegistry,
            @Autowired(required = false) List<RetryAspectExt> retryAspectExts,
            FallbackDecorators fallbackDecorators,
            SpelResolver spelResolver
        ) {
            retryAspect = new RetryAspect(retryConfigurationProperties(), retryRegistry,
                retryAspectExts, fallbackDecorators, spelResolver,contextAwareScheduledThreadPoolExecutor);
            return retryAspect;
        }

        @Bean
        public EventConsumerRegistry<RetryEvent> eventConsumerRegistry() {
            retryEventEventConsumerRegistry = new DefaultEventConsumerRegistry<>();
            return retryEventEventConsumerRegistry;
        }

        @Bean
        public RetryConfigurationProperties retryConfigurationProperties() {
            retryConfigurationProperties = new RetryConfigurationPropertiesTest();
            return retryConfigurationProperties;
        }

        private class RetryConfigurationPropertiesTest extends RetryConfigurationProperties {

            RetryConfigurationPropertiesTest() {
                InstanceProperties instanceProperties = new InstanceProperties();
                instanceProperties.setBaseConfig("sharedConfig");
                instanceProperties.setMaxAttempts(3);
                getConfigs().put("sharedBackend", instanceProperties);
            }

        }
    }

}