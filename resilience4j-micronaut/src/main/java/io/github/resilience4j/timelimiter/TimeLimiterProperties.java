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
package io.github.resilience4j.timelimiter;

import io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigurationProperties;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.util.Toggleable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Configuration for time limiter registry.
 *
 */
@ConfigurationProperties("resilience4j.timelimiter")
public class TimeLimiterProperties extends TimeLimiterConfigurationProperties implements Toggleable {
    private boolean enabled;

    public void setConfigs(Map<String, TimeLimiterConfigurationProperties.InstanceProperties> configs) {
        this.getConfigs().putAll(configs);
    }

    public void setInstances(Map<String, TimeLimiterConfigurationProperties.InstanceProperties> instances) {
        this.getInstances().putAll(instances);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
