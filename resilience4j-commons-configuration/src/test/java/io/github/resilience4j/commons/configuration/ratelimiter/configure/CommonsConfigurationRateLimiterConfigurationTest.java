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

package io.github.resilience4j.commons.configuration.ratelimiter.configure;

import io.github.resilience4j.common.ratelimiter.configuration.CommonRateLimiterConfigurationProperties;
import io.github.resilience4j.commons.configuration.util.CommonsConfigurationUtil;
import io.github.resilience4j.commons.configuration.util.TestConstants;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.YAMLConfiguration;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CommonsConfigurationRateLimiterConfigurationTest {
    @Test
    void fromPropertiesFile() throws Exception {
        Configuration config = CommonsConfigurationUtil.getConfiguration(PropertiesConfiguration.class, TestConstants.RESILIENCE_CONFIG_PROPERTIES_FILE_NAME);

        CommonsConfigurationRateLimiterConfiguration bulkHeadConfiguration  = CommonsConfigurationRateLimiterConfiguration.of(config);

        assertConfigs(bulkHeadConfiguration.getConfigs());
        assertInstances(bulkHeadConfiguration.getInstances());
    }

    @Test
    void fromYamlFile() throws Exception {
        Configuration config = CommonsConfigurationUtil.getConfiguration(YAMLConfiguration.class, TestConstants.RESILIENCE_CONFIG_YAML_FILE_NAME);

        CommonsConfigurationRateLimiterConfiguration bulkHeadConfiguration  = CommonsConfigurationRateLimiterConfiguration.of(config);

        assertConfigs(bulkHeadConfiguration.getConfigs());
        assertInstances(bulkHeadConfiguration.getInstances());
    }

    private void assertConfigs(Map<String, CommonRateLimiterConfigurationProperties.InstanceProperties> config) {
        assertThat(config)
                .hasSize(1)
                .containsKey(TestConstants.DEFAULT);
        assertConfigDefault(config.get(TestConstants.DEFAULT));
    }

    private void assertInstances(Map<String, CommonRateLimiterConfigurationProperties.InstanceProperties> instances) {
        assertThat(instances)
                .hasSize(2)
                .containsKey(TestConstants.BACKEND_A)
                .containsKey(TestConstants.BACKEND_B);
        assertInstanceBackendA(instances.get(TestConstants.BACKEND_A));
        assertInstanceBackendB(instances.get(TestConstants.BACKEND_B));
    }

    private void assertConfigDefault(CommonRateLimiterConfigurationProperties.InstanceProperties configDefault) {
        assertThat(configDefault.getLimitForPeriod()).isEqualTo(10);
        assertThat(configDefault.getLimitRefreshPeriod()).isEqualTo(Duration.ofSeconds(1));
        assertThat(configDefault.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(10));
        assertThat(configDefault.getSubscribeForEvents()).isNull();
        assertThat(configDefault.getAllowHealthIndicatorToFail()).isNull();
        assertThat(configDefault.getRegisterHealthIndicator()).isFalse();
        assertThat(configDefault.getEventConsumerBufferSize()).isEqualTo(100);
        assertThat(configDefault.getWritableStackTraceEnabled()).isNull();

    }

    private void assertInstanceBackendA(CommonRateLimiterConfigurationProperties.InstanceProperties instanceBackendA) {
        assertThat(instanceBackendA.getBaseConfig()).isEqualTo(TestConstants.DEFAULT);
        assertThat(instanceBackendA.getLimitForPeriod()).isNull();
        assertThat(instanceBackendA.getLimitRefreshPeriod()).isNull();
        assertThat(instanceBackendA.getTimeoutDuration()).isNull();
        assertThat(instanceBackendA.getSubscribeForEvents()).isNull();
        assertThat(instanceBackendA.getAllowHealthIndicatorToFail()).isNull();
        assertThat(instanceBackendA.getRegisterHealthIndicator()).isNull();
        assertThat(instanceBackendA.getEventConsumerBufferSize()).isNull();
        assertThat(instanceBackendA.getWritableStackTraceEnabled()).isNull();
    }

    private void assertInstanceBackendB(CommonRateLimiterConfigurationProperties.InstanceProperties instanceBackendB) {
        assertThat(instanceBackendB.getBaseConfig()).isNull();
        assertThat(instanceBackendB.getLimitForPeriod()).isEqualTo(6);
        assertThat(instanceBackendB.getLimitRefreshPeriod()).isEqualTo(Duration.ofMillis(500));
        assertThat(instanceBackendB.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(3));
        assertThat(instanceBackendB.getSubscribeForEvents()).isTrue();
        assertThat(instanceBackendB.getAllowHealthIndicatorToFail()).isFalse();
        assertThat(instanceBackendB.getRegisterHealthIndicator()).isTrue();
        assertThat(instanceBackendB.getEventConsumerBufferSize()).isEqualTo(10);
        assertThat(instanceBackendB.getWritableStackTraceEnabled()).isFalse();
    }
}