/*
 * Copyright 2019 Ingyu Hwang
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

package io.github.resilience4j.circuitbreaker.autoconfigure;

import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfigurationProperties;
import io.github.resilience4j.common.circuitbreaker.configuration.CompositeCircuitBreakerCustomizer;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.core.registry.CompositeRegistryEventConsumer;
import org.junit.Test;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;
import java.util.Collections;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

public class AbstractRefreshScopedCircuitBreakerConfigurationTest {

    @Test
    public void testRefreshScopedCircuitBreakerConfig() {
        Arrays.stream(AbstractRefreshScopedCircuitBreakerConfiguration.class.getMethods())
            .filter(method -> method.isAnnotationPresent(Bean.class))
            .forEach(method -> assertThat(method.isAnnotationPresent(RefreshScope.class)).isTrue());
    }

    @Test
    public void testCircuitBreakerCloudCommonConfig() {
        CircuitBreakerConfig circuitBreakerConfig = new CircuitBreakerConfig(
            new CircuitBreakerConfigurationProperties());

        assertThat(circuitBreakerConfig.circuitBreakerRegistry(
            new DefaultEventConsumerRegistry<>(),
            new CompositeRegistryEventConsumer<>(emptyList()),
            new CompositeCircuitBreakerCustomizer(Collections.emptyList())))
            .isNotNull();
    }

    static class CircuitBreakerConfig extends AbstractRefreshScopedCircuitBreakerConfiguration {

        CircuitBreakerConfig(CircuitBreakerConfigurationProperties circuitBreakerProperties) {
            super(circuitBreakerProperties);
        }
    }
}