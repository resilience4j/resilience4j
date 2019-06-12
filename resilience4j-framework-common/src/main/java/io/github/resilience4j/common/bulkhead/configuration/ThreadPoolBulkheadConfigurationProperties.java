/*
 * Copyright 2019 Dan Maas
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
package io.github.resilience4j.common.bulkhead.configuration;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.Min;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.StringUtils;
import io.github.resilience4j.core.lang.Nullable;

public class ThreadPoolBulkheadConfigurationProperties {

	private Map<String, InstanceProperties> instances = new HashMap<>();
	private Map<String, InstanceProperties> configs = new HashMap<>();

	public Map<String, InstanceProperties> getInstances() {
		return instances;
	}

	/**
	 * For backwards compatibility when setting backends in configuration properties.
	 */
	public Map<String, InstanceProperties> getBackends() {
		return instances;
	}

	public Map<String, InstanceProperties> getConfigs() {
		return configs;
	}

	@Nullable
	public InstanceProperties getBackendProperties(String backend) {
		return instances.get(backend);
	}

	// Thread pool bulkhead section
	public ThreadPoolBulkheadConfig createThreadPoolBulkheadConfig(String backend) {
		return createThreadPoolBulkheadConfig(getBackendProperties(backend));
	}

	public ThreadPoolBulkheadConfig createThreadPoolBulkheadConfig(InstanceProperties instanceProperties) {
		if (StringUtils.isNotEmpty(instanceProperties.getBaseConfig())) {
			InstanceProperties baseProperties = configs.get(instanceProperties.getBaseConfig());
			if (baseProperties == null) {
				throw new ConfigurationNotFoundException(instanceProperties.getBaseConfig());
			}
			return buildThreadPoolConfigFromBaseConfig(baseProperties, instanceProperties);
		}
		return buildThreadPoolBulkheadConfig(ThreadPoolBulkheadConfig.custom(), instanceProperties);
	}

	private ThreadPoolBulkheadConfig buildThreadPoolConfigFromBaseConfig(InstanceProperties baseProperties, InstanceProperties instanceProperties) {
		ThreadPoolBulkheadConfig baseConfig = buildThreadPoolBulkheadConfig(ThreadPoolBulkheadConfig.custom(), baseProperties);
		return buildThreadPoolBulkheadConfig(ThreadPoolBulkheadConfig.from(baseConfig), instanceProperties);
	}

	public ThreadPoolBulkheadConfig buildThreadPoolBulkheadConfig(ThreadPoolBulkheadConfig.Builder builder, InstanceProperties properties) {
		if (properties == null) {
			return ThreadPoolBulkheadConfig.custom().build();
		}
		if (properties.getThreadPoolProperties() != null) {
			if (properties.getThreadPoolProperties().getQueueCapacity() > 0) {
				builder.queueCapacity(properties.getThreadPoolProperties().getQueueCapacity());
			}
			if (properties.getThreadPoolProperties().getCoreThreadPoolSize() > 0) {
				builder.coreThreadPoolSize(properties.getThreadPoolProperties().getCoreThreadPoolSize());
			}
			if (properties.getThreadPoolProperties().getMaxThreadPoolSize() > 0) {
				builder.maxThreadPoolSize(properties.getThreadPoolProperties().getMaxThreadPoolSize());
			}
			if (properties.getThreadPoolProperties().getKeepAliveTime() > 0) {
				builder.keepAliveTime(properties.getThreadPoolProperties().getKeepAliveTime());
			}
			if (properties.getThreadPoolProperties().getKeepAliveDuration() != null && properties.getThreadPoolProperties().getKeepAliveDuration().toMillis() > 0) {
				builder.keepAliveTime(properties.getThreadPoolProperties().getKeepAliveDuration().toMillis());
			}
		}
		return builder.build();
	}


	/**
	 * Class storing property values for configuring {@link Bulkhead} instances.
	 */
	public static class InstanceProperties {

		@Min(1)
		@Nullable
		private Integer eventConsumerBufferSize;

		@Nullable
		private String baseConfig;

		@Nullable
		private ThreadPoolProperties threadPoolProperties;

		@Nullable
		public ThreadPoolProperties getThreadPoolProperties() {
			return threadPoolProperties;
		}

		public InstanceProperties setThreadPoolProperties(@Nullable ThreadPoolProperties threadPoolProperties) {
			this.threadPoolProperties = threadPoolProperties;
			return this;
		}

		public Integer getEventConsumerBufferSize() {
			return eventConsumerBufferSize;
		}

		public InstanceProperties setEventConsumerBufferSize(Integer eventConsumerBufferSize) {
			this.eventConsumerBufferSize = eventConsumerBufferSize;
			return this;
		}

		/**
		 * Gets the shared configuration name. If this is set, the configuration builder will use the the shared
		 * configuration backend over this one.
		 *
		 * @return The shared configuration name.
		 */
		@Nullable
		public String getBaseConfig() {
			return baseConfig;
		}

		/**
		 * Sets the shared configuration name. If this is set, the configuration builder will use the the shared
		 * configuration backend over this one.
		 *
		 * @param baseConfig The shared configuration name.
		 */
		public void setBaseConfig(String baseConfig) {
			this.baseConfig = baseConfig;
		}
	}
}
