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
package io.github.resilience4j.spring6.retry.configure;

import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.ContextAwareScheduledThreadPoolExecutor;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.event.RetryEvent;
import io.github.resilience4j.spring6.fallback.FallbackExecutor;
import io.github.resilience4j.spring6.fallback.configure.FallbackConfiguration;
import io.github.resilience4j.spring6.spelresolver.SpelResolver;
import io.github.resilience4j.spring6.spelresolver.configure.SpelResolverConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    RetryConfigurationSpringTest.ConfigWithOverrides.class
})
class RetryConfigurationSpringTest {

    @Autowired
    private ConfigWithOverrides configWithOverrides;

    @Test
    void testAllCircuitBreakerConfigurationBeansOverridden() {
        assertThat(configWithOverrides.retryRegistry).isNotNull();
        assertThat(configWithOverrides.retryAspect).isNotNull();
        assertThat(configWithOverrides.retryEventEventConsumerRegistry).isNotNull();
        assertThat(configWithOverrides.retryConfigurationProperties).isNotNull();
        assertThat(configWithOverrides.retryConfigurationProperties().getConfigs()).hasSize(1);
    }

    @Configuration
    @Import({FallbackConfiguration.class, SpelResolverConfiguration.class})
    static class ConfigWithOverrides {

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
            FallbackExecutor fallbackExecutor,
            SpelResolver spelResolver
        ) {
            retryAspect = new RetryAspect(retryConfigurationProperties(), retryRegistry,
                retryAspectExts, fallbackExecutor, spelResolver,contextAwareScheduledThreadPoolExecutor);
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
