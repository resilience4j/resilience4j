package io.github.resilience4j.springboot.ratelimiter.autoconfigure;

import io.github.resilience4j.springboot.TestUtils;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.spring6.fallback.FallbackExecutor;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.spring6.ratelimiter.configure.RateLimiterAspect;
import io.github.resilience4j.spring6.ratelimiter.configure.RateLimiterAspectExt;
import io.github.resilience4j.spring6.ratelimiter.configure.RateLimiterConfiguration;
import io.github.resilience4j.spring6.ratelimiter.configure.RateLimiterConfigurationProperties;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import io.github.resilience4j.spring6.spelresolver.SpelResolver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    HealthContributorAutoConfiguration.class,
    RateLimiterConfigurationOnMissingBeanTest.ConfigWithOverrides.class,
    RateLimiterAutoConfiguration.class,
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
        TestUtils.assertAnnotations(RateLimiterConfiguration.class, RateLimiterAutoConfiguration.class);
    }

    @Test
    public void testAllCircuitBreakerConfigurationBeansOverridden() {
        assertEquals(rateLimiterRegistry, configWithOverrides.rateLimiterRegistry);
        assertEquals(rateLimiterAspect, configWithOverrides.rateLimiterAspect);
        assertEquals(rateLimiterEventsConsumerRegistry,
            configWithOverrides.rateLimiterEventsConsumerRegistry);
    }

    @Configuration
    public static class ConfigWithOverrides {

        public RateLimiterRegistry rateLimiterRegistry;

        public RateLimiterAspect rateLimiterAspect;

        public EventConsumerRegistry<RateLimiterEvent> rateLimiterEventsConsumerRegistry;

        @Bean
        public RateLimiterRegistry rateLimiterRegistry() {
            rateLimiterRegistry = RateLimiterRegistry.of(RateLimiterConfig.ofDefaults());
            return rateLimiterRegistry;
        }

        @Bean
        public RateLimiterAspect rateLimiterAspect(
            RateLimiterRegistry rateLimiterRegistry,
            @Autowired(required = false) List<RateLimiterAspectExt> rateLimiterAspectExtList,
            FallbackExecutor fallbackExecutor,
            SpelResolver spelResolver
        ) {
            rateLimiterAspect = new RateLimiterAspect(
                rateLimiterRegistry,
                new RateLimiterConfigurationProperties(),
                rateLimiterAspectExtList,
                fallbackExecutor,
                spelResolver
            );
            return rateLimiterAspect;
        }

        @Bean
        public EventConsumerRegistry<RateLimiterEvent> rateLimiterEventsConsumerRegistry() {
            rateLimiterEventsConsumerRegistry = new DefaultEventConsumerRegistry<>();
            return rateLimiterEventsConsumerRegistry;
        }

    }
}
