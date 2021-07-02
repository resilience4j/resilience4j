/*
 * Copyright 2019 Mahmoud Romeh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.common.retry.configuration;

import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.RecordFailurePredicate;
import io.github.resilience4j.common.TestIntervalBiFunction;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(MockitoJUnitRunner.class)
public class RetryConfigurationPropertiesTest {

    @Test
    public void testRetryProperties() {
        RetryConfigurationProperties.InstanceProperties instanceProperties1 = new RetryConfigurationProperties.InstanceProperties();
        instanceProperties1.setMaxAttempts(3);
        instanceProperties1.setWaitDuration(Duration.ofMillis(1000));
        instanceProperties1.setEnableExponentialBackoff(false);
        instanceProperties1.setEnableRandomizedWait(false);
        instanceProperties1.setEventConsumerBufferSize(100);
        instanceProperties1.setRetryExceptions(new Class[]{IllegalStateException.class});
        instanceProperties1.setIgnoreExceptions(new Class[]{IllegalArgumentException.class});
        instanceProperties1.setRetryExceptionPredicate(RecordFailurePredicate.class);
        instanceProperties1.setFailAfterMaxAttempts(true);

        RetryConfigurationProperties.InstanceProperties instanceProperties2 = new RetryConfigurationProperties.InstanceProperties();
        instanceProperties2.setMaxAttempts(2);
        instanceProperties2.setEnableExponentialBackoff(true);
        instanceProperties2.setExponentialBackoffMultiplier(1.0);
        instanceProperties2.setWaitDuration(Duration.ofMillis(100L));
        instanceProperties2.setExponentialMaxWaitDuration(Duration.ofMillis(99L));

        RetryConfigurationProperties retryConfigurationProperties = new RetryConfigurationProperties();
        retryConfigurationProperties.getInstances().put("backend1", instanceProperties1);
        retryConfigurationProperties.getInstances().put("backend2", instanceProperties2);
        Map<String, String> globalTagsForRetries = new HashMap<>();
        globalTagsForRetries.put("testKey1","testKet2");
        retryConfigurationProperties.setTags(globalTagsForRetries);

        assertThat(retryConfigurationProperties.getTags()).hasSize(1);
        assertThat(retryConfigurationProperties.getInstances()).hasSize(2);
        assertThat(retryConfigurationProperties.getBackends()).hasSize(2);
        RetryConfig retry1 = retryConfigurationProperties
            .createRetryConfig("backend1", compositeRetryCustomizer());
        RetryConfig retry2 = retryConfigurationProperties
            .createRetryConfig("backend2", compositeRetryCustomizer());
        RetryConfigurationProperties.InstanceProperties instancePropertiesForRetry1 = retryConfigurationProperties
            .getInstances().get("backend1");
        assertThat(instancePropertiesForRetry1.getWaitDuration()).isEqualTo(Duration.ofMillis(1000));
        assertThat(retry1).isNotNull();
        assertThat(retry1.isFailAfterMaxAttempts()).isEqualTo(true);
        assertThat(retry1.getMaxAttempts()).isEqualTo(3);
        assertThat(retry2).isNotNull();
        assertThat(retry2.getMaxAttempts()).isEqualTo(2);
        assertThat(retry2.getIntervalFunction().apply(1)).isEqualTo(99L) ;
        assertThat(retry2.getIntervalFunction().apply(2)).isEqualTo(99L);
        assertThat(retry2.isFailAfterMaxAttempts()).isEqualTo(false);
    }

    @Test
    public void testExponentialRandomBackoffConfig() {
        RetryConfigurationProperties.InstanceProperties instanceProperties1 = new RetryConfigurationProperties.InstanceProperties();
        instanceProperties1.setMaxAttempts(3);
        instanceProperties1.setMaxAttempts(3);
        instanceProperties1.setWaitDuration(Duration.ofMillis(1000));
        instanceProperties1.setEnableExponentialBackoff(true);
        instanceProperties1.setEnableRandomizedWait(true);
        instanceProperties1.setRandomizedWaitFactor(0.5D);
        instanceProperties1.setExponentialBackoffMultiplier(2.0D);
        instanceProperties1.setExponentialMaxWaitDuration(Duration.ofMillis(3000L));
        RetryConfigurationProperties retryConfigurationProperties = new RetryConfigurationProperties();
        retryConfigurationProperties.getInstances().put("backend1", instanceProperties1);

        RetryConfig retry1 = retryConfigurationProperties.createRetryConfig("backend1", compositeRetryCustomizer());

        assertThat(retry1).isNotNull();
        assertThat(retry1.getIntervalBiFunction().apply(1, null)).isBetween(500L, 1500L);
        assertThat(retry1.getIntervalBiFunction().apply(2, null)).isBetween(1000L, 3000L);
        assertThat(retry1.getIntervalBiFunction().apply(3, null)).isBetween(2000L, 3000L);
    }

    @Test
    public void testCreateRetryPropertiesWithSharedConfigs() {
        RetryConfigurationProperties.InstanceProperties defaultProperties = new RetryConfigurationProperties.InstanceProperties();
        defaultProperties.setMaxAttempts(3);
        defaultProperties.setMaxAttempts(3);
        defaultProperties.setWaitDuration(Duration.ofMillis(100L));

        RetryConfigurationProperties.InstanceProperties sharedProperties = new RetryConfigurationProperties.InstanceProperties();
        sharedProperties.setMaxAttempts(2);
        sharedProperties.setMaxAttempts(2);
        sharedProperties.setWaitDuration(Duration.ofMillis(100L));

        RetryConfigurationProperties.InstanceProperties backendWithDefaultConfig = new RetryConfigurationProperties.InstanceProperties();
        backendWithDefaultConfig.setBaseConfig("default");
        backendWithDefaultConfig.setWaitDuration(Duration.ofMillis(200L));

        RetryConfigurationProperties.InstanceProperties backendWithSharedConfig = new RetryConfigurationProperties.InstanceProperties();
        backendWithSharedConfig.setBaseConfig("sharedConfig");
        backendWithSharedConfig.setWaitDuration(Duration.ofMillis(300L));

        RetryConfigurationProperties retryConfigurationProperties = new RetryConfigurationProperties();
        retryConfigurationProperties.getConfigs().put("default", defaultProperties);
        retryConfigurationProperties.getConfigs().put("sharedConfig", sharedProperties);

        retryConfigurationProperties.getInstances().put("backendWithDefaultConfig", backendWithDefaultConfig);
        retryConfigurationProperties.getInstances().put("backendWithSharedConfig", backendWithSharedConfig);

        // Should get default config and overwrite max attempt and wait time
        RetryConfig retry1 = retryConfigurationProperties
            .createRetryConfig("backendWithDefaultConfig", compositeRetryCustomizer());
        assertThat(retry1).isNotNull();
        assertThat(retry1.getMaxAttempts()).isEqualTo(3);
        assertThat(retry1.getIntervalBiFunction().apply(1, null)).isEqualTo(200L);

        // Should get shared config and overwrite wait time
        RetryConfig retry2 = retryConfigurationProperties
            .createRetryConfig("backendWithSharedConfig", compositeRetryCustomizer());
        assertThat(retry2).isNotNull();
        assertThat(retry2.getMaxAttempts()).isEqualTo(2);
        assertThat(retry2.getIntervalBiFunction().apply(1, null)).isEqualTo(300L);

        // Unknown backend should get default config of Registry
        RetryConfig retry3 = retryConfigurationProperties
            .createRetryConfig("unknownBackend", compositeRetryCustomizer());
        assertThat(retry3).isNotNull();
        assertThat(retry3.getMaxAttempts()).isEqualTo(3);
    }

    @Test
    public void testCreatePropertiesWithUnknownConfig() {
        RetryConfigurationProperties retryConfigurationProperties = new RetryConfigurationProperties();
        RetryConfigurationProperties.InstanceProperties instanceProperties = new RetryConfigurationProperties.InstanceProperties();
        instanceProperties.setBaseConfig("unknownConfig");
        retryConfigurationProperties.getInstances().put("backend", instanceProperties);
        CompositeCustomizer<RetryConfigCustomizer> customizer = compositeRetryCustomizer();

        assertThatThrownBy(() -> retryConfigurationProperties.createRetryConfig("backend", customizer))
            .isInstanceOf(ConfigurationNotFoundException.class)
            .hasMessage("Configuration with name 'unknownConfig' does not exist");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgumentOnEventConsumerBufferSize() {
        RetryConfigurationProperties.InstanceProperties defaultProperties = new RetryConfigurationProperties.InstanceProperties();
        defaultProperties.setEventConsumerBufferSize(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgumentOnWaitDuration() {
        RetryConfigurationProperties.InstanceProperties defaultProperties = new RetryConfigurationProperties.InstanceProperties();
        defaultProperties.setWaitDuration(Duration.ofMillis(-1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgumentOnMaxRetryAttempts() {
        RetryConfigurationProperties.InstanceProperties defaultProperties = new RetryConfigurationProperties.InstanceProperties();
        defaultProperties.setMaxRetryAttempts(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgumentOnMaxAttempts() {
        RetryConfigurationProperties.InstanceProperties defaultProperties = new RetryConfigurationProperties.InstanceProperties();
        defaultProperties.setMaxAttempts(0);
    }

    @Test
    public void testIntervalBiFunctionConfig() {
        RetryConfigurationProperties.InstanceProperties instanceProperties = new RetryConfigurationProperties.InstanceProperties();
        instanceProperties.setIntervalBiFunction(TestIntervalBiFunction.class);

        RetryConfigurationProperties retryConfigurationProperties = new RetryConfigurationProperties();
        retryConfigurationProperties.getInstances().put("backend", instanceProperties);

        RetryConfig retryConfig = retryConfigurationProperties
            .createRetryConfig("backend", compositeRetryCustomizer());

        assertThat(retryConfig.getIntervalBiFunction()).isNotNull();
        assertThat(retryConfig.getIntervalBiFunction()).isExactlyInstanceOf(TestIntervalBiFunction.class);
    }

    @Test
    public void testRetryConfigWithBaseConfig() {
        RetryConfigurationProperties.InstanceProperties defaultConfig = new RetryConfigurationProperties.InstanceProperties();
        defaultConfig.setMaxAttempts(10);
        defaultConfig.setWaitDuration(Duration.ofMillis(100L));

        RetryConfigurationProperties.InstanceProperties sharedConfigWithDefaultConfig = new RetryConfigurationProperties.InstanceProperties();
        sharedConfigWithDefaultConfig.setWaitDuration(Duration.ofMillis(1000L));
        sharedConfigWithDefaultConfig.setBaseConfig("defaultConfig");

        RetryConfigurationProperties.InstanceProperties instanceWithSharedConfig = new RetryConfigurationProperties.InstanceProperties();
        instanceWithSharedConfig.setBaseConfig("sharedConfig");


        RetryConfigurationProperties retryConfigurationProperties = new RetryConfigurationProperties();
        retryConfigurationProperties.getConfigs().put("defaultConfig", defaultConfig);
        retryConfigurationProperties.getConfigs().put("sharedConfig", sharedConfigWithDefaultConfig);
        retryConfigurationProperties.getInstances().put("instanceWithSharedConfig", instanceWithSharedConfig);


        RetryConfig instance = retryConfigurationProperties
            .createRetryConfig("instanceWithSharedConfig", compositeRetryCustomizer());
        assertThat(instance).isNotNull();
        assertThat(instance.getMaxAttempts()).isEqualTo(10);
        assertThat(instance.getIntervalBiFunction().apply(1, null)).isEqualTo(1000L);
    }

    private CompositeCustomizer<RetryConfigCustomizer> compositeRetryCustomizer() {
        return new CompositeCustomizer<>(Collections.emptyList());
    }
}
