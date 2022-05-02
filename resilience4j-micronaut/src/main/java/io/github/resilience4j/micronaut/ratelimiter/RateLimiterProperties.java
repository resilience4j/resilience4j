/*
 * Copyright 2019 Michael Pollind
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
package io.github.resilience4j.micronaut.ratelimiter;

import io.github.resilience4j.common.ratelimiter.configuration.CommonRateLimiterConfigurationProperties;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.naming.Named;
import io.micronaut.core.util.Toggleable;

import java.util.List;

/**
 * Configuration for generic rate limiting registry.
 *
 */
@ConfigurationProperties("resilience4j.ratelimiter")
public class RateLimiterProperties extends CommonRateLimiterConfigurationProperties implements Toggleable {
    private boolean enabled;

    public RateLimiterProperties(
        List<RateLimiterProperties.InstancePropertiesConfigs> configs,
        List<RateLimiterProperties.InstancePropertiesInstances> instances) {
        for (RateLimiterProperties.InstancePropertiesConfigs config : configs) {
            this.getConfigs().put(config.getName(), config);
        }
        for (RateLimiterProperties.InstancePropertiesInstances instance : instances) {
            this.getInstances().put(instance.getName(), instance);
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @EachProperty(value = "configs", primary = "default")
    public static class InstancePropertiesConfigs extends CommonRateLimiterConfigurationProperties.InstanceProperties implements Named {
        private final String name;

        public InstancePropertiesConfigs(@Parameter String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    @EachProperty(value = "instances", primary = "default")
    public static class InstancePropertiesInstances extends CommonRateLimiterConfigurationProperties.InstanceProperties implements Named {
        private final String name;

        public InstancePropertiesInstances(@Parameter String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
