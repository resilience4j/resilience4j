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

import io.github.resilience4j.common.RecordFailurePredicate;
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
        //Given
        RetryConfigurationProperties.InstanceProperties instanceProperties1 = new RetryConfigurationProperties.InstanceProperties();
        instanceProperties1.setMaxRetryAttempts(3);
        instanceProperties1.setWaitDuration(Duration.ofMillis(1000));
        instanceProperties1.setEnableExponentialBackoff(false);
        instanceProperties1.setEnableRandomizedWait(false);
        instanceProperties1.setEventConsumerBufferSize(100);
        instanceProperties1.setRetryExceptions(new Class[]{IllegalStateException.class});
        instanceProperties1.setIgnoreExceptions(new Class[]{IllegalArgumentException.class});
        instanceProperties1.setRetryExceptionPredicate(RecordFailurePredicate.class);

        RetryConfigurationProperties.InstanceProperties instanceProperties2 = new RetryConfigurationProperties.InstanceProperties();
        instanceProperties2.setMaxRetryAttempts(2);
        instanceProperties2.setEnableExponentialBackoff(true);
        instanceProperties2.setExponentialBackoffMultiplier(1.0);
        instanceProperties2.setWaitDuration(Duration.ofMillis(100L));

        RetryConfigurationProperties retryConfigurationProperties = new RetryConfigurationProperties();
        retryConfigurationProperties.getInstances().put("backend1", instanceProperties1);
        retryConfigurationProperties.getInstances().put("backend2", instanceProperties2);
        Map<String,String> globalTagsForRetries=new HashMap<>();
        globalTagsForRetries.put("testKey1","testKet2");
        retryConfigurationProperties.setTags(globalTagsForRetries);
        //Then
        assertThat(retryConfigurationProperties.getTags().size()).isEqualTo(1);
        assertThat(retryConfigurationProperties.getInstances().size()).isEqualTo(2);
        assertThat(retryConfigurationProperties.getBackends().size()).isEqualTo(2);
        final RetryConfig retry1 = retryConfigurationProperties
            .createRetryConfig("backend1", compositeRetryCustomizer());
        final RetryConfig retry2 = retryConfigurationProperties
            .createRetryConfig("backend2", compositeRetryCustomizer());
        RetryConfigurationProperties.InstanceProperties instancePropertiesForRetry1 = retryConfigurationProperties
            .getInstances().get("backend1");
        assertThat(instancePropertiesForRetry1.getWaitDuration().toMillis()).isEqualTo(1000);
        assertThat(retry1).isNotNull();
        assertThat(retry1.getMaxAttempts()).isEqualTo(3);

        assertThat(retry2).isNotNull();
        assertThat(retry2.getMaxAttempts()).isEqualTo(2);


    }

    @Test
    public void testCreateRetryPropertiesWithSharedConfigs() {
        //Given
        RetryConfigurationProperties.InstanceProperties defaultProperties = new RetryConfigurationProperties.InstanceProperties();
        defaultProperties.setMaxRetryAttempts(3);
        defaultProperties.setWaitDuration(Duration.ofMillis(100L));

        RetryConfigurationProperties.InstanceProperties sharedProperties = new RetryConfigurationProperties.InstanceProperties();
        sharedProperties.setMaxRetryAttempts(2);
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

        retryConfigurationProperties.getInstances()
            .put("backendWithDefaultConfig", backendWithDefaultConfig);
        retryConfigurationProperties.getInstances()
            .put("backendWithSharedConfig", backendWithSharedConfig);

        //Then
        // Should get default config and overwrite max attempt and wait time
        RetryConfig retry1 = retryConfigurationProperties
            .createRetryConfig("backendWithDefaultConfig", compositeRetryCustomizer());
        assertThat(retry1).isNotNull();
        assertThat(retry1.getMaxAttempts()).isEqualTo(3);
        assertThat(retry1.getIntervalFunction().apply(1)).isEqualTo(200L);

        // Should get shared config and overwrite wait time
        RetryConfig retry2 = retryConfigurationProperties
            .createRetryConfig("backendWithSharedConfig", compositeRetryCustomizer());
        assertThat(retry2).isNotNull();
        assertThat(retry2.getMaxAttempts()).isEqualTo(2);
        assertThat(retry2.getIntervalFunction().apply(1)).isEqualTo(300L);

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

        //then
        assertThatThrownBy(() -> retryConfigurationProperties
            .createRetryConfig("backend", compositeRetryCustomizer()))
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
        defaultProperties.setWaitDuration(Duration.ofMillis(50));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgumentOnMaxRetryAttempts() {
        RetryConfigurationProperties.InstanceProperties defaultProperties = new RetryConfigurationProperties.InstanceProperties();
        defaultProperties.setMaxRetryAttempts(0);
    }

    private CompositeRetryCustomizer compositeRetryCustomizer() {
        return new CompositeRetryCustomizer(Collections.emptyList());
    }
}