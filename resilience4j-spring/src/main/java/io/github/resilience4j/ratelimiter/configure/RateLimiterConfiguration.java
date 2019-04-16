/*
 * Copyright 2017 Bohdan Storozhuk
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
package io.github.resilience4j.ratelimiter.configure;

import static io.github.resilience4j.ratelimiter.configure.RateLimiterConfigurationProperties.createRateLimiterConfig;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import io.github.resilience4j.utils.ReactorOnClasspathCondition;
import io.github.resilience4j.utils.RxJava2OnClasspathCondition;

/**
 * {@link org.springframework.context.annotation.Configuration
 * Configuration} for resilience4j ratelimiter.
 */
@Configuration
public class RateLimiterConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(RateLimiterConfiguration.class);

	@Bean
	public RateLimiterRegistry rateLimiterRegistry(RateLimiterConfigurationProperties rateLimiterProperties,
	                                               EventConsumerRegistry<RateLimiterEvent> rateLimiterEventsConsumerRegistry,
	                                               ConfigurableBeanFactory beanFactory) {
		RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.of(RateLimiterConfig.ofDefaults());
		rateLimiterProperties.getLimiters().forEach(
				(name, properties) -> {
					RateLimiter rateLimiter = createRateLimiter(rateLimiterRegistry, name, properties);
					if (properties.getSubscribeForEvents()) {
						subscribeToLimiterEvents(rateLimiterEventsConsumerRegistry, name, properties, rateLimiter);
					}
				}
		);
		return rateLimiterRegistry;
	}

	@Bean
	public RateLimiterAspect rateLimiterAspect(RateLimiterConfigurationProperties rateLimiterProperties, RateLimiterRegistry rateLimiterRegistry, @Autowired(required = false) List<RateLimiterAspectExt> rateLimiterAspectExtList) {
		return new RateLimiterAspect(rateLimiterRegistry, rateLimiterProperties, rateLimiterAspectExtList);
	}

	@Bean
	@Conditional(value = {RxJava2OnClasspathCondition.class})
	public RxJava2RateLimiterAspectExt rxJava2RateLimterAspectExt() {
		return new RxJava2RateLimiterAspectExt();
	}

	@Bean
	@Conditional(value = {ReactorOnClasspathCondition.class})
	public ReactorRateLimiterAspectExt reactorRateLimiterAspectExt() {
		return new ReactorRateLimiterAspectExt();
	}

	/**
	 * The EventConsumerRegistry is used to manage EventConsumer instances.
	 * The EventConsumerRegistry is used by the RateLimiterHealthIndicator to show the latest RateLimiterEvents events
	 * for each RateLimiter instance.
     *
     * @return The EventConsumerRegistry of RateLimiterEvent bean.
	 */
	@Bean
	public EventConsumerRegistry<RateLimiterEvent> rateLimiterEventsConsumerRegistry() {
		return new DefaultEventConsumerRegistry<>();
	}

	private void subscribeToLimiterEvents(EventConsumerRegistry<RateLimiterEvent> rateLimiterEventsConsumerRegistry, String name, RateLimiterConfigurationProperties.LimiterProperties properties, RateLimiter rateLimiter) {
		EventConsumer<RateLimiterEvent> eventConsumer = rateLimiterEventsConsumerRegistry
				.createEventConsumer(name, properties.getEventConsumerBufferSize());
		rateLimiter.getEventPublisher().onEvent(eventConsumer);

		logger.debug("Autoconfigure subscription for Rate Limiter {}", rateLimiter);
	}

	private RateLimiter createRateLimiter(RateLimiterRegistry rateLimiterRegistry, String name, RateLimiterConfigurationProperties.LimiterProperties properties) {
		RateLimiter rateLimiter =
				rateLimiterRegistry.rateLimiter(name, createRateLimiterConfig(properties));
		logger.debug("Autoconfigure Rate Limiter registered. {}", rateLimiter);
		return rateLimiter;
	}
}
