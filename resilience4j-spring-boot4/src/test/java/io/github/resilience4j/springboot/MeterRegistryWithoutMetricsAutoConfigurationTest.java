/*
 * Copyright 2025 Yevhenii Voievodin, Artur Havliukovskyi
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
package io.github.resilience4j.springboot;

import io.github.resilience4j.micrometer.tagged.*;
import io.github.resilience4j.springboot.service.test.TestApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = TestApplication.class)
@EnableAutoConfiguration(exclude = {MetricsAutoConfiguration.class, ErrorMvcAutoConfiguration.class})
public class MeterRegistryWithoutMetricsAutoConfigurationTest {

	@Autowired(required = false)
	TaggedCircuitBreakerMetricsPublisher taggedCircuitBreakerMetricsPublisher;

	@Autowired(required = false)
	TaggedBulkheadMetricsPublisher taggedBulkheadMetricsPublisher;

	@Autowired(required = false)
	TaggedThreadPoolBulkheadMetricsPublisher taggedThreadPoolBulkheadMetricsPublisher;

	@Autowired(required = false)
	TaggedRateLimiterMetricsPublisher taggedRateLimiterMetricsPublisher;

	@Autowired(required = false)
	TaggedRetryMetricsPublisher taggedRetryMetricsPublisher;

	@Test
	public void newCircuitBreakerPublisherIsBound() {
		assertThat(taggedCircuitBreakerMetricsPublisher).isNull();
	}

	@Test
	public void newBulkheadPublisherIsBound() {
		assertThat(taggedBulkheadMetricsPublisher).isNull();
	}

	@Test
	public void newThreadPoolBulkheadPublisherIsBound() {
		assertThat(taggedThreadPoolBulkheadMetricsPublisher).isNull();
	}

	@Test
	public void newRateLimiterPublisherIsBound() {
		assertThat(taggedRateLimiterMetricsPublisher).isNull();
	}

	@Test
	public void newRetryPublisherIsBound() {
		assertThat(taggedRetryMetricsPublisher).isNull();
	}

}
