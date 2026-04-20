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

package io.github.resilience4j.commons.configuration.circuitbreaker.configure;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.common.circuitbreaker.configuration.CommonCircuitBreakerConfigurationProperties;
import io.github.resilience4j.commons.configuration.dummy.DummyIgnoredException;
import io.github.resilience4j.commons.configuration.dummy.DummyPredicateThrowable;
import io.github.resilience4j.commons.configuration.util.CommonsConfigurationUtil;
import io.github.resilience4j.commons.configuration.util.TestConstants;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.YAMLConfiguration;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CommonsConfigurationCircuitBreakerConfigurationTest {

    @Test
    void fromPropertiesFile() throws Exception {
        Configuration config = CommonsConfigurationUtil.getConfiguration(PropertiesConfiguration.class, TestConstants.RESILIENCE_CONFIG_PROPERTIES_FILE_NAME);

        CommonsConfigurationCircuitBreakerConfiguration commonsConfigurationCircuitBreakerConfiguration =
                CommonsConfigurationCircuitBreakerConfiguration.of(config);

        assertConfigs(commonsConfigurationCircuitBreakerConfiguration.getConfigs());
        assertInstances(commonsConfigurationCircuitBreakerConfiguration.getInstances());
    }

    @Test
    void fromYamlFile() throws Exception {
        Configuration config = CommonsConfigurationUtil.getConfiguration(YAMLConfiguration.class, TestConstants.RESILIENCE_CONFIG_YAML_FILE_NAME);

        CommonsConfigurationCircuitBreakerConfiguration commonsConfigurationCircuitBreakerConfiguration =
                CommonsConfigurationCircuitBreakerConfiguration.of(config);

        assertConfigs(commonsConfigurationCircuitBreakerConfiguration.getConfigs());
        assertInstances(commonsConfigurationCircuitBreakerConfiguration.getInstances());
    }

    private static void assertConfigs(Map<String, CommonCircuitBreakerConfigurationProperties.InstanceProperties> config) {
        assertThat(config)
                .hasSize(2)
                .containsKey(TestConstants.DEFAULT);
        assertConfigDefault(config.get(TestConstants.DEFAULT));
        assertThat(config).containsKey(TestConstants.SHARED);
        assertConfigShared(config.get(TestConstants.SHARED));
    }

    private static void assertConfigDefault(CommonCircuitBreakerConfigurationProperties.InstanceProperties configDefault) {
        assertThat(configDefault.getSlidingWindowSize()).isEqualTo(100);
        assertThat(configDefault.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(10);
        assertThat(configDefault.getWaitDurationInOpenState()).isEqualTo(Duration.ofMinutes(1));
        assertThat(configDefault.getFailureRateThreshold()).isEqualTo(50f);
        assertThat(configDefault.getSlowCallRateThreshold()).isEqualTo(50f);
        assertThat(configDefault.getSlowCallDurationThreshold()).isEqualTo(Duration.ofSeconds(2));
        assertThat(configDefault.getWritableStackTraceEnabled()).isTrue();
        assertThat(configDefault.getMaxWaitDurationInHalfOpenState()).isEqualTo(Duration.ofSeconds(10));
        assertThat(configDefault.getTransitionToStateAfterWaitDuration()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(configDefault.getAutomaticTransitionFromOpenToHalfOpenEnabled()).isTrue();
        assertThat(configDefault.getInitialState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(configDefault.getSlidingWindowType()).isEqualTo(CircuitBreakerConfig.SlidingWindowType.TIME_BASED);
        assertThat(configDefault.getMinimumNumberOfCalls()).isEqualTo(5);
        assertThat(configDefault.getEventConsumerBufferSize()).isEqualTo(10);
        assertThat(configDefault.getRegisterHealthIndicator()).isTrue();
        assertThat(configDefault.getRecordFailurePredicate()).isEqualTo(DummyPredicateThrowable.class);
        assertThat(configDefault.getRecordExceptions()).containsExactly(IllegalArgumentException.class, NullPointerException.class);
        assertThat(configDefault.getIgnoreExceptionPredicate()).isEqualTo(DummyPredicateThrowable.class);
        assertThat(configDefault.getIgnoreExceptions()).containsExactly(IllegalStateException.class, IndexOutOfBoundsException.class);
    }

    private static void assertConfigShared(CommonCircuitBreakerConfigurationProperties.InstanceProperties configShared) {
        assertThat(configShared.getSlidingWindowType()).isEqualTo(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED);
        assertThat(configShared.getSlidingWindowSize()).isEqualTo(100);
        assertThat(configShared.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(30);
        assertThat(configShared.getWaitDurationInOpenState()).isEqualTo(Duration.ofSeconds(10));
        assertThat(configShared.getFailureRateThreshold()).isEqualTo(50f);
        assertThat(configShared.getEventConsumerBufferSize()).isEqualTo(10);
        assertThat(configShared.getIgnoreExceptions()).containsExactly(DummyIgnoredException.class);
    }

    private static void assertInstances(Map<String, CommonCircuitBreakerConfigurationProperties.InstanceProperties> instances) {
        assertThat(instances)
                .hasSize(2)
                .containsKey(TestConstants.BACKEND_A);
        assertInstanceBackendA(instances.get(TestConstants.BACKEND_A));
        assertInstanceBackendB(instances.get(TestConstants.BACKEND_B));
    }

    private static void assertInstanceBackendA(CommonCircuitBreakerConfigurationProperties.InstanceProperties instanceBackendA) {
        assertThat(instanceBackendA.getBaseConfig()).isEqualTo(TestConstants.DEFAULT);
        assertThat(instanceBackendA.getWaitDurationInOpenState()).isEqualTo(Duration.ofSeconds(5));
    }

    private static void assertInstanceBackendB(CommonCircuitBreakerConfigurationProperties.InstanceProperties instanceBackendB) {
        assertThat(instanceBackendB.getBaseConfig()).isEqualTo(TestConstants.SHARED);
        assertThat(instanceBackendB.getRegisterHealthIndicator()).isTrue();
        assertThat(instanceBackendB.getSlidingWindowSize()).isEqualTo(10);
        assertThat(instanceBackendB.getMinimumNumberOfCalls()).isEqualTo(10);
        assertThat(instanceBackendB.getFailureRateThreshold()).isEqualTo(60f);
        assertThat(instanceBackendB.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(3);
        assertThat(instanceBackendB.getWaitDurationInOpenState()).isEqualTo(Duration.ofSeconds(5));
        assertThat(instanceBackendB.getEventConsumerBufferSize()).isEqualTo(20);
        assertThat(instanceBackendB.getRecordFailurePredicate()).isEqualTo(DummyPredicateThrowable.class);
        assertThat(instanceBackendB.getInitialState()).isEqualTo(CircuitBreaker.State.METRICS_ONLY);
    }
}
