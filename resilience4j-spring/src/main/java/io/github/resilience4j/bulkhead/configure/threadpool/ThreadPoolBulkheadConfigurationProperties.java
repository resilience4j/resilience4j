/*
 * Copyright 2019 lespinsideg
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
package io.github.resilience4j.bulkhead.configure.threadpool;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.Min;

import org.springframework.util.StringUtils;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.lang.Nullable;

public class ThreadPoolBulkheadConfigurationProperties {

	private Map<String, BackendProperties> backends = new HashMap<>();
	private Map<String, BackendProperties> configs = new HashMap<>();

	public Map<String, BackendProperties> getBackends() {
		return backends;
	}

	public Map<String, BackendProperties> getConfigs() {
		return configs;
	}

	@Nullable
	public BackendProperties getBackendProperties(String backend) {
		return backends.get(backend);
	}

	// Thread pool bulkhead section
	public ThreadPoolBulkheadConfig createThreadPoolBulkheadConfig(String backend) {
		return createThreadPoolBulkheadConfig(getBackendProperties(backend));
	}

	public ThreadPoolBulkheadConfig createThreadPoolBulkheadConfig(BackendProperties backendProperties) {
		if (!StringUtils.isEmpty(backendProperties.getBaseConfig())) {
			BackendProperties baseProperties = configs.get(backendProperties.getBaseConfig());
			if (baseProperties == null) {
				throw new ConfigurationNotFoundException(backendProperties.getBaseConfig());
			}
			return buildThreadPoolConfigFromBaseConfig(baseProperties, backendProperties);
		}
		return buildThreadPoolBulkheadConfig(ThreadPoolBulkheadConfig.custom(), backendProperties);
	}

	private ThreadPoolBulkheadConfig buildThreadPoolConfigFromBaseConfig(BackendProperties baseProperties, BackendProperties backendProperties) {
		ThreadPoolBulkheadConfig baseConfig = buildThreadPoolBulkheadConfig(ThreadPoolBulkheadConfig.custom(), baseProperties);
		return buildThreadPoolBulkheadConfig(ThreadPoolBulkheadConfig.from(baseConfig), backendProperties);
	}

	public ThreadPoolBulkheadConfig buildThreadPoolBulkheadConfig(ThreadPoolBulkheadConfig.Builder builder, BackendProperties properties) {
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
		}
		return builder.build();
	}


	/**
	 * Class storing property values for configuring {@link Bulkhead} instances.
	 */
	public static class BackendProperties {

		@Min(1)
		private Integer eventConsumerBufferSize = 100;

		@Nullable
		private String baseConfig;


		@Nullable
		private ThreadPoolProperties threadPoolProperties;


		@Nullable
		public ThreadPoolProperties getThreadPoolProperties() {
			return threadPoolProperties;
		}

		public void setThreadPoolProperties(@Nullable ThreadPoolProperties threadPoolProperties) {
			this.threadPoolProperties = threadPoolProperties;
		}

		public Integer getEventConsumerBufferSize() {
			return eventConsumerBufferSize;
		}

		public void setEventConsumerBufferSize(Integer eventConsumerBufferSize) {
			this.eventConsumerBufferSize = eventConsumerBufferSize;
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
