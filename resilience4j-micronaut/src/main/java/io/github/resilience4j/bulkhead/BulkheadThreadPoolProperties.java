package io.github.resilience4j.bulkhead;

import io.github.resilience4j.common.bulkhead.configuration.ThreadPoolBulkheadConfigurationProperties;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Factory;
import io.micronaut.core.util.Toggleable;

import java.util.Map;

@Factory
@ConfigurationProperties("resilience4j.thread-pool-bulkhead")
public class BulkheadThreadPoolProperties extends ThreadPoolBulkheadConfigurationProperties implements Toggleable {
    private boolean enabled;

    public void setConfigs(Map<String, ThreadPoolBulkheadConfigurationProperties.InstanceProperties> configs) {
        this.getConfigs().putAll(configs);

    }

    public void setInstances(Map<String, ThreadPoolBulkheadConfigurationProperties.InstanceProperties> instances) {
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
