package io.github.resilience4j.bulkhead.configure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.core.ConfigurationNotFoundException;

/**
 * test custom init of bulkhead configuration
 */
@RunWith(MockitoJUnitRunner.class)
public class BulkHeadConfigurationTest {

	@Test
	public void testBulkHeadRegistry() {
		//Given
		BulkheadConfigurationProperties.BackendProperties backendProperties1 = new BulkheadConfigurationProperties.BackendProperties();
		backendProperties1.setMaxConcurrentCall(3);

		BulkheadConfigurationProperties.BackendProperties backendProperties2 = new BulkheadConfigurationProperties.BackendProperties();
		backendProperties2.setMaxConcurrentCall(2);

		BulkheadConfigurationProperties bulkheadConfigurationProperties = new BulkheadConfigurationProperties();
		bulkheadConfigurationProperties.getBackends().put("backend1", backendProperties1);
		bulkheadConfigurationProperties.getBackends().put("backend2", backendProperties2);

		BulkheadConfiguration bulkheadConfiguration = new BulkheadConfiguration();
		DefaultEventConsumerRegistry<BulkheadEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

		//When
		BulkheadRegistry bulkheadRegistry = bulkheadConfiguration.bulkheadRegistry(bulkheadConfigurationProperties, eventConsumerRegistry);

		//Then
		assertThat(bulkheadRegistry.getAllBulkheads().size()).isEqualTo(2);
		Bulkhead bulkhead1 = bulkheadRegistry.bulkhead("backend1");
		assertThat(bulkhead1).isNotNull();
		assertThat(bulkhead1.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(3);

		Bulkhead bulkhead2 = bulkheadRegistry.bulkhead("backend2");
		assertThat(bulkhead2).isNotNull();
		assertThat(bulkhead2.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(2);

		assertThat(eventConsumerRegistry.getAllEventConsumer()).hasSize(2);
	}

	@Test
	public void testCreateBulkHeadRegistryWithSharedConfigs() {
		//Given
		BulkheadConfigurationProperties.BackendProperties defaultProperties = new BulkheadConfigurationProperties.BackendProperties();
		defaultProperties.setMaxConcurrentCall(3);
		defaultProperties.setMaxWaitTime(50L);

		BulkheadConfigurationProperties.BackendProperties sharedProperties = new BulkheadConfigurationProperties.BackendProperties();
		sharedProperties.setMaxConcurrentCall(2);
		sharedProperties.setMaxWaitTime(100L);

		BulkheadConfigurationProperties.BackendProperties backendWithDefaultConfig = new BulkheadConfigurationProperties.BackendProperties();
		backendWithDefaultConfig.setBaseConfig("default");
		backendWithDefaultConfig.setMaxWaitTime(200L);

		BulkheadConfigurationProperties.BackendProperties backendWithSharedConfig = new BulkheadConfigurationProperties.BackendProperties();
		backendWithSharedConfig.setBaseConfig("sharedConfig");
		backendWithSharedConfig.setMaxWaitTime(300L);

		BulkheadConfigurationProperties bulkheadConfigurationProperties = new BulkheadConfigurationProperties();
		bulkheadConfigurationProperties.getConfigs().put("default", defaultProperties);
		bulkheadConfigurationProperties.getConfigs().put("sharedConfig", sharedProperties);

		bulkheadConfigurationProperties.getBackends().put("backendWithDefaultConfig", backendWithDefaultConfig);
		bulkheadConfigurationProperties.getBackends().put("backendWithSharedConfig", backendWithSharedConfig);

		BulkheadConfiguration bulkheadConfiguration = new BulkheadConfiguration();
		DefaultEventConsumerRegistry<BulkheadEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

		//When
		BulkheadRegistry bulkheadRegistry = bulkheadConfiguration.bulkheadRegistry(bulkheadConfigurationProperties, eventConsumerRegistry);

		//Then
		assertThat(bulkheadRegistry.getAllBulkheads().size()).isEqualTo(2);

		// Should get default config and overwrite max calls and wait time
		Bulkhead bulkhead1 = bulkheadRegistry.bulkhead("backendWithDefaultConfig");
		assertThat(bulkhead1).isNotNull();
		assertThat(bulkhead1.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(3);
		assertThat(bulkhead1.getBulkheadConfig().getMaxWaitTime()).isEqualTo(200L);

		// Should get shared config and overwrite wait time
		Bulkhead bulkhead2 = bulkheadRegistry.bulkhead("backendWithSharedConfig");
		assertThat(bulkhead2).isNotNull();
		assertThat(bulkhead2.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(2);
		assertThat(bulkhead2.getBulkheadConfig().getMaxWaitTime()).isEqualTo(300L);

		// Unknown backend should get default config of Registry
		Bulkhead bulkhead3 = bulkheadRegistry.bulkhead("unknownBackend");
		assertThat(bulkhead3).isNotNull();
		assertThat(bulkhead3.getBulkheadConfig().getMaxWaitTime()).isEqualTo(50L);

		assertThat(eventConsumerRegistry.getAllEventConsumer()).hasSize(3);
	}

	@Test
	public void testCreateBulkHeadRegistryWithUnknownConfig() {
		BulkheadConfigurationProperties bulkheadConfigurationProperties = new BulkheadConfigurationProperties();

		BulkheadConfigurationProperties.BackendProperties backendProperties = new BulkheadConfigurationProperties.BackendProperties();
		backendProperties.setBaseConfig("unknownConfig");
		bulkheadConfigurationProperties.getBackends().put("backend", backendProperties);

		BulkheadConfiguration bulkheadConfiguration = new BulkheadConfiguration();
		DefaultEventConsumerRegistry<BulkheadEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

		//When
		assertThatThrownBy(() -> bulkheadConfiguration.bulkheadRegistry(bulkheadConfigurationProperties, eventConsumerRegistry))
				.isInstanceOf(ConfigurationNotFoundException.class)
				.hasMessage("Configuration with name 'unknownConfig' does not exist");
	}

}