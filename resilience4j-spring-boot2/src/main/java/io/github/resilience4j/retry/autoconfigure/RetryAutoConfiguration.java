/*
 * Copyright 2019 Mahmoud Romeh
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
package io.github.resilience4j.retry.autoconfigure;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.retry.AsyncRetry;
import io.github.resilience4j.retry.AsyncRetryRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.event.RetryEvent;
import io.github.resilience4j.retry.monitoring.endpoint.RetryEndpoint;
import io.github.resilience4j.retry.monitoring.endpoint.RetryEventsEndpoint;
import io.github.resilience4j.retry.monitoring.health.AsyncRetryHealthIndicator;
import io.github.resilience4j.retry.monitoring.health.RetryHealthIndicator;


/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} for resilience4j-retry.
 */
@Configuration
@ConditionalOnClass(Retry.class)
@EnableConfigurationProperties(RetryProperties.class)
@Import(RetryConfigurationOnMissingBean.class)
public class RetryAutoConfiguration {

	private final RetryProperties retryProperties;
	private final RetryRegistry retryRegistry;
	private final AsyncRetryRegistry asyncRetryRegistry;
	private final ConfigurableBeanFactory beanFactory;

	public RetryAutoConfiguration(RetryProperties retryProperties, RetryRegistry retryRegistry, AsyncRetryRegistry asyncRetryRegistry, ConfigurableBeanFactory beanFactory) {
		this.retryProperties = retryProperties;
		this.retryRegistry = retryRegistry;
		this.asyncRetryRegistry = asyncRetryRegistry;
		this.beanFactory = beanFactory;
	}

	@Bean
	@ConditionalOnEnabledEndpoint
	public RetryEndpoint retryEndpoint(RetryRegistry retryRegistry, AsyncRetryRegistry asyncRetryRegistry) {
		return new RetryEndpoint(retryRegistry, asyncRetryRegistry);
	}

	@Bean
	@ConditionalOnEnabledEndpoint
	public RetryEventsEndpoint retryEventsEndpoint(@Qualifier("retryEventConsumerRegistry") EventConsumerRegistry<RetryEvent> eventConsumerRegistry,
	                                               @Qualifier("asyncRetryEventConsumerRegistry") EventConsumerRegistry<RetryEvent> asyncRetryEventConsumerRegistry) {
		return new RetryEventsEndpoint(eventConsumerRegistry, asyncRetryEventConsumerRegistry);
	}

	@PostConstruct
	public void configureRegistryWithHealthEndpoint() {
		retryProperties.getBackends().forEach(
				(name, properties) -> {
					if (properties.getRegisterHealthIndicator()) {
						createHeathIndicatorForRetry(name);
					}
				}
		);
	}

	private void createHeathIndicatorForRetry(String name) {
		Retry retry = retryRegistry.retry(name);
		AsyncRetry asyncRetry = asyncRetryRegistry.retry(name);
		RetryHealthIndicator healthIndicator = new RetryHealthIndicator(retry);
		AsyncRetryHealthIndicator asyncHealthIndicator = new AsyncRetryHealthIndicator(asyncRetry);
		beanFactory.registerSingleton(
				name + "RetryHealthIndicator",
				healthIndicator
		);
		beanFactory.registerSingleton(
				name + "AsyncRetryHealthIndicator",
				asyncHealthIndicator
		);
	}

}
