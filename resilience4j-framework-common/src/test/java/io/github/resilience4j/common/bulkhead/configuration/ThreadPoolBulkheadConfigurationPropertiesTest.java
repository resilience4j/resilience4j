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
        CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties backendProperties1 = new CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        backendProperties1.setCoreThreadPoolSize(1);

        CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties backendProperties2 = new CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        backendProperties2.setCoreThreadPoolSize(2);

        CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties backendProperties3 = new CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        backendProperties3.setQueueCapacity(0);

        CommonThreadPoolBulkheadConfigurationProperties bulkheadConfigurationProperties = new CommonThreadPoolBulkheadConfigurationProperties();
        bulkheadConfigurationProperties.getBackends().put("backend1", backendProperties1);
        bulkheadConfigurationProperties.getBackends().put("backend2", backendProperties2);
        bulkheadConfigurationProperties.getBackends().put("backend3", backendProperties3);
        Map<String, String> tags = new HashMap<>();
        tags.put("testKey1", "testKet2");
        bulkheadConfigurationProperties.setTags(tags);

        //Then
        assertThat(bulkheadConfigurationProperties.getTags()).isNotEmpty();
        assertThat(bulkheadConfigurationProperties.getBackends()).hasSize(3);
        assertThat(bulkheadConfigurationProperties.getInstances()).hasSize(3);
        ThreadPoolBulkheadConfig bulkhead1 = bulkheadConfigurationProperties
            .createThreadPoolBulkheadConfig("backend1", compositeThreadPoolBulkheadCustomizer());
        assertThat(bulkhead1).isNotNull();
        assertThat(bulkhead1.getCoreThreadPoolSize()).isEqualTo(1);

        ThreadPoolBulkheadConfig bulkhead2 = bulkheadConfigurationProperties
            .createThreadPoolBulkheadConfig("backend2", compositeThreadPoolBulkheadCustomizer());
        assertThat(bulkhead2).isNotNull();
        assertThat(bulkhead2.getCoreThreadPoolSize()).isEqualTo(2);

        ThreadPoolBulkheadConfig bulkhead3 = bulkheadConfigurationProperties
            .createThreadPoolBulkheadConfig("backend3", compositeThreadPoolBulkheadCustomizer());
        assertThat(bulkhead3).isNotNull();
        assertThat(bulkhead3.getQueueCapacity()).isZero();

    }

    @Test
    public void testCreateThreadPoolBulkHeadPropertiesWithSharedConfigs() {
        //Given
        CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties defaultProperties = new CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        defaultProperties.setCoreThreadPoolSize(1);
        defaultProperties.setQueueCapacity(1);
        defaultProperties.setKeepAliveDuration(Duration.ofMillis(5));
        defaultProperties.setMaxThreadPoolSize(10);

        CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties sharedProperties = new CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        sharedProperties.setCoreThreadPoolSize(2);
        sharedProperties.setMaxThreadPoolSize(20);
        sharedProperties.setQueueCapacity(2);

        CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties backendWithDefaultConfig = new CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        backendWithDefaultConfig.setBaseConfig("defaultConfig");
        backendWithDefaultConfig.setCoreThreadPoolSize(3);

        CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties backendWithSharedConfig = new CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        backendWithSharedConfig.setBaseConfig("sharedConfig");
        backendWithSharedConfig.setCoreThreadPoolSize(4);

        CommonThreadPoolBulkheadConfigurationProperties bulkheadConfigurationProperties = new CommonThreadPoolBulkheadConfigurationProperties();
        bulkheadConfigurationProperties.getConfigs().put("defaultConfig", defaultProperties);
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

    @Test
    public void testCreateThreadPoolBulkHeadPropertiesWithDefaultConfig() {
        //Given
        CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties defaultProperties = new CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        defaultProperties.setCoreThreadPoolSize(1);
        defaultProperties.setQueueCapacity(1);
        defaultProperties.setKeepAliveDuration(Duration.ofMillis(5));
        defaultProperties.setMaxThreadPoolSize(10);

        CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties sharedProperties = new CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        sharedProperties.setCoreThreadPoolSize(2);
        sharedProperties.setMaxThreadPoolSize(20);
        sharedProperties.setQueueCapacity(2);

        CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties backendWithoutBaseConfig = new CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        backendWithoutBaseConfig.setCoreThreadPoolSize(3);

        CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties backendWithSharedConfig = new CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        backendWithSharedConfig.setBaseConfig("sharedConfig");
        backendWithSharedConfig.setCoreThreadPoolSize(4);

        CommonThreadPoolBulkheadConfigurationProperties bulkheadConfigurationProperties = new CommonThreadPoolBulkheadConfigurationProperties();
        bulkheadConfigurationProperties.getConfigs().put("default", defaultProperties);
        bulkheadConfigurationProperties.getConfigs().put("sharedConfig", sharedProperties);

        bulkheadConfigurationProperties.getBackends()
            .put("backendWithoutBaseConfig", backendWithoutBaseConfig);
        bulkheadConfigurationProperties.getBackends()
            .put("backendWithSharedConfig", backendWithSharedConfig);

        //When
        //Then
        assertThat(bulkheadConfigurationProperties.getBackends().size()).isEqualTo(2);
        // Should get default config and core number
        ThreadPoolBulkheadConfig bulkhead1 = bulkheadConfigurationProperties
            .createThreadPoolBulkheadConfig("backendWithoutBaseConfig",
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
        assertThat(bulkhead3.getCoreThreadPoolSize()).isEqualTo(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThreadPoolBulkheadIllegalArgumentOnEventConsumerBufferSizeLessThanOne() {
        CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties defaultProperties = new CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        defaultProperties.setEventConsumerBufferSize(0);
    }

    @Test
    public void testThreadPoolBulkheadConfigWithBaseConfig() {
        CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties defaultConfig = new CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        defaultConfig.setMaxThreadPoolSize(2000);
        defaultConfig.setKeepAliveDuration(Duration.ofMillis(100L));

        CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties sharedConfigWithDefaultConfig = new CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        sharedConfigWithDefaultConfig.setKeepAliveDuration(Duration.ofMillis(1000L));
        sharedConfigWithDefaultConfig.setBaseConfig("defaultConfig");

        CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties instanceWithSharedConfig = new CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        instanceWithSharedConfig.setBaseConfig("sharedConfig");


        CommonThreadPoolBulkheadConfigurationProperties threadPoolBulkheadConfigurationProperties = new CommonThreadPoolBulkheadConfigurationProperties();
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
