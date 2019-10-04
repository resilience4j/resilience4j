package io.github.resilience4j.ratelimiter.autoconfigure;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.health.HealthIndicatorAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.github.resilience4j.TestUtils;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.configure.RateLimiterAspect;
import io.github.resilience4j.ratelimiter.configure.RateLimiterAspectExt;
import io.github.resilience4j.ratelimiter.configure.RateLimiterConfiguration;
import io.github.resilience4j.ratelimiter.configure.RateLimiterConfigurationProperties;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import io.github.resilience4j.fallback.FallbackDecorators;

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
	public void testAllBeansFromCircuitBreakerConfigurationHasOnMissingBean() throws NoSuchMethodException {
		final Class<RateLimiterConfiguration> originalClass = RateLimiterConfiguration.class;
		final Class<RateLimiterConfigurationOnMissingBean> onMissingBeanClass = RateLimiterConfigurationOnMissingBean.class;
		TestUtils.assertAnnotations(originalClass, onMissingBeanClass);
	}

	@Test
	public void testAllCircuitBreakerConfigurationBeansOverridden() {
		assertEquals(rateLimiterRegistry, configWithOverrides.rateLimiterRegistry);
		assertEquals(rateLimiterAspect, configWithOverrides.rateLimiterAspect);
		assertEquals(rateLimiterEventsConsumerRegistry, configWithOverrides.rateLimiterEventsConsumerRegistry);
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
		public RateLimiterAspect rateLimiterAspect(RateLimiterRegistry rateLimiterRegistry, @Autowired(required = false) List<RateLimiterAspectExt> rateLimiterAspectExtList, FallbackDecorators fallbackDecorators) {
			rateLimiterAspect = new RateLimiterAspect(rateLimiterRegistry, new RateLimiterConfigurationProperties(), rateLimiterAspectExtList, fallbackDecorators);
			return rateLimiterAspect;
		}

		@Bean
		public EventConsumerRegistry<RateLimiterEvent> rateLimiterEventsConsumerRegistry() {
			rateLimiterEventsConsumerRegistry = new DefaultEventConsumerRegistry<>();
			return rateLimiterEventsConsumerRegistry;
		}

	}
}