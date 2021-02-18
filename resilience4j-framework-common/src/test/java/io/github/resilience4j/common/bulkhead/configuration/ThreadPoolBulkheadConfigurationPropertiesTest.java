package io.github.resilience4j.common.bulkhead.configuration;

import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.common.CompositeCustomizer;
import org.junit.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * unit test for thread pool bulkhead properties
 */
public class ThreadPoolBulkheadConfigurationPropertiesTest  {

    @Test
    public void tesFixedThreadPoolBulkHeadProperties() {
        //Given
        ThreadPoolBulkheadConfigurationProperties.InstanceProperties backendProperties1 = new ThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        backendProperties1.setCoreThreadPoolSize(1);

        ThreadPoolBulkheadConfigurationProperties.InstanceProperties backendProperties2 = new ThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        backendProperties2.setCoreThreadPoolSize(2);

        ThreadPoolBulkheadConfigurationProperties bulkheadConfigurationProperties = new ThreadPoolBulkheadConfigurationProperties();
        bulkheadConfigurationProperties.getBackends().put("backend1", backendProperties1);
        bulkheadConfigurationProperties.getBackends().put("backend2", backendProperties2);
        Map<String, String> tags = new HashMap<>();
        tags.put("testKey1", "testKet2");
        bulkheadConfigurationProperties.setTags(tags);

        //Then
        assertThat(bulkheadConfigurationProperties.getTags()).isNotEmpty();
        assertThat(bulkheadConfigurationProperties.getBackends().size()).isEqualTo(2);
        assertThat(bulkheadConfigurationProperties.getInstances().size()).isEqualTo(2);
        ThreadPoolBulkheadConfig bulkhead1 = bulkheadConfigurationProperties
            .createThreadPoolBulkheadConfig("backend1", compositeThreadPoolBulkheadCustomizer());
        assertThat(bulkhead1).isNotNull();
        assertThat(bulkhead1.getCoreThreadPoolSize()).isEqualTo(1);

        ThreadPoolBulkheadConfig bulkhead2 = bulkheadConfigurationProperties
            .createThreadPoolBulkheadConfig("backend2", compositeThreadPoolBulkheadCustomizer());
        assertThat(bulkhead2).isNotNull();
        assertThat(bulkhead2.getCoreThreadPoolSize()).isEqualTo(2);

    }

    @Test
    public void testCreateThreadPoolBulkHeadPropertiesWithSharedConfigs() {
        //Given
        ThreadPoolBulkheadConfigurationProperties.InstanceProperties defaultProperties = new ThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        defaultProperties.setCoreThreadPoolSize(1);
        defaultProperties.setQueueCapacity(1);
        defaultProperties.setKeepAliveDuration(Duration.ofMillis(5));
        defaultProperties.setMaxThreadPoolSize(10);

        ThreadPoolBulkheadConfigurationProperties.InstanceProperties sharedProperties = new ThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        sharedProperties.setCoreThreadPoolSize(2);
        sharedProperties.setMaxThreadPoolSize(20);
        sharedProperties.setQueueCapacity(2);

        ThreadPoolBulkheadConfigurationProperties.InstanceProperties backendWithDefaultConfig = new ThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        backendWithDefaultConfig.setBaseConfig("default");
        backendWithDefaultConfig.setCoreThreadPoolSize(3);

        ThreadPoolBulkheadConfigurationProperties.InstanceProperties backendWithSharedConfig = new ThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        backendWithSharedConfig.setBaseConfig("sharedConfig");
        backendWithSharedConfig.setCoreThreadPoolSize(4);

        ThreadPoolBulkheadConfigurationProperties bulkheadConfigurationProperties = new ThreadPoolBulkheadConfigurationProperties();
        bulkheadConfigurationProperties.getConfigs().put("default", defaultProperties);
        bulkheadConfigurationProperties.getConfigs().put("sharedConfig", sharedProperties);

        bulkheadConfigurationProperties.getBackends()
            .put("backendWithDefaultConfig", backendWithDefaultConfig);
        bulkheadConfigurationProperties.getBackends()
            .put("backendWithSharedConfig", backendWithSharedConfig);

        //When
        //Then
        assertThat(bulkheadConfigurationProperties.getBackends().size()).isEqualTo(2);
        // Should get default config and core number
        ThreadPoolBulkheadConfig bulkhead1 = bulkheadConfigurationProperties
            .createThreadPoolBulkheadConfig("backendWithDefaultConfig",
                compositeThreadPoolBulkheadCustomizer());
        assertThat(bulkhead1).isNotNull();
        assertThat(bulkhead1.getCoreThreadPoolSize()).isEqualTo(3);
        assertThat(bulkhead1.getMaxThreadPoolSize()).isEqualTo(10);
        assertThat(bulkhead1.getQueueCapacity()).isEqualTo(1);
        // Should get shared config and overwrite core number
        ThreadPoolBulkheadConfig bulkhead2 = bulkheadConfigurationProperties
            .createThreadPoolBulkheadConfig("backendWithSharedConfig",
                compositeThreadPoolBulkheadCustomizer());
        assertThat(bulkhead2).isNotNull();
        assertThat(bulkhead2.getCoreThreadPoolSize()).isEqualTo(4);
        assertThat(bulkhead2.getMaxThreadPoolSize()).isEqualTo(20);
        assertThat(bulkhead2.getQueueCapacity()).isEqualTo(2);
        // Unknown backend should get default config of Registry
        ThreadPoolBulkheadConfig bulkhead3 = bulkheadConfigurationProperties
            .createThreadPoolBulkheadConfig("unknownBackend",
                compositeThreadPoolBulkheadCustomizer());
        assertThat(bulkhead3).isNotNull();
        assertThat(bulkhead3.getCoreThreadPoolSize())
            .isEqualTo(ThreadPoolBulkheadConfig.DEFAULT_CORE_THREAD_POOL_SIZE);

    }

