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
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.time.Duration;
import java.util.Map;

public class CommonsConfigurationCircuitBreakerConfigurationTest {

    @Test
    public void testFromPropertiesFile() throws ConfigurationException {
        Configuration config = CommonsConfigurationUtil.getConfiguration(PropertiesConfiguration.class, TestConstants.RESILIENCE_CONFIG_PROPERTIES_FILE_NAME);

        CommonsConfigurationCircuitBreakerConfiguration commonsConfigurationCircuitBreakerConfiguration =
                CommonsConfigurationCircuitBreakerConfiguration.of(config);

        assertConfigs(commonsConfigurationCircuitBreakerConfiguration.getConfigs());
        assertInstances(commonsConfigurationCircuitBreakerConfiguration.getInstances());
    }

    @Test
    public void testFromYamlFile() throws ConfigurationException {
        Configuration config = CommonsConfigurationUtil.getConfiguration(YAMLConfiguration.class, TestConstants.RESILIENCE_CONFIG_YAML_FILE_NAME);

        CommonsConfigurationCircuitBreakerConfiguration commonsConfigurationCircuitBreakerConfiguration =
                CommonsConfigurationCircuitBreakerConfiguration.of(config);

        assertConfigs(commonsConfigurationCircuitBreakerConfiguration.getConfigs());
        assertInstances(commonsConfigurationCircuitBreakerConfiguration.getInstances());
    }

    private static void assertConfigs(Map<String, CommonCircuitBreakerConfigurationProperties.InstanceProperties> config) {
        Assertions.assertThat(config.size()).isEqualTo(2);
        Assertions.assertThat(config.containsKey(TestConstants.DEFAULT)).isTrue();
        assertConfigDefault(config.get(TestConstants.DEFAULT));
        Assertions.assertThat(config.containsKey(TestConstants.SHARED)).isTrue();
        assertConfigShared(config.get(TestConstants.SHARED));
    }

    private static void assertConfigDefault(CommonCircuitBreakerConfigurationProperties.InstanceProperties configDefault) {
        Assertions.assertThat(configDefault.getSlidingWindowSize()).isEqualTo(100);
        Assertions.assertThat(configDefault.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(10);
        Assertions.assertThat(configDefault.getWaitDurationInOpenState()).isEqualTo(Duration.ofMinutes(1));
        Assertions.assertThat(configDefault.getFailureRateThreshold()).isEqualTo(50f);
        Assertions.assertThat(configDefault.getSlowCallRateThreshold()).isEqualTo(50f);
        Assertions.assertThat(configDefault.getSlowCallDurationThreshold()).isEqualTo(Duration.ofSeconds(2));
        Assertions.assertThat(configDefault.getWritableStackTraceEnabled()).isEqualTo(true);
        Assertions.assertThat(configDefault.getMaxWaitDurationInHalfOpenState()).isEqualTo(Duration.ofSeconds(10));
        Assertions.assertThat(configDefault.getTransitionToStateAfterWaitDuration()).isEqualTo(CircuitBreaker.State.OPEN);
        Assertions.assertThat(configDefault.getAutomaticTransitionFromOpenToHalfOpenEnabled()).isEqualTo(true);
        Assertions.assertThat(configDefault.getInitialState()).isEqualTo(CircuitBreaker.State.CLOSED);
        Assertions.assertThat(configDefault.getSlidingWindowType()).isEqualTo(CircuitBreakerConfig.SlidingWindowType.TIME_BASED);
        Assertions.assertThat(configDefault.getMinimumNumberOfCalls()).isEqualTo(5);
        Assertions.assertThat(configDefault.getEventConsumerBufferSize()).isEqualTo(10);
        Assertions.assertThat(configDefault.getRegisterHealthIndicator()).isEqualTo(true);
        Assertions.assertThat(configDefault.getRecordFailurePredicate()).isEqualTo(DummyPredicateThrowable.class);
        Assertions.assertThat(configDefault.getRecordExceptions()).containsExactly(IllegalArgumentException.class, NullPointerException.class);
        Assertions.assertThat(configDefault.getIgnoreExceptionPredicate()).isEqualTo(DummyPredicateThrowable.class);
        Assertions.assertThat(configDefault.getIgnoreExceptions()).containsExactly(IllegalStateException.class, IndexOutOfBoundsException.class);
    }

    private static void assertConfigShared(CommonCircuitBreakerConfigurationProperties.InstanceProperties configShared) {
        Assertions.assertThat(configShared.getSlidingWindowType()).isEqualTo(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED);
        Assertions.assertThat(configShared.getSlidingWindowSize()).isEqualTo(100);
        Assertions.assertThat(configShared.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(30);
        Assertions.assertThat(configShared.getWaitDurationInOpenState()).isEqualTo(Duration.ofSeconds(10));
        Assertions.assertThat(configShared.getFailureRateThreshold()).isEqualTo(50f);
        Assertions.assertThat(configShared.getEventConsumerBufferSize()).isEqualTo(10);
        Assertions.assertThat(configShared.getIgnoreExceptions()).containsExactly(DummyIgnoredException.class);
    }

    private static void assertInstances(Map<String, CommonCircuitBreakerConfigurationProperties.InstanceProperties> instances) {
        Assertions.assertThat(instances.size()).isEqualTo(2);
        Assertions.assertThat(instances.containsKey(TestConstants.BACKEND_A)).isTrue();
        assertInstanceBackendA(instances.get(TestConstants.BACKEND_A));
        assertInstanceBackendB(instances.get(TestConstants.BACKEND_B));
    }

    private static void assertInstanceBackendA(CommonCircuitBreakerConfigurationProperties.InstanceProperties instanceBackendA) {
        Assertions.assertThat(instanceBackendA.getBaseConfig()).isEqualTo(TestConstants.DEFAULT);
        Assertions.assertThat(instanceBackendA.getWaitDurationInOpenState()).isEqualTo(Duration.ofSeconds(5));
    }

    private static void assertInstanceBackendB(CommonCircuitBreakerConfigurationProperties.InstanceProperties instanceBackendB) {
        Assertions.assertThat(instanceBackendB.getBaseConfig()).isEqualTo(TestConstants.SHARED);
        Assertions.assertThat(instanceBackendB.getRegisterHealthIndicator()).isEqualTo(true);
        Assertions.assertThat(instanceBackendB.getSlidingWindowSize()).isEqualTo(10);
        Assertions.assertThat(instanceBackendB.getMinimumNumberOfCalls()).isEqualTo(10);
        Assertions.assertThat(instanceBackendB.getFailureRateThreshold()).isEqualTo(60f);
        Assertions.assertThat(instanceBackendB.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(3);
        Assertions.assertThat(instanceBackendB.getWaitDurationInOpenState()).isEqualTo(Duration.ofSeconds(5));
        Assertions.assertThat(instanceBackendB.getEventConsumerBufferSize()).isEqualTo(20);
        Assertions.assertThat(instanceBackendB.getRecordFailurePredicate()).isEqualTo(DummyPredicateThrowable.class);
        Assertions.assertThat(instanceBackendB.getInitialState()).isEqualTo(CircuitBreaker.State.METRICS_ONLY);
    }
}
