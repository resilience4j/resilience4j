package io.github.resilience4j.retry.configure;

import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.retry.configuration.RetryConfigCustomizer;
import io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties.InstanceProperties;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.registry.CompositeRegistryEventConsumer;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.event.RetryEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Duration;
import java.util.Collections;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * test custom init of retry configuration
 */
@RunWith(MockitoJUnitRunner.class)
public class RetryConfigurationTest {

    @Test
    public void testRetryRegistry() {
        InstanceProperties instanceProperties1 = new InstanceProperties();
        instanceProperties1.setMaxAttempts(3);
        InstanceProperties instanceProperties2 = new InstanceProperties();
        instanceProperties2.setMaxAttempts(2);
        RetryConfigurationProperties retryConfigurationProperties = new RetryConfigurationProperties();
        retryConfigurationProperties.getInstances().put("backend1", instanceProperties1);
        retryConfigurationProperties.getInstances().put("backend2", instanceProperties2);
        retryConfigurationProperties.setRetryAspectOrder(200);
        RetryConfiguration retryConfiguration = new RetryConfiguration();
        DefaultEventConsumerRegistry<RetryEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

        RetryRegistry retryRegistry = retryConfiguration
            .retryRegistry(retryConfigurationProperties, eventConsumerRegistry,
                new CompositeRegistryEventConsumer<>(emptyList()), compositeRetryCustomizerTest());

        assertThat(retryConfigurationProperties.getRetryAspectOrder()).isEqualTo(200);
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
        InstanceProperties defaultProperties = new InstanceProperties();
        defaultProperties.setMaxAttempts(3);
        defaultProperties.setWaitDuration(Duration.ofMillis(100L));
        InstanceProperties sharedProperties = new InstanceProperties();
        sharedProperties.setMaxAttempts(2);
        sharedProperties.setWaitDuration(Duration.ofMillis(100L));
        InstanceProperties backendWithDefaultConfig = new InstanceProperties();
        backendWithDefaultConfig.setBaseConfig("default");
        backendWithDefaultConfig.setWaitDuration(Duration.ofMillis(200L));
        InstanceProperties backendWithSharedConfig = new InstanceProperties();
        backendWithSharedConfig.setBaseConfig("sharedConfig");
        backendWithSharedConfig.setWaitDuration(Duration.ofMillis(300L));
        RetryConfigurationProperties retryConfigurationProperties = new RetryConfigurationProperties();
        retryConfigurationProperties.getConfigs().put("default", defaultProperties);
        retryConfigurationProperties.getConfigs().put("sharedConfig", sharedProperties);
        retryConfigurationProperties.getInstances()
            .put("backendWithDefaultConfig", backendWithDefaultConfig);
        retryConfigurationProperties.getInstances()
            .put("backendWithSharedConfig", backendWithSharedConfig);
        RetryConfiguration retryConfiguration = new RetryConfiguration();
        DefaultEventConsumerRegistry<RetryEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

        RetryRegistry retryRegistry = retryConfiguration
            .retryRegistry(retryConfigurationProperties, eventConsumerRegistry,
                new CompositeRegistryEventConsumer<>(emptyList()), compositeRetryCustomizerTest());

        assertThat(retryRegistry.getAllRetries().size()).isEqualTo(2);
        // Should get default config and overwrite max attempt and wait time
        Retry retry1 = retryRegistry.retry("backendWithDefaultConfig");
        assertThat(retry1).isNotNull();
        assertThat(retry1.getRetryConfig().getMaxAttempts()).isEqualTo(3);
        assertThat(retry1.getRetryConfig().getIntervalBiFunction().apply(1, null)).isEqualTo(200L);
        // Should get shared config and overwrite wait time
        Retry retry2 = retryRegistry.retry("backendWithSharedConfig");
        assertThat(retry2).isNotNull();
        assertThat(retry2.getRetryConfig().getMaxAttempts()).isEqualTo(2);
        assertThat(retry2.getRetryConfig().getIntervalBiFunction().apply(1, null)).isEqualTo(300L);
        // Unknown backend should get default config of Registry
        Retry retry3 = retryRegistry.retry("unknownBackend");
        assertThat(retry3).isNotNull();
        assertThat(retry3.getRetryConfig().getMaxAttempts()).isEqualTo(3);
        assertThat(eventConsumerRegistry.getAllEventConsumer()).hasSize(3);
    }

    @Test
    public void testCreateRetryRegistryWithUnknownConfig() {
        RetryConfigurationProperties retryConfigurationProperties = new RetryConfigurationProperties();
        InstanceProperties instanceProperties = new InstanceProperties();
        instanceProperties.setBaseConfig("unknownConfig");
        retryConfigurationProperties.getInstances().put("backend", instanceProperties);
        RetryConfiguration retryConfiguration = new RetryConfiguration();
        DefaultEventConsumerRegistry<RetryEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

        assertThatThrownBy(() -> retryConfiguration
            .retryRegistry(retryConfigurationProperties, eventConsumerRegistry,
                new CompositeRegistryEventConsumer<>(emptyList()), compositeRetryCustomizerTest()))
            .isInstanceOf(ConfigurationNotFoundException.class)
            .hasMessage("Configuration with name 'unknownConfig' does not exist");
    }

    private CompositeCustomizer<RetryConfigCustomizer> compositeRetryCustomizerTest() {
        return new CompositeCustomizer<>(Collections.emptyList());
    }

}