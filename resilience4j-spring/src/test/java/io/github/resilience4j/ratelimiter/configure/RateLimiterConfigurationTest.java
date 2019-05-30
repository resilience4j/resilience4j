package io.github.resilience4j.ratelimiter.configure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;

/**
 * test custom init of rate limiter configuration
 */
@RunWith(MockitoJUnitRunner.class)
public class RateLimiterConfigurationTest {

	@Test
	public void testRateLimiterRegistry() {
		//Given
		RateLimiterConfigurationProperties.LimiterProperties backendProperties1 = new RateLimiterConfigurationProperties.LimiterProperties();
		backendProperties1.setLimitForPeriod(2);
		backendProperties1.setSubscribeForEvents(true);

		RateLimiterConfigurationProperties.LimiterProperties backendProperties2 = new RateLimiterConfigurationProperties.LimiterProperties();
		backendProperties2.setLimitForPeriod(4);
		backendProperties2.setSubscribeForEvents(true);

		RateLimiterConfigurationProperties rateLimiterConfigurationProperties = new RateLimiterConfigurationProperties();
		rateLimiterConfigurationProperties.getLimiters().put("backend1", backendProperties1);
		rateLimiterConfigurationProperties.getLimiters().put("backend2", backendProperties2);

		RateLimiterConfiguration rateLimiterConfiguration = new RateLimiterConfiguration();
		DefaultEventConsumerRegistry<RateLimiterEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

		//When
		RateLimiterRegistry rateLimiterRegistry = rateLimiterConfiguration.rateLimiterRegistry(rateLimiterConfigurationProperties, eventConsumerRegistry);

		//Then
		assertThat(rateLimiterRegistry.getAllRateLimiters().size()).isEqualTo(2);
		RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("backend1");
		assertThat(rateLimiter).isNotNull();
		assertThat(rateLimiter.getRateLimiterConfig().getLimitForPeriod()).isEqualTo(2);

		RateLimiter rateLimiter2 = rateLimiterRegistry.rateLimiter("backend2");
		assertThat(rateLimiter2).isNotNull();
		assertThat(rateLimiter2.getRateLimiterConfig().getLimitForPeriod()).isEqualTo(4);

		assertThat(eventConsumerRegistry.getAllEventConsumer()).hasSize(2);
	}

	@Test
	public void testCreateRateLimiterRegistryWithSharedConfigs() {
		//Given
		RateLimiterConfigurationProperties.LimiterProperties defaultProperties = new RateLimiterConfigurationProperties.LimiterProperties();
		defaultProperties.setLimitForPeriod(3);
		defaultProperties.setLimitRefreshPeriodInMillis(5);
		defaultProperties.setSubscribeForEvents(true);

		RateLimiterConfigurationProperties.LimiterProperties sharedProperties = new RateLimiterConfigurationProperties.LimiterProperties();
		sharedProperties.setLimitForPeriod(2);
		sharedProperties.setLimitRefreshPeriodInMillis(6);
		sharedProperties.setSubscribeForEvents(true);

		RateLimiterConfigurationProperties.LimiterProperties backendWithDefaultConfig = new RateLimiterConfigurationProperties.LimiterProperties();
		backendWithDefaultConfig.setBaseConfig("default");
		backendWithDefaultConfig.setLimitForPeriod(200);
		backendWithDefaultConfig.setSubscribeForEvents(true);

		RateLimiterConfigurationProperties.LimiterProperties backendWithSharedConfig = new RateLimiterConfigurationProperties.LimiterProperties();
		backendWithSharedConfig.setBaseConfig("sharedConfig");
		backendWithSharedConfig.setLimitForPeriod(300);
		backendWithSharedConfig.setSubscribeForEvents(true);

		RateLimiterConfigurationProperties rateLimiterConfigurationProperties = new RateLimiterConfigurationProperties();
		rateLimiterConfigurationProperties.getConfigs().put("default", defaultProperties);
		rateLimiterConfigurationProperties.getConfigs().put("sharedConfig", sharedProperties);

		rateLimiterConfigurationProperties.getLimiters().put("backendWithDefaultConfig", backendWithDefaultConfig);
		rateLimiterConfigurationProperties.getLimiters().put("backendWithSharedConfig", backendWithSharedConfig);

		RateLimiterConfiguration rateLimiterConfiguration = new RateLimiterConfiguration();
		DefaultEventConsumerRegistry<RateLimiterEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

		//When
		RateLimiterRegistry rateLimiterRegistry = rateLimiterConfiguration.rateLimiterRegistry(rateLimiterConfigurationProperties, eventConsumerRegistry);

		//Then
		assertThat(rateLimiterRegistry.getAllRateLimiters().size()).isEqualTo(2);

		// Should get default config and override LimitForPeriod
		RateLimiter rateLimiter1 = rateLimiterRegistry.rateLimiter("backendWithDefaultConfig");
		assertThat(rateLimiter1).isNotNull();
		assertThat(rateLimiter1.getRateLimiterConfig().getLimitForPeriod()).isEqualTo(200);
		assertThat(rateLimiter1.getRateLimiterConfig().getLimitRefreshPeriod()).isEqualTo(Duration.ofMillis(5));

		// Should get shared config and override LimitForPeriod
		RateLimiter rateLimiter2 = rateLimiterRegistry.rateLimiter("backendWithSharedConfig");
		assertThat(rateLimiter2).isNotNull();
		assertThat(rateLimiter2.getRateLimiterConfig().getLimitForPeriod()).isEqualTo(300);
		assertThat(rateLimiter2.getRateLimiterConfig().getLimitRefreshPeriod()).isEqualTo(Duration.ofMillis(6));

		// Unknown backend should get default config of Registry
		RateLimiter rerateLimiter3 = rateLimiterRegistry.rateLimiter("unknownBackend");
		assertThat(rerateLimiter3).isNotNull();
		assertThat(rerateLimiter3.getRateLimiterConfig().getLimitForPeriod()).isEqualTo(3);

		assertThat(eventConsumerRegistry.getAllEventConsumer()).hasSize(2);
	}

	@Test
	public void testCreateRateLimiterRegistryWithUnknownConfig() {
		RateLimiterConfigurationProperties rateLimiterConfigurationProperties = new RateLimiterConfigurationProperties();

		RateLimiterConfigurationProperties.LimiterProperties backendProperties = new RateLimiterConfigurationProperties.LimiterProperties();
		backendProperties.setBaseConfig("unknownConfig");
		rateLimiterConfigurationProperties.getLimiters().put("backend", backendProperties);

		RateLimiterConfiguration rateLimiterConfiguration = new RateLimiterConfiguration();
		DefaultEventConsumerRegistry<RateLimiterEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

		//When
		assertThatThrownBy(() -> rateLimiterConfiguration.rateLimiterRegistry(rateLimiterConfigurationProperties, eventConsumerRegistry))
				.isInstanceOf(ConfigurationNotFoundException.class)
				.hasMessage("Configuration with name 'unknownConfig' does not exist");
	}

}