/*
 * Copyright 2020 Michael Pollind
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
package io.github.resilience4j.micronaut.bulkhead;

import io.github.resilience4j.common.bulkhead.configuration.CommonThreadPoolBulkheadConfigurationProperties;
import io.micronaut.context.annotation.*;
import io.micronaut.core.naming.Named;
import io.micronaut.core.util.Toggleable;

import java.util.List;

/**
 * Configures thread pool bulkhead instances and configs.
 *
 * @author gkrocher
 * @author Michael Pollend
 */
@ConfigurationProperties("resilience4j.thread-pool-bulkhead")
public class ThreadPoolBulkheadProperties extends CommonThreadPoolBulkheadConfigurationProperties implements Toggleable {
    private boolean enabled;

    public ThreadPoolBulkheadProperties(
        List<InstancePropertiesConfigs> configs,
        List<InstancePropertiesInstances> instances) {
        for (InstancePropertiesConfigs config : configs) {
            this.getConfigs().put(config.getName(), config);
        }
        for (InstancePropertiesInstances instance : instances) {
            this.getInstances().put(instance.getName(), instance);
        }
    }

    /**
     * Sets whether the config is enabled.
     * @param enabled True if is enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @EachProperty(value = "configs", primary = "default")
    public static class InstancePropertiesConfigs extends CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties implements Named {
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
    public static class InstancePropertiesInstances extends CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties implements Named {
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
