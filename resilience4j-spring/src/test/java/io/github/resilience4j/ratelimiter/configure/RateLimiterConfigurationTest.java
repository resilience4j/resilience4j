package io.github.resilience4j.ratelimiter.configure;

import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.registry.CompositeRegistryEventConsumer;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Duration;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * test custom init of rate limiter configuration
 */
@RunWith(MockitoJUnitRunner.class)
public class RateLimiterConfigurationTest {

	@Test
	public void testRateLimiterRegistry() {
		io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties.InstanceProperties instanceProperties1 = new io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties.InstanceProperties();
		instanceProperties1.setLimitForPeriod(2);
		instanceProperties1.setSubscribeForEvents(true);
		io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties.InstanceProperties instanceProperties2 = new io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties.InstanceProperties();
		instanceProperties2.setLimitForPeriod(4);
		instanceProperties2.setSubscribeForEvents(true);
		RateLimiterConfigurationProperties rateLimiterConfigurationProperties = new RateLimiterConfigurationProperties();
		rateLimiterConfigurationProperties.getInstances().put("backend1", instanceProperties1);
		rateLimiterConfigurationProperties.getInstances().put("backend2", instanceProperties2);
		rateLimiterConfigurationProperties.setRateLimiterAspectOrder(300);
		RateLimiterConfiguration rateLimiterConfiguration = new RateLimiterConfiguration();
		DefaultEventConsumerRegistry<RateLimiterEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

		RateLimiterRegistry rateLimiterRegistry = rateLimiterConfiguration.rateLimiterRegistry(rateLimiterConfigurationProperties, eventConsumerRegistry, new CompositeRegistryEventConsumer<>(emptyList()));

		assertThat(rateLimiterConfigurationProperties.getRateLimiterAspectOrder()).isEqualTo(300);
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
		io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties.InstanceProperties defaultProperties = new io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties.InstanceProperties();
		defaultProperties.setLimitForPeriod(3);
		defaultProperties.setLimitRefreshPeriod(Duration.ofNanos(5000000));
		defaultProperties.setSubscribeForEvents(true);

		io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties.InstanceProperties sharedProperties = new io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties.InstanceProperties();
		sharedProperties.setLimitForPeriod(2);
		sharedProperties.setLimitRefreshPeriod(Duration.ofNanos(6000000));
		sharedProperties.setSubscribeForEvents(true);

		io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties.InstanceProperties backendWithDefaultConfig = new io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties.InstanceProperties();
		backendWithDefaultConfig.setBaseConfig("default");
		backendWithDefaultConfig.setLimitForPeriod(200);
		backendWithDefaultConfig.setSubscribeForEvents(true);

		io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties.InstanceProperties backendWithSharedConfig = new io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties.InstanceProperties();
		backendWithSharedConfig.setBaseConfig("sharedConfig");
		backendWithSharedConfig.setLimitForPeriod(300);
		backendWithSharedConfig.setSubscribeForEvents(true);

		RateLimiterConfigurationProperties rateLimiterConfigurationProperties = new RateLimiterConfigurationProperties();
		rateLimiterConfigurationProperties.getConfigs().put("default", defaultProperties);
		rateLimiterConfigurationProperties.getConfigs().put("sharedConfig", sharedProperties);

		rateLimiterConfigurationProperties.getInstances().put("backendWithDefaultConfig", backendWithDefaultConfig);
		rateLimiterConfigurationProperties.getInstances().put("backendWithSharedConfig", backendWithSharedConfig);

		RateLimiterConfiguration rateLimiterConfiguration = new RateLimiterConfiguration();
		DefaultEventConsumerRegistry<RateLimiterEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

		//When
		RateLimiterRegistry rateLimiterRegistry = rateLimiterConfiguration.rateLimiterRegistry(rateLimiterConfigurationProperties, eventConsumerRegistry, new CompositeRegistryEventConsumer<>(emptyList()));

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
		io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties.InstanceProperties instanceProperties = new io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties.InstanceProperties();
		instanceProperties.setBaseConfig("unknownConfig");
		rateLimiterConfigurationProperties.getInstances().put("backend", instanceProperties);
		RateLimiterConfiguration rateLimiterConfiguration = new RateLimiterConfiguration();
		DefaultEventConsumerRegistry<RateLimiterEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

		assertThatThrownBy(() -> rateLimiterConfiguration.rateLimiterRegistry(rateLimiterConfigurationProperties, eventConsumerRegistry, new CompositeRegistryEventConsumer<>(emptyList())))
				.isInstanceOf(ConfigurationNotFoundException.class)
				.hasMessage("Configuration with name 'unknownConfig' does not exist");
	}

}