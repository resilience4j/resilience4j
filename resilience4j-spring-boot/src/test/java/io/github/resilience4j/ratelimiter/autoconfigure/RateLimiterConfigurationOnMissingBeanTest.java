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
package io.github.resilience4j.ratelimiter.autoconfigure;

import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.fallback.FallbackDecorators;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.configure.RateLimiterAspect;
import io.github.resilience4j.ratelimiter.configure.RateLimiterAspectExt;
import io.github.resilience4j.ratelimiter.configure.RateLimiterConfiguration;
import io.github.resilience4j.ratelimiter.configure.RateLimiterConfigurationProperties;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.HealthIndicatorAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.BDDAssertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    HealthIndicatorAutoConfiguration.class,
    RateLimiterConfigurationOnMissingBeanTest.ConfigWithOverrides.class,
    RateLimiterAutoConfiguration.class,
    RateLimiterConfigurationOnMissingBean.class
})
@EnableConfigurationProperties(RateLimiterProperties.class)
public class RateLimiterConfigurationOnMissingBeanTest {

    @Autowired
    public ConfigWithOverrides configWithOverrides;

    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;

    @Autowired
    private RateLimiterAspect rateLimiterAspect;

    @Autowired
    private EventConsumerRegistry<RateLimiterEvent> rateLimiterEventsConsumerRegistry;

    @Test
    public void testAllBeansFromCircuitBreakerConfigurationHasOnMissingBean()
        throws NoSuchMethodException {
        final Class<RateLimiterConfiguration> originalClass = RateLimiterConfiguration.class;
        final Class<RateLimiterConfigurationOnMissingBean> onMissingBeanClass = RateLimiterConfigurationOnMissingBean.class;

        for (Method methodCircuitBreakerConfiguration : originalClass.getMethods()) {
            if (methodCircuitBreakerConfiguration.isAnnotationPresent(Bean.class)) {
                final Method methodOnMissing = onMissingBeanClass
                    .getMethod(methodCircuitBreakerConfiguration.getName(),
                        methodCircuitBreakerConfiguration.getParameterTypes());

                assertThat(methodOnMissing.isAnnotationPresent(Bean.class)).isTrue();

                if (!"rateLimiterEventsConsumerRegistry".equals(methodOnMissing.getName()) &&
                    !"rateLimiterRegistryEventConsumer".equals(methodOnMissing.getName())) {
                    assertThat(methodOnMissing.isAnnotationPresent(ConditionalOnMissingBean.class))
                        .isTrue();
                }
            }
        }
    }

    @Test
    public void testAllCircuitBreakerConfigurationBeansOverridden() {
        assertEquals(rateLimiterRegistry, configWithOverrides.rateLimiterRegistry);
        assertEquals(rateLimiterAspect, configWithOverrides.rateLimiterAspect);
        assertNotEquals(rateLimiterEventsConsumerRegistry,
            configWithOverrides.rateLimiterEventsConsumerRegistry);
    }

    @Configuration
    public static class ConfigWithOverrides {

        private RateLimiterRegistry rateLimiterRegistry;

        private RateLimiterAspect rateLimiterAspect;

        private EventConsumerRegistry<RateLimiterEvent> rateLimiterEventsConsumerRegistry;

        @Bean
        public RateLimiterRegistry rateLimiterRegistry() {
            rateLimiterRegistry = RateLimiterRegistry.of(RateLimiterConfig.ofDefaults());
            return rateLimiterRegistry;
        }

        @Bean
        public RateLimiterAspect rateLimiterAspect(RateLimiterRegistry rateLimiterRegistry,
            @Autowired(required = false) List<RateLimiterAspectExt> rateLimiterAspectExtList,
            FallbackDecorators fallbackDecorators) {
            rateLimiterAspect = new RateLimiterAspect(rateLimiterRegistry,
                new RateLimiterConfigurationProperties(), rateLimiterAspectExtList,
                fallbackDecorators);
            return rateLimiterAspect;
        }

        @Bean
        public EventConsumerRegistry<RateLimiterEvent> rateLimiterEventsConsumerRegistry() {
            rateLimiterEventsConsumerRegistry = new DefaultEventConsumerRegistry<>();
            return rateLimiterEventsConsumerRegistry;
        }

    }
}