package io.github.resilience4j.ratelimiter.configure;

import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * test custom init of rate limiter configuration
 */
@RunWith(MockitoJUnitRunner.class)
public class RateLimiterConfigurationTest {

	@Test
	public void testRateLimiterRegistry() {
		//Given
		RateLimiterConfigurationProperties.BackendProperties backendProperties1 = new RateLimiterConfigurationProperties.BackendProperties();
		backendProperties1.setLimitForPeriod(2);
		backendProperties1.setSubscribeForEvents(true);

		RateLimiterConfigurationProperties.BackendProperties backendProperties2 = new RateLimiterConfigurationProperties.BackendProperties();
		backendProperties2.setLimitForPeriod(4);
		backendProperties2.setSubscribeForEvents(true);

		RateLimiterConfigurationProperties rateLimiterConfigurationProperties = new RateLimiterConfigurationProperties();
		rateLimiterConfigurationProperties.getBackends().put("backend1", backendProperties1);
		rateLimiterConfigurationProperties.getBackends().put("backend2", backendProperties2);

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
		RateLimiterConfigurationProperties.BackendProperties defaultProperties = new RateLimiterConfigurationProperties.BackendProperties();
		defaultProperties.setLimitForPeriod(3);
		defaultProperties.setLimitRefreshPeriodInNanos(5000000);
		defaultProperties.setSubscribeForEvents(true);

		RateLimiterConfigurationProperties.BackendProperties sharedProperties = new RateLimiterConfigurationProperties.BackendProperties();
		sharedProperties.setLimitForPeriod(2);
		sharedProperties.setLimitRefreshPeriodInNanos(6000000);
		sharedProperties.setSubscribeForEvents(true);

		RateLimiterConfigurationProperties.BackendProperties backendWithDefaultConfig = new RateLimiterConfigurationProperties.BackendProperties();
		backendWithDefaultConfig.setBaseConfig("default");
		backendWithDefaultConfig.setLimitForPeriod(200);
		backendWithDefaultConfig.setSubscribeForEvents(true);

		RateLimiterConfigurationProperties.BackendProperties backendWithSharedConfig = new RateLimiterConfigurationProperties.BackendProperties();
		backendWithSharedConfig.setBaseConfig("sharedConfig");
		backendWithSharedConfig.setLimitForPeriod(300);
		backendWithSharedConfig.setSubscribeForEvents(true);

		RateLimiterConfigurationProperties rateLimiterConfigurationProperties = new RateLimiterConfigurationProperties();
		rateLimiterConfigurationProperties.getConfigs().put("default", defaultProperties);
		rateLimiterConfigurationProperties.getConfigs().put("sharedConfig", sharedProperties);

		rateLimiterConfigurationProperties.getBackends().put("backendWithDefaultConfig", backendWithDefaultConfig);
		rateLimiterConfigurationProperties.getBackends().put("backendWithSharedConfig", backendWithSharedConfig);

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

		RateLimiterConfigurationProperties.BackendProperties backendProperties = new RateLimiterConfigurationProperties.BackendProperties();
		backendProperties.setBaseConfig("unknownConfig");
		rateLimiterConfigurationProperties.getBackends().put("backend", backendProperties);

		RateLimiterConfiguration rateLimiterConfiguration = new RateLimiterConfiguration();
		DefaultEventConsumerRegistry<RateLimiterEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

		//When
		assertThatThrownBy(() -> rateLimiterConfiguration.rateLimiterRegistry(rateLimiterConfigurationProperties, eventConsumerRegistry))
				.isInstanceOf(ConfigurationNotFoundException.class)
				.hasMessage("Configuration with name 'unknownConfig' does not exist");
	}

}