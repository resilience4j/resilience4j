package io.github.resilience4j.retry.configure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.event.RetryEvent;

/**
 * test custom init of retry configuration
 */
@RunWith(MockitoJUnitRunner.class)
public class RetryConfigurationTest {

	@Test
	public void testRetryRegistry() {
		//Given
		RetryConfigurationProperties.BackendProperties backendProperties1 = new RetryConfigurationProperties.BackendProperties();
		backendProperties1.setMaxRetryAttempts(3);

		RetryConfigurationProperties.BackendProperties backendProperties2 = new RetryConfigurationProperties.BackendProperties();
		backendProperties2.setMaxRetryAttempts(2);

		RetryConfigurationProperties retryConfigurationProperties = new RetryConfigurationProperties();
		retryConfigurationProperties.getBackends().put("backend1", backendProperties1);
		retryConfigurationProperties.getBackends().put("backend2", backendProperties2);

		RetryConfiguration retryConfiguration = new RetryConfiguration();
		DefaultEventConsumerRegistry<RetryEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

		//When
		RetryRegistry retryRegistry = retryConfiguration.retryRegistry(retryConfigurationProperties, eventConsumerRegistry);

		//Then
		assertThat(retryRegistry.getAllRetries().size()).isEqualTo(2);
		Retry retry1 = retryRegistry.retry("backend1");
		assertThat(retry1).isNotNull();
		assertThat(retry1.getRetryConfig().getMaxAttempts()).isEqualTo(3);

		Retry retry2 = retryRegistry.retry("backend2");
		assertThat(retry2).isNotNull();
		assertThat(retry2.getRetryConfig().getMaxAttempts()).isEqualTo(2);

		assertThat(eventConsumerRegistry.getAllEventConsumer()).hasSize(2);
	}

	@Test
	public void testCreateRetryRegistryWithSharedConfigs() {
		//Given
		RetryConfigurationProperties.BackendProperties defaultProperties = new RetryConfigurationProperties.BackendProperties();
		defaultProperties.setMaxRetryAttempts(3);
		defaultProperties.setWaitDuration(50L);

		RetryConfigurationProperties.BackendProperties sharedProperties = new RetryConfigurationProperties.BackendProperties();
		sharedProperties.setMaxRetryAttempts(2);
		sharedProperties.setWaitDuration(100L);

		RetryConfigurationProperties.BackendProperties backendWithDefaultConfig = new RetryConfigurationProperties.BackendProperties();
		backendWithDefaultConfig.setBaseConfig("default");
		backendWithDefaultConfig.setWaitDuration(200L);

		RetryConfigurationProperties.BackendProperties backendWithSharedConfig = new RetryConfigurationProperties.BackendProperties();
		backendWithSharedConfig.setBaseConfig("sharedConfig");
		backendWithSharedConfig.setWaitDuration(300L);

		RetryConfigurationProperties retryConfigurationProperties = new RetryConfigurationProperties();
		retryConfigurationProperties.getConfigs().put("default", defaultProperties);
		retryConfigurationProperties.getConfigs().put("sharedConfig", sharedProperties);

		retryConfigurationProperties.getBackends().put("backendWithDefaultConfig", backendWithDefaultConfig);
		retryConfigurationProperties.getBackends().put("backendWithSharedConfig", backendWithSharedConfig);

		RetryConfiguration retryConfiguration = new RetryConfiguration();
		DefaultEventConsumerRegistry<RetryEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

		//When
		RetryRegistry retryRegistry = retryConfiguration.retryRegistry(retryConfigurationProperties, eventConsumerRegistry);

		//Then
		assertThat(retryRegistry.getAllRetries().size()).isEqualTo(2);

		// Should get default config and overwrite max attempt and wait time
		Retry retry1 = retryRegistry.retry("backendWithDefaultConfig");
		assertThat(retry1).isNotNull();
		assertThat(retry1.getRetryConfig().getMaxAttempts()).isEqualTo(3);
		assertThat(retry1.getRetryConfig().getIntervalFunction().apply(1)).isEqualTo(200L);

		// Should get shared config and overwrite wait time
		Retry retry2 = retryRegistry.retry("backendWithSharedConfig");
		assertThat(retry2).isNotNull();
		assertThat(retry2.getRetryConfig().getMaxAttempts()).isEqualTo(2);
		assertThat(retry2.getRetryConfig().getIntervalFunction().apply(1)).isEqualTo(300L);

		// Unknown backend should get default config of Registry
		Retry retry3 = retryRegistry.retry("unknownBackend");
		assertThat(retry3).isNotNull();
		assertThat(retry3.getRetryConfig().getMaxAttempts()).isEqualTo(3);

		assertThat(eventConsumerRegistry.getAllEventConsumer()).hasSize(3);
	}

	@Test
	public void testCreateRetryRegistryWithUnknownConfig() {
		RetryConfigurationProperties retryConfigurationProperties = new RetryConfigurationProperties();

		RetryConfigurationProperties.BackendProperties backendProperties = new RetryConfigurationProperties.BackendProperties();
		backendProperties.setBaseConfig("unknownConfig");
		retryConfigurationProperties.getBackends().put("backend", backendProperties);

		RetryConfiguration retryConfiguration = new RetryConfiguration();
		DefaultEventConsumerRegistry<RetryEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

		//When
		assertThatThrownBy(() -> retryConfiguration.retryRegistry(retryConfigurationProperties, eventConsumerRegistry))
				.isInstanceOf(ConfigurationNotFoundException.class)
				.hasMessage("Configuration with name 'unknownConfig' does not exist");
	}

}