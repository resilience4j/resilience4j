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
package io.github.resilience4j.ratelimiter.configure;

import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.fallback.FallbackDecorators;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import io.github.resilience4j.spelresolver.SpelResolver;
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
    RateLimiterConfigurationSpringTest.ConfigWithOverrides.class
})
public class RateLimiterConfigurationSpringTest {

    @Autowired
    private ConfigWithOverrides configWithOverrides;


    @Test
    public void testAllCircuitBreakerConfigurationBeansOverridden() {
        assertNotNull(configWithOverrides.rateLimiterRegistry);
        assertNotNull(configWithOverrides.rateLimiterAspect);
        assertNotNull(configWithOverrides.rateLimiterEventEventConsumerRegistry);
        assertNotNull(configWithOverrides.rateLimiterConfigurationProperties);
        assertTrue(configWithOverrides.rateLimiterConfigurationProperties.getConfigs().size() == 1);
    }

    @Configuration
    @ComponentScan({"io.github.resilience4j.ratelimiter", "io.github.resilience4j.fallback", "io.github.resilience4j.spelresolver"})
    public static class ConfigWithOverrides {

        private RateLimiterRegistry rateLimiterRegistry;

        private RateLimiterAspect rateLimiterAspect;

        private EventConsumerRegistry<RateLimiterEvent> rateLimiterEventEventConsumerRegistry;

        private RateLimiterConfigurationProperties rateLimiterConfigurationProperties;

        @Bean
        public RateLimiterRegistry rateLimiterRegistry() {
            rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
            return rateLimiterRegistry;
        }

        @Bean
        public RateLimiterAspect rateLimiterAspect(
            RateLimiterRegistry rateLimiterRegistry,
            @Autowired(required = false) List<RateLimiterAspectExt> rateLimiterAspectExts,
            FallbackDecorators recoveryDecorators,
            SpelResolver spelResolver
        ) {
            rateLimiterAspect = new RateLimiterAspect(rateLimiterRegistry,
                rateLimiterConfigurationProperties(), rateLimiterAspectExts, recoveryDecorators, spelResolver);
            return rateLimiterAspect;
        }

        @Bean
        public EventConsumerRegistry<RateLimiterEvent> eventConsumerRegistry() {
            rateLimiterEventEventConsumerRegistry = new DefaultEventConsumerRegistry<>();
            return rateLimiterEventEventConsumerRegistry;
        }

        @Bean
        public RateLimiterConfigurationProperties rateLimiterConfigurationProperties() {
            rateLimiterConfigurationProperties = new RateLimiterConfigurationPropertiesTest();
            return rateLimiterConfigurationProperties;
        }

        private class RateLimiterConfigurationPropertiesTest extends
            RateLimiterConfigurationProperties {

            RateLimiterConfigurationPropertiesTest() {
                InstanceProperties instanceProperties = new InstanceProperties();
                instanceProperties.setBaseConfig("sharedConfig");
                instanceProperties.setLimitForPeriod(3);
                getConfigs().put("sharedBackend", instanceProperties);
            }

        }
    }

}