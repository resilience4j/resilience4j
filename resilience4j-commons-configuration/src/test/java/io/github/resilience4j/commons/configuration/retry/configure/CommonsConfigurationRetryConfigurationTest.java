/*
 *   Copyright 2026: Deepak Kumar
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.github.resilience4j.commons.configuration.retry.configure;

import io.github.resilience4j.common.retry.configuration.CommonRetryConfigurationProperties;
import io.github.resilience4j.commons.configuration.dummy.DummyIgnoredException;
import io.github.resilience4j.commons.configuration.dummy.DummyPredicateObject;
import io.github.resilience4j.commons.configuration.dummy.DummyPredicateThrowable;
import io.github.resilience4j.commons.configuration.util.CommonsConfigurationUtil;
import io.github.resilience4j.commons.configuration.util.TestConstants;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.YAMLConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class CommonsConfigurationRetryConfigurationTest {

    @Test
    void fromPropertiesFile() throws Exception {
        Configuration config = CommonsConfigurationUtil.getConfiguration(PropertiesConfiguration.class, TestConstants.RESILIENCE_CONFIG_PROPERTIES_FILE_NAME);

        CommonsConfigurationRetryConfiguration retryConfiguration = CommonsConfigurationRetryConfiguration.of(config);

        assertConfigs(retryConfiguration.getConfigs());
        assertInstances(retryConfiguration.getInstances());
    }

    @Test
    void fromYamlFile() throws Exception {
        Configuration config = CommonsConfigurationUtil.getConfiguration(YAMLConfiguration.class, TestConstants.RESILIENCE_CONFIG_YAML_FILE_NAME);

        CommonsConfigurationRetryConfiguration retryConfiguration = CommonsConfigurationRetryConfiguration.of(config);

        assertConfigs(retryConfiguration.getConfigs());
        assertInstances(retryConfiguration.getInstances());
    }

    private void assertConfigs(Map<String, CommonRetryConfigurationProperties.InstanceProperties> configs) {
        assertThat(configs)
                .hasSize(1)
                .containsKey(TestConstants.DEFAULT);
        assertConfigDefault(configs.get(TestConstants.DEFAULT));
    }

    private void assertConfigDefault(CommonRetryConfigurationProperties.InstanceProperties instanceProperties) {
        assertThat(instanceProperties.getMaxAttempts()).isEqualTo(3);
        assertThat(instanceProperties.getWaitDuration()).isEqualTo(Duration.ofSeconds(10));
        assertThat(instanceProperties.getRetryExceptions()).containsExactlyInAnyOrder(TimeoutException.class,
                IOException.class);
        assertThat(instanceProperties.getIgnoreExceptions()).containsExactlyInAnyOrder(DummyIgnoredException.class, RuntimeException.class);
    }

    private void assertInstances(Map<String, CommonRetryConfigurationProperties.InstanceProperties> instances) {
        assertThat(instances)
                .hasSize(2)
                .containsKey(TestConstants.BACKEND_A)
                .containsKey(TestConstants.BACKEND_B);
        assertInstanceBackendA(instances.get(TestConstants.BACKEND_A));
        assertInstanceBackendB(instances.get(TestConstants.BACKEND_B));
    }

    private void assertInstanceBackendA(CommonRetryConfigurationProperties.InstanceProperties instanceBackendA) {
        assertThat(instanceBackendA.getBaseConfig()).isEqualTo(TestConstants.DEFAULT);
        assertThat(instanceBackendA.getMaxAttempts()).isNull();
        assertThat(instanceBackendA.getWaitDuration()).isNull();
        assertThat(instanceBackendA.getIgnoreExceptions()).isNull();
        assertThat(instanceBackendA.getRetryExceptions()).isNull();
        ;
    }

    private void assertInstanceBackendB(CommonRetryConfigurationProperties.InstanceProperties instanceBackendB) {
        assertThat(instanceBackendB.getBaseConfig()).isNull();
        assertThat(instanceBackendB.getMaxAttempts()).isEqualTo(5);
        assertThat(instanceBackendB.getRetryExceptionPredicate()).isEqualTo(DummyPredicateThrowable.class);
        assertThat(instanceBackendB.getResultPredicate()).isEqualTo(DummyPredicateObject.class);
        assertThat(instanceBackendB.getRetryExceptions()).containsExactlyInAnyOrder(TimeoutException.class, IOException.class);
        assertThat(instanceBackendB.getIgnoreExceptions()).containsExactlyInAnyOrder(DummyIgnoredException.class, RuntimeException.class);
        assertThat(instanceBackendB.getEventConsumerBufferSize()).isEqualTo(10);
        assertThat(instanceBackendB.getEnableExponentialBackoff()).isTrue();
        assertThat(instanceBackendB.getExponentialBackoffMultiplier()).isEqualTo(2);
        assertThat(instanceBackendB.getExponentialMaxWaitDuration()).isEqualTo(Duration.ofSeconds(10));
        assertThat(instanceBackendB.getEnableRandomizedWait()).isFalse();
        assertThat(instanceBackendB.getFailAfterMaxAttempts()).isTrue();
    }


}