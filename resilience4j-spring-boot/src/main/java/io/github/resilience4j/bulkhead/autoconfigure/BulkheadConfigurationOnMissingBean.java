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
package io.github.resilience4j.bulkhead.autoconfigure;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.configure.BulkheadAspect;
import io.github.resilience4j.bulkhead.configure.BulkheadAspectExt;
import io.github.resilience4j.bulkhead.configure.BulkheadConfiguration;
import io.github.resilience4j.bulkhead.configure.BulkheadConfigurationProperties;
import io.github.resilience4j.bulkhead.configure.ReactorBulkheadAspectExt;
import io.github.resilience4j.bulkhead.configure.RxJava2BulkheadAspectExt;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.utils.ReactorOnClasspathCondition;
import io.github.resilience4j.utils.RxJava2OnClasspathCondition;

/**
 * {@link Configuration
 * Configuration} for resilience4j-bulkhead.
 */
@Configuration
public class BulkheadConfigurationOnMissingBean {

	private final BulkheadConfiguration bulkheadConfiguration;

	public BulkheadConfigurationOnMissingBean() {
		this.bulkheadConfiguration = new BulkheadConfiguration();
	}

	@Bean
	@ConditionalOnMissingBean
	public BulkheadRegistry bulkheadRegistry(BulkheadConfigurationProperties bulkheadConfigurationProperties,
	                                         EventConsumerRegistry<BulkheadEvent> bulkheadEventConsumerRegistry) {
		return bulkheadConfiguration.bulkheadRegistry(bulkheadConfigurationProperties, bulkheadEventConsumerRegistry);
	}

	@Bean
	@ConditionalOnMissingBean
	public BulkheadAspect bulkheadAspect(BulkheadConfigurationProperties bulkheadConfigurationProperties,
	                                     BulkheadRegistry bulkheadRegistry, @Autowired(required = false) List<BulkheadAspectExt> bulkHeadAspectExtList) {
		return bulkheadConfiguration.bulkheadAspect(bulkheadConfigurationProperties, bulkheadRegistry, bulkHeadAspectExtList);
	}

	@Bean
	@Conditional(value = {RxJava2OnClasspathCondition.class})
	@ConditionalOnMissingBean
	public RxJava2BulkheadAspectExt rxJava2BulkHeadAspectExt() {
		return bulkheadConfiguration.rxJava2BulkHeadAspectExt();
	}

	@Bean
	@Conditional(value = {ReactorOnClasspathCondition.class})
	@ConditionalOnMissingBean
	public ReactorBulkheadAspectExt reactorBulkHeadAspectExt() {
		return bulkheadConfiguration.reactorBulkHeadAspectExt();
	}

	/**
	 * The EventConsumerRegistry is used to manage EventConsumer instances.
	 * The EventConsumerRegistry is used by the BulkheadHealthIndicator to show the latest Bulkhead events
	 * for each Bulkhead instance.
	 *
	 * @return a default EventConsumerRegistry {@link DefaultEventConsumerRegistry}
	 */
	@Bean
	public EventConsumerRegistry<BulkheadEvent> bulkheadEventConsumerRegistry() {
		return bulkheadConfiguration.bulkheadEventConsumerRegistry();
	}
}
