/*
 *
 * Copyright 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package io.github.resilience4j.common.bulkhead.configuration;

import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.common.CompositeCustomizer;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * unit test for thread pool bulkhead properties
 */
class ThreadPoolBulkheadConfigurationPropertiesTest  {

    @Test
    void tesFixedThreadPoolBulkHeadProperties() {
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
        assertThat(bulkhead1.getCoreThreadPoolSize()).isOne();

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
    void createThreadPoolBulkHeadPropertiesWithSharedConfigs() {
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
        assertThat(bulkheadConfigurationProperties.getBackends()).hasSize(2);
        // Should get default config and core number
        ThreadPoolBulkheadConfig bulkhead1 = bulkheadConfigurationProperties
            .createThreadPoolBulkheadConfig("backendWithDefaultConfig",
                compositeThreadPoolBulkheadCustomizer());
        assertThat(bulkhead1).isNotNull();
        assertThat(bulkhead1.getCoreThreadPoolSize()).isEqualTo(3);
        assertThat(bulkhead1.getMaxThreadPoolSize()).isEqualTo(10);
        assertThat(bulkhead1.getQueueCapacity()).isOne();
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
    void createThreadPoolBulkHeadPropertiesWithDefaultConfig() {
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
        assertThat(bulkheadConfigurationProperties.getBackends()).hasSize(2);
        // Should get default config and core number
        ThreadPoolBulkheadConfig bulkhead1 = bulkheadConfigurationProperties
            .createThreadPoolBulkheadConfig("backendWithoutBaseConfig",
                compositeThreadPoolBulkheadCustomizer());
        assertThat(bulkhead1).isNotNull();
        assertThat(bulkhead1.getCoreThreadPoolSize()).isEqualTo(3);
        assertThat(bulkhead1.getMaxThreadPoolSize()).isEqualTo(10);
        assertThat(bulkhead1.getQueueCapacity()).isOne();
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
        assertThat(bulkhead3.getCoreThreadPoolSize()).isOne();
    }

    @Test
    void threadPoolBulkheadIllegalArgumentOnEventConsumerBufferSizeLessThanOne() {
CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties defaultProperties = new CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        assertThatThrownBy(() -> defaultProperties.setEventConsumerBufferSize(0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void threadPoolBulkheadConfigWithBaseConfig() {
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
