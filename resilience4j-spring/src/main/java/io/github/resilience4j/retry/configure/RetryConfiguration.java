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
package io.github.resilience4j.retry.configure;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.retry.AsyncRetry;
import io.github.resilience4j.retry.AsyncRetryRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.event.RetryEvent;
import io.github.resilience4j.retry.internal.InMemoryAsyncRetryRegistry;
import io.github.resilience4j.retry.internal.InMemoryRetryRegistry;

/**
 * {@link Configuration
 * Configuration} for resilience4j-retry.
 */
@Configuration
public class RetryConfiguration {

	/**
	 * @param retryConfigurationProperties retryConfigurationProperties retry configuration spring properties
	 * @param retryEventConsumerRegistry   the event retry registry
	 * @return the retry definition registry
	 */
	@Bean
	public RetryRegistry retryRegistry(RetryConfigurationProperties retryConfigurationProperties, @Qualifier("retryEventConsumerRegistry") EventConsumerRegistry<RetryEvent> retryEventConsumerRegistry) {
		RetryRegistry retryRegistry = new InMemoryRetryRegistry();
		retryConfigurationProperties.getBackends().forEach(
				(name, properties) -> {
					RetryConfig retryConfig = retryConfigurationProperties.createRetryConfig(name);
					Retry retry = retryRegistry.retry(name, retryConfig);
					retry.getEventPublisher().onEvent(retryEventConsumerRegistry.createEventConsumer(name, properties.getEventConsumerBufferSize()));
				}
		);
		return retryRegistry;
	}

	/**
	 * @param retryConfigurationProperties retryConfigurationProperties retry configuration spring properties
	 * @param retryEventConsumerRegistry   the event retry registry
	 * @return the async retry definition registry
	 */
	@Bean
	public AsyncRetryRegistry asyncRetryRegistry(RetryConfigurationProperties retryConfigurationProperties, @Qualifier("asyncRetryEventConsumerRegistry") EventConsumerRegistry<RetryEvent> retryEventConsumerRegistry) {
		AsyncRetryRegistry retryRegistry = new InMemoryAsyncRetryRegistry();
		retryConfigurationProperties.getBackends().forEach(
				(name, properties) -> {
					RetryConfig retryConfig = retryConfigurationProperties.createRetryConfig(name);
					AsyncRetry retry = retryRegistry.retry(name, retryConfig);
					retry.getEventPublisher().onEvent(retryEventConsumerRegistry.createEventConsumer(name, properties.getEventConsumerBufferSize()));
				}
		);
		return retryRegistry;
	}


	/**
	 * @param retryConfigurationProperties retry configuration spring properties
	 * @param asyncRetryRegistry           async retry in memory registry
	 * @return the spring retry AOP aspect
	 */
	@Bean
	public AsyncRetryAspect asyncRetryAspect(RetryConfigurationProperties retryConfigurationProperties,
	                                         AsyncRetryRegistry asyncRetryRegistry) {
		return new AsyncRetryAspect(retryConfigurationProperties, asyncRetryRegistry);
	}


	/**
	 * @param retryConfigurationProperties retry configuration spring properties
	 * @param retryRegistry                retry in memory registry
	 * @return the spring retry AOP aspect
	 */
	@Bean
	public RetryAspect retryAspect(RetryConfigurationProperties retryConfigurationProperties,
	                               RetryRegistry retryRegistry) {
		return new RetryAspect(retryConfigurationProperties, retryRegistry);
	}

	/**
	 * The EventConsumerRegistry is used to manage EventConsumer instances.
	 * The EventConsumerRegistry is used by the Retry events monitor to show the latest RetryEvent events
	 * for each Retry instance.
	 *
	 * @return a default EventConsumerRegistry {@link DefaultEventConsumerRegistry}
	 */
	@Bean
	@Qualifier("syncRetryEventConsumerRegistry")
	public EventConsumerRegistry<RetryEvent> retryEventConsumerRegistry() {
		return new DefaultEventConsumerRegistry<>();
	}

	/**
	 * The EventConsumerRegistry is used to manage EventConsumer instances.
	 * The EventConsumerRegistry is used by the Retry events monitor to show the latest Async RetryEvent events
	 * for each async Retry instance.
	 *
	 * @return a default EventConsumerRegistry {@link DefaultEventConsumerRegistry}
	 */
	@Bean
	@Qualifier("asyncRetryEventConsumerRegistry")
	public EventConsumerRegistry<RetryEvent> asyncRetryEventConsumerRegistry() {
		return new DefaultEventConsumerRegistry<>();
	}
}
