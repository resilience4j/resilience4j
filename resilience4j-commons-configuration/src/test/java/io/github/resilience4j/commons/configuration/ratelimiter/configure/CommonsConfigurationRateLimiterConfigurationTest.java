/*
 *   Copyright 2023: Deepak Kumar
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

package io.github.resilience4j.commons.configuration.ratelimiter.configure;

import io.github.resilience4j.common.ratelimiter.configuration.CommonRateLimiterConfigurationProperties;
import io.github.resilience4j.commons.configuration.util.CommonsConfigurationUtil;
import io.github.resilience4j.commons.configuration.util.TestConstants;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.YAMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.time.Duration;
import java.util.Map;

public class CommonsConfigurationRateLimiterConfigurationTest {
    @Test
    public void testFromPropertiesFile() throws ConfigurationException {
        Configuration config = CommonsConfigurationUtil.getConfiguration(PropertiesConfiguration.class, TestConstants.RESILIENCE_CONFIG_PROPERTIES_FILE_NAME);

        CommonsConfigurationRateLimiterConfiguration bulkHeadConfiguration  = CommonsConfigurationRateLimiterConfiguration.of(config);

        assertConfigs(bulkHeadConfiguration.getConfigs());
        assertInstances(bulkHeadConfiguration.getInstances());
    }

    @Test
    public void testFromYamlFile() throws ConfigurationException {
        Configuration config = CommonsConfigurationUtil.getConfiguration(YAMLConfiguration.class, TestConstants.RESILIENCE_CONFIG_YAML_FILE_NAME);

        CommonsConfigurationRateLimiterConfiguration bulkHeadConfiguration  = CommonsConfigurationRateLimiterConfiguration.of(config);

        assertConfigs(bulkHeadConfiguration.getConfigs());
        assertInstances(bulkHeadConfiguration.getInstances());
    }

    private void assertConfigs(Map<String, CommonRateLimiterConfigurationProperties.InstanceProperties> config) {
        Assertions.assertThat(config.size()).isEqualTo(1);
        Assertions.assertThat(config.containsKey(TestConstants.DEFAULT)).isTrue();
        assertConfigDefault(config.get(TestConstants.DEFAULT));
    }

    private void assertInstances(Map<String, CommonRateLimiterConfigurationProperties.InstanceProperties> instances) {
        Assertions.assertThat(instances.size()).isEqualTo(2);
        Assertions.assertThat(instances.containsKey(TestConstants.BACKEND_A)).isTrue();
        Assertions.assertThat(instances.containsKey(TestConstants.BACKEND_B)).isTrue();
        assertInstanceBackendA(instances.get(TestConstants.BACKEND_A));
        assertInstanceBackendB(instances.get(TestConstants.BACKEND_B));
    }

    private void assertConfigDefault(CommonRateLimiterConfigurationProperties.InstanceProperties configDefault) {
        Assertions.assertThat(configDefault.getLimitForPeriod()).isEqualTo(10);
        Assertions.assertThat(configDefault.getLimitRefreshPeriod()).isEqualTo(Duration.ofSeconds(1));
        Assertions.assertThat(configDefault.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(10));
        Assertions.assertThat(configDefault.getSubscribeForEvents()).isNull();
        Assertions.assertThat(configDefault.getAllowHealthIndicatorToFail()).isNull();
        Assertions.assertThat(configDefault.getRegisterHealthIndicator()).isFalse();
        Assertions.assertThat(configDefault.getEventConsumerBufferSize()).isEqualTo(100);
        Assertions.assertThat(configDefault.getWritableStackTraceEnabled()).isNull();

    }

    private void assertInstanceBackendA(CommonRateLimiterConfigurationProperties.InstanceProperties instanceBackendA) {
        Assertions.assertThat(instanceBackendA.getBaseConfig()).isEqualTo(TestConstants.DEFAULT);
        Assertions.assertThat(instanceBackendA.getLimitForPeriod()).isNull();
        Assertions.assertThat(instanceBackendA.getLimitRefreshPeriod()).isNull();
        Assertions.assertThat(instanceBackendA.getTimeoutDuration()).isNull();
        Assertions.assertThat(instanceBackendA.getSubscribeForEvents()).isNull();
        Assertions.assertThat(instanceBackendA.getAllowHealthIndicatorToFail()).isNull();
        Assertions.assertThat(instanceBackendA.getRegisterHealthIndicator()).isNull();
        Assertions.assertThat(instanceBackendA.getEventConsumerBufferSize()).isNull();
        Assertions.assertThat(instanceBackendA.getWritableStackTraceEnabled()).isNull();
    }

    private void assertInstanceBackendB(CommonRateLimiterConfigurationProperties.InstanceProperties instanceBackendB) {
        Assertions.assertThat(instanceBackendB.getBaseConfig()).isNull();
        Assertions.assertThat(instanceBackendB.getLimitForPeriod()).isEqualTo(6);
        Assertions.assertThat(instanceBackendB.getLimitRefreshPeriod()).isEqualTo(Duration.ofMillis(500));
        Assertions.assertThat(instanceBackendB.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(3));
        Assertions.assertThat(instanceBackendB.getSubscribeForEvents()).isTrue();
        Assertions.assertThat(instanceBackendB.getAllowHealthIndicatorToFail()).isFalse();
        Assertions.assertThat(instanceBackendB.getRegisterHealthIndicator()).isTrue();
        Assertions.assertThat(instanceBackendB.getEventConsumerBufferSize()).isEqualTo(10);
        Assertions.assertThat(instanceBackendB.getWritableStackTraceEnabled()).isFalse();
    }
}