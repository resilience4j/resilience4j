/*
 * Copyright 2017 Dan Maas
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
package io.github.resilience4j.retry;

import io.github.resilience4j.core.ConfigurationNotFoundException;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;


public class RetryRegistryTest {

	private RetryRegistry retryRegistry;
	private Logger LOGGER;
	private Consumer<Retry> post_consumer = circuitBreaker -> LOGGER.info("invoking the post consumer1");

	@Before
	public void setUp() {
		LOGGER = mock(Logger.class);
		retryRegistry = RetryRegistry.ofDefaults();
		retryRegistry.registerPostCreationConsumer(post_consumer);
	}

	@Test
	public void testCreateWithNullConfig() {
		assertThatThrownBy(() -> RetryRegistry.of((RetryConfig) null)).isInstanceOf(NullPointerException.class).hasMessage("Config must not be null");
	}

	@Test
	public void shouldReturnTheCorrectName() {
		Retry retry = retryRegistry.retry("testName");
		Assertions.assertThat(retry).isNotNull();
		Assertions.assertThat(retry.getName()).isEqualTo("testName");
		BDDMockito.then(LOGGER).should(times(1)).info("invoking the post consumer1");
	}

	@Test
	public void shouldBeTheSameRetry() {
		Retry retry = retryRegistry.retry("testName");
		Retry retry2 = retryRegistry.retry("testName");
		Assertions.assertThat(retry).isSameAs(retry2);
		Assertions.assertThat(retryRegistry.getAllRetries()).hasSize(1);
		BDDMockito.then(LOGGER).should(times(1)).info("invoking the post consumer1");
	}

	@Test
	public void shouldBeNotTheSameRetry() {

		Retry retry = retryRegistry.retry("testName");
		Retry retry2 = retryRegistry.retry("otherTestName");
		Assertions.assertThat(retry).isNotSameAs(retry2);
		Assertions.assertThat(retryRegistry.getAllRetries()).hasSize(2);
		BDDMockito.then(LOGGER).should(times(2)).info("invoking the post consumer1");
	}

	@Test
	public void canBuildRetryFromRegistryWithConfig() {
		RetryConfig config = RetryConfig.custom().maxAttempts(1000).waitDuration(Duration.ofSeconds(300)).build();
		Retry retry = retryRegistry.retry("testName", config);
		Assertions.assertThat(retry).isNotNull();
		Assertions.assertThat(retryRegistry.getAllRetries()).hasSize(1);
		BDDMockito.then(LOGGER).should(times(1)).info("invoking the post consumer1");
	}

	@Test
	public void canBuildRetryFromRegistryWithConfigSupplier() {
		RetryConfig config = RetryConfig.custom().maxAttempts(1000).waitDuration(Duration.ofSeconds(300)).build();
		Retry retry = retryRegistry.retry("testName", () -> config);
		Assertions.assertThat(retry).isNotNull();
		Assertions.assertThat(retryRegistry.getAllRetries()).hasSize(1);
		BDDMockito.then(LOGGER).should(times(1)).info("invoking the post consumer1");
	}

	@Test
	public void canBuildRetryRegistryWithConfig() {
		RetryConfig config = RetryConfig.custom().maxAttempts(1000).waitDuration(Duration.ofSeconds(300)).build();
		retryRegistry = RetryRegistry.of(config);
		retryRegistry.registerPostCreationConsumer(post_consumer);
		Retry retry = retryRegistry.retry("testName", () -> config);
		Assertions.assertThat(retry).isNotNull();
		Assertions.assertThat(retryRegistry.getAllRetries()).hasSize(1);
		BDDMockito.then(LOGGER).should(times(1)).info("invoking the post consumer1");
	}

	@Test
	public void testCreateWithConfigurationMap() {
		Map<String, RetryConfig> configs = new HashMap<>();
		configs.put("default", RetryConfig.ofDefaults());
		configs.put("custom", RetryConfig.ofDefaults());

		RetryRegistry retryRegistry = RetryRegistry.of(configs);

		assertThat(retryRegistry.getDefaultConfig()).isNotNull();
		assertThat(retryRegistry.getConfiguration("custom")).isNotNull();
	}

	@Test
	public void testCreateWithConfigurationMapWithoutDefaultConfig() {
		Map<String, RetryConfig> configs = new HashMap<>();
		configs.put("custom", RetryConfig.ofDefaults());

		RetryRegistry retryRegistry = RetryRegistry.of(configs);

		assertThat(retryRegistry.getDefaultConfig()).isNotNull();
		assertThat(retryRegistry.getConfiguration("custom")).isNotNull();
	}

	@Test
	public void testWithNotExistingConfig() {
		RetryRegistry retryRegistry = RetryRegistry.ofDefaults();

		assertThatThrownBy(() -> retryRegistry.retry("test", "doesNotExist"))
				.isInstanceOf(ConfigurationNotFoundException.class);
	}

	@Test
	public void testAddConfiguration() {
		RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
		retryRegistry.addConfiguration("custom", RetryConfig.custom().build());

		assertThat(retryRegistry.getDefaultConfig()).isNotNull();
		assertThat(retryRegistry.getConfiguration("custom")).isNotNull();
	}
}
