/*
 * Copyright 2025 Mahmoud Romeh, Artur Havliukovskyi
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
package io.github.resilience4j.springboot.retry.autoconfigure;

import io.github.resilience4j.springboot.TestUtils;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.ContextAwareScheduledThreadPoolExecutor;
import io.github.resilience4j.spring6.fallback.FallbackExecutor;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.spring6.retry.configure.RetryAspect;
import io.github.resilience4j.spring6.retry.configure.RetryAspectExt;
import io.github.resilience4j.spring6.retry.configure.RetryConfiguration;
import io.github.resilience4j.retry.event.RetryEvent;
import io.github.resilience4j.spring6.spelresolver.SpelResolver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    RetryConfigurationOnMissingBeanTest.ConfigWithOverrides.class,
    RetryAutoConfiguration.class,
})
@EnableConfigurationProperties(RetryProperties.class)
public class RetryConfigurationOnMissingBeanTest {

    @Autowired
    private ConfigWithOverrides configWithOverrides;

    @Autowired
    private RetryRegistry retryRegistry;

    @Autowired
    private RetryAspect retryAspect;

    @Autowired
    private EventConsumerRegistry<RetryEvent> retryEventConsumerRegistry;

    @Test
    public void testAllBeansFromRetryHasOnMissingBean() throws NoSuchMethodException {
        TestUtils.assertAnnotations(RetryConfiguration.class, RetryAutoConfiguration.class);
    }

    @Test
    public void testAllRetryConfigurationBeansOverridden() {
        assertEquals(retryAspect, configWithOverrides.retryAspect);
        assertEquals(retryEventConsumerRegistry, configWithOverrides.retryEventConsumerRegistry);
        assertEquals(retryRegistry, configWithOverrides.retryRegistry);
    }

    @Configuration
    public static class ConfigWithOverrides {

        private RetryRegistry retryRegistry;

        private RetryAspect retryAspect;

        private EventConsumerRegistry<RetryEvent> retryEventConsumerRegistry;

        @Bean
        public RetryRegistry retryRegistry() {
            this.retryRegistry = RetryRegistry.ofDefaults();
            return retryRegistry;
        }

        @Bean
        public RetryAspect retryAspect(
            RetryRegistry retryRegistry,
            @Autowired(required = false) List<RetryAspectExt> retryAspectExts,
            FallbackExecutor fallbackExecutor,
            SpelResolver spelResolver,
            @Autowired(required = false) ContextAwareScheduledThreadPoolExecutor contextAwareScheduledThreadPoolExecutor
        ) {
            this.retryAspect = new RetryAspect(new RetryProperties(), retryRegistry,
                retryAspectExts, fallbackExecutor, spelResolver, contextAwareScheduledThreadPoolExecutor);
            return retryAspect;
        }

        @Bean
        public EventConsumerRegistry<RetryEvent> retryEventConsumerRegistry() {
            this.retryEventConsumerRegistry = new DefaultEventConsumerRegistry<>();
            return retryEventConsumerRegistry;
        }
    }
}
