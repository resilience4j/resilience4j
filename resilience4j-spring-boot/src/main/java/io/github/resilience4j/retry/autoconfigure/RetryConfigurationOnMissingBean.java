package io.github.resilience4j.retry.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.configure.RetryAspect;
import io.github.resilience4j.retry.configure.RetryConfiguration;
import io.github.resilience4j.retry.configure.RetryConfigurationProperties;
import io.github.resilience4j.retry.event.RetryEvent;

/**
 * {@link Configuration
 * Configuration} for resilience4j-retry.
 */
@Configuration
public class RetryConfigurationOnMissingBean {

	private final RetryConfiguration retryConfiguration;

	public RetryConfigurationOnMissingBean(RetryConfiguration retryConfiguration) {
		this.retryConfiguration = retryConfiguration;
	}

	/**
	 * @param retryConfigurationProperties retryConfigurationProperties retry configuration spring properties
	 * @param retryEventConsumerRegistry   the event retry registry
	 * @return the retry definition registry
	 */
	@Bean
	@ConditionalOnMissingBean
	public RetryRegistry retryRegistry(RetryConfigurationProperties retryConfigurationProperties, EventConsumerRegistry<RetryEvent> retryEventConsumerRegistry) {
		return retryConfiguration.retryRegistry(retryConfigurationProperties, retryEventConsumerRegistry);
	}

	/**
	 * @param retryConfigurationProperties retry configuration spring properties
	 * @param retryRegistry                retry in memory registry
	 * @return the spring retry AOP aspect
	 */
	@Bean
	@ConditionalOnMissingBean
	public RetryAspect retryAspect(RetryConfigurationProperties retryConfigurationProperties,
	                               RetryRegistry retryRegistry) {
		return retryConfiguration.retryAspect(retryConfigurationProperties, retryRegistry);
	}

	/**
	 * The EventConsumerRegistry is used to manage EventConsumer instances.
	 * The EventConsumerRegistry is used by the Retry events monitor to show the latest RetryEvent events
	 * for each Retry instance.
	 *
	 * @return a default EventConsumerRegistry {@link DefaultEventConsumerRegistry}
	 */
	@Bean
	@ConditionalOnMissingBean
	public EventConsumerRegistry<RetryEvent> retryEventConsumerRegistry() {
		return retryConfiguration.retryEventConsumerRegistry();
	}
}
