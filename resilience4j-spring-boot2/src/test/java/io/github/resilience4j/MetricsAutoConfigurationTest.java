/*
 * Copyright 2019 Yevhenii Voievodin
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
package io.github.resilience4j;

import io.github.resilience4j.micrometer.tagged.*;
import io.github.resilience4j.service.test.TestApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = TestApplication.class)
public class MetricsAutoConfigurationTest {

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
		assertThat(taggedCircuitBreakerMetricsPublisher).isNotNull();
	}

	@Test
	public void newBulkheadPublisherIsBound() {
		assertThat(taggedBulkheadMetricsPublisher).isNotNull();
	}

	@Test
	public void newThreadPoolBulkheadPublisherIsBound() {
		assertThat(taggedThreadPoolBulkheadMetricsPublisher).isNotNull();
	}

	@Test
	public void newRateLimiterPublisherIsBound() {
		assertThat(taggedRateLimiterMetricsPublisher).isNotNull();
	}

	@Test
	public void newRetryPublisherIsBound() {
		assertThat(taggedRetryMetricsPublisher).isNotNull();
	}

}