    @Test(expected = IllegalArgumentException.class)
    public void testThreadPoolBulkheadIllegalArgumentOnEventConsumerBufferSize() {
        ThreadPoolBulkheadConfigurationProperties.InstanceProperties defaultProperties = new ThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        defaultProperties.setEventConsumerBufferSize(-1);
    }

    @Test
    public void testThreadPoolBulkheadConfigWithBaseConfig() {
        ThreadPoolBulkheadConfigurationProperties.InstanceProperties defaultConfig = new ThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        defaultConfig.setMaxThreadPoolSize(2000);
        defaultConfig.setKeepAliveDuration(Duration.ofMillis(100L));

        ThreadPoolBulkheadConfigurationProperties.InstanceProperties sharedConfigWithDefaultConfig = new ThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        sharedConfigWithDefaultConfig.setKeepAliveDuration(Duration.ofMillis(1000L));
        sharedConfigWithDefaultConfig.setBaseConfig("defaultConfig");

        ThreadPoolBulkheadConfigurationProperties.InstanceProperties instanceWithSharedConfig = new ThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        instanceWithSharedConfig.setBaseConfig("sharedConfig");


        ThreadPoolBulkheadConfigurationProperties threadPoolBulkheadConfigurationProperties = new ThreadPoolBulkheadConfigurationProperties();
        threadPoolBulkheadConfigurationProperties.getConfigs().put("defaultConfig", defaultConfig);
        threadPoolBulkheadConfigurationProperties.getConfigs().put("sharedConfig", sharedConfigWithDefaultConfig);
        threadPoolBulkheadConfigurationProperties.getInstances().put("instanceWithSharedConfig", instanceWithSharedConfig);


        ThreadPoolBulkheadConfig instance = threadPoolBulkheadConfigurationProperties
            .createThreadPoolBulkheadConfig(instanceWithSharedConfig, compositeThreadPoolBulkheadCustomizer(), "instanceWithSharedConfig");
        assertThat(instance).isNotNull();
        assertThat(instance.getMaxThreadPoolSize()).isEqualTo(2000);
        assertThat(instance.getKeepAliveDuration()).isEqualTo(Duration.ofMillis(1000L));
    }


    private CompositeCustomizer<ThreadPoolBulkheadConfigCustomizer> compositeThreadPoolBulkheadCustomizer() {
        return new CompositeCustomizer<>(Collections.emptyList());
    }

}
