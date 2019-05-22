package io.github.resilience4j.bulkhead.configure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.bulkhead.configure.threadpool.ThreadPoolBulkheadConfiguration;
import io.github.resilience4j.bulkhead.configure.threadpool.ThreadPoolProperties;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.core.ConfigurationNotFoundException;

/**
 * test custom init of bulkhead configuration
 */
@RunWith(MockitoJUnitRunner.class)
public class BulkHeadConfigurationTest {

	@Test
	public void tesFixedThreadPoolBulkHeadRegistry() {
		//Given
		BulkheadConfigurationProperties.BackendProperties backendProperties1 = new BulkheadConfigurationProperties.BackendProperties();
		ThreadPoolProperties threadPoolProperties = new ThreadPoolProperties();
		threadPoolProperties.setCoreThreadPoolSize(1);
		backendProperties1.setThreadPoolProperties(threadPoolProperties);

		BulkheadConfigurationProperties.BackendProperties backendProperties2 = new BulkheadConfigurationProperties.BackendProperties();
		ThreadPoolProperties threadPoolProperties2 = new ThreadPoolProperties();
		threadPoolProperties2.setCoreThreadPoolSize(2);
		backendProperties2.setThreadPoolProperties(threadPoolProperties2);

		BulkheadConfigurationProperties bulkheadConfigurationProperties = new BulkheadConfigurationProperties();
		bulkheadConfigurationProperties.getBackends().put("backend1", backendProperties1);
		bulkheadConfigurationProperties.getBackends().put("backend2", backendProperties2);

		ThreadPoolBulkheadConfiguration threadPoolBulkheadConfiguration = new ThreadPoolBulkheadConfiguration();
		DefaultEventConsumerRegistry<BulkheadEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

		//When
		ThreadPoolBulkheadRegistry bulkheadRegistry = threadPoolBulkheadConfiguration.threadPoolBulkheadRegistry(bulkheadConfigurationProperties, eventConsumerRegistry);

		//Then
		assertThat(bulkheadRegistry.getAllBulkheads().size()).isEqualTo(2);
		ThreadPoolBulkhead bulkhead1 = bulkheadRegistry.bulkhead("backend1");
		assertThat(bulkhead1).isNotNull();
		assertThat(bulkhead1.getBulkheadConfig().getCoreThreadPoolSize()).isEqualTo(1);

		ThreadPoolBulkhead bulkhead2 = bulkheadRegistry.bulkhead("backend2");
		assertThat(bulkhead2).isNotNull();
		assertThat(bulkhead2.getBulkheadConfig().getCoreThreadPoolSize()).isEqualTo(2);

		assertThat(eventConsumerRegistry.getAllEventConsumer()).hasSize(2);
	}

	@Test
	public void testCreateThreadPoolBulkHeadRegistryWithSharedConfigs() {
		//Given
		BulkheadConfigurationProperties.BackendProperties defaultProperties = new BulkheadConfigurationProperties.BackendProperties();
		ThreadPoolProperties threadPoolProperties = new ThreadPoolProperties();
		threadPoolProperties.setCoreThreadPoolSize(1);
		threadPoolProperties.setQueueCapacity(1);
		threadPoolProperties.setKeepAliveTime(5);
		threadPoolProperties.setMaxThreadPoolSize(10);
		defaultProperties.setThreadPoolProperties(threadPoolProperties);

		BulkheadConfigurationProperties.BackendProperties sharedProperties = new BulkheadConfigurationProperties.BackendProperties();
		ThreadPoolProperties threadPoolProperties2 = new ThreadPoolProperties();
		threadPoolProperties2.setCoreThreadPoolSize(2);
		threadPoolProperties2.setQueueCapacity(2);
		sharedProperties.setThreadPoolProperties(threadPoolProperties2);

		BulkheadConfigurationProperties.BackendProperties backendWithDefaultConfig = new BulkheadConfigurationProperties.BackendProperties();
		backendWithDefaultConfig.setBaseConfig("default");
		ThreadPoolProperties threadPoolProperties3 = new ThreadPoolProperties();
		threadPoolProperties3.setCoreThreadPoolSize(3);
		backendWithDefaultConfig.setThreadPoolProperties(threadPoolProperties3);

		BulkheadConfigurationProperties.BackendProperties backendWithSharedConfig = new BulkheadConfigurationProperties.BackendProperties();
		backendWithSharedConfig.setBaseConfig("sharedConfig");
		ThreadPoolProperties threadPoolProperties4 = new ThreadPoolProperties();
		threadPoolProperties4.setCoreThreadPoolSize(4);
		backendWithSharedConfig.setThreadPoolProperties(threadPoolProperties4);

		BulkheadConfigurationProperties bulkheadConfigurationProperties = new BulkheadConfigurationProperties();
		bulkheadConfigurationProperties.getConfigs().put("default", defaultProperties);
		bulkheadConfigurationProperties.getConfigs().put("sharedConfig", sharedProperties);

		bulkheadConfigurationProperties.getBackends().put("backendWithDefaultConfig", backendWithDefaultConfig);
		bulkheadConfigurationProperties.getBackends().put("backendWithSharedConfig", backendWithSharedConfig);

		ThreadPoolBulkheadConfiguration threadPoolBulkheadConfiguration = new ThreadPoolBulkheadConfiguration();
		DefaultEventConsumerRegistry<BulkheadEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

		//When
		ThreadPoolBulkheadRegistry bulkheadRegistry = threadPoolBulkheadConfiguration.threadPoolBulkheadRegistry(bulkheadConfigurationProperties, eventConsumerRegistry);

		//Then
		assertThat(bulkheadRegistry.getAllBulkheads().size()).isEqualTo(2);

		// Should get default config and core number
		ThreadPoolBulkhead bulkhead1 = bulkheadRegistry.bulkhead("backendWithDefaultConfig");
		assertThat(bulkhead1).isNotNull();
		assertThat(bulkhead1.getBulkheadConfig().getCoreThreadPoolSize()).isEqualTo(3);
		assertThat(bulkhead1.getBulkheadConfig().getQueueCapacity()).isEqualTo(1);

		// Should get shared config and overwrite core number
		ThreadPoolBulkhead bulkhead2 = bulkheadRegistry.bulkhead("backendWithSharedConfig");
		assertThat(bulkhead2).isNotNull();
		assertThat(bulkhead2.getBulkheadConfig().getCoreThreadPoolSize()).isEqualTo(4);
		assertThat(bulkhead2.getBulkheadConfig().getQueueCapacity()).isEqualTo(2);

		// Unknown backend should get default config of Registry
		ThreadPoolBulkhead bulkhead3 = bulkheadRegistry.bulkhead("unknownBackend");
		assertThat(bulkhead3).isNotNull();
		assertThat(bulkhead3.getBulkheadConfig().getCoreThreadPoolSize()).isEqualTo(1);

		assertThat(eventConsumerRegistry.getAllEventConsumer()).hasSize(3);
	}


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