/*
 *
 *  Copyright 2017 Robert Winkler, Lucas Lech, Mahmoud Romeh
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.bulkhead;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.slf4j.Logger;

import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;

import io.github.resilience4j.test.HelloWorldService;

public class ThreadPoolBulkheadEventPublisherTest {

	private HelloWorldService helloWorldService;
	private ThreadPoolBulkheadConfig config;
	private Logger logger;
	private ThreadPoolBulkhead bulkhead;

	@Before
	public void setUp() {
		helloWorldService = mock(HelloWorldService.class);
		config = ThreadPoolBulkheadConfig.custom()
				.maxThreadPoolSize(1)
				.coreThreadPoolSize(1)
				.build();

		bulkhead = ThreadPoolBulkhead.of("test", config);

		logger = mock(Logger.class);
		Awaitility.reset();
	}

	@Test
	public void shouldReturnTheSameConsumer() {
		ThreadPoolBulkhead.ThreadPoolBulkheadEventPublisher eventPublisher = bulkhead.getEventPublisher();
		ThreadPoolBulkhead.ThreadPoolBulkheadEventPublisher eventPublisher2 = bulkhead.getEventPublisher();

		assertThat(eventPublisher).isEqualTo(eventPublisher2);
	}

	@Test
	public void shouldConsumeOnCallRejectedEvent() throws ExecutionException, InterruptedException {
		// Given
		ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("test", ThreadPoolBulkheadConfig.custom()
				.maxThreadPoolSize(1)
				.coreThreadPoolSize(1)
				.queueCapacity(1)
				.build());

		BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

		// When
		bulkhead.getEventPublisher()
				.onCallRejected(event ->
						logger.info(event.getEventType().toString()));
		final Exception exception = new Exception();
		// When
		new Thread(() -> {
			try {
				bulkhead.executeRunnable(() -> {
					final AtomicInteger counter = new AtomicInteger(0);
					Awaitility.waitAtMost(Duration.TWO_HUNDRED_MILLISECONDS).until(() -> counter.incrementAndGet() >= 2);
				});
			} catch (Exception e) {
				exception.initCause(e);
			}

		}).start();
		new Thread(() -> {
			try {
				bulkhead.executeCallable(helloWorldService::returnHelloWorld);
			} catch (Exception e) {
				exception.initCause(e);
			}
		}).start();
		new Thread(() -> {
			try {
				bulkhead.executeCallable(helloWorldService::returnHelloWorld);
			} catch (Exception e) {
				exception.initCause(e);
			}
		}).start();

		final AtomicInteger counter = new AtomicInteger(0);
		Awaitility.waitAtMost(Duration.FIVE_HUNDRED_MILLISECONDS).until(() -> counter.incrementAndGet() >= 2);
		// Then
		assertThat(exception).hasCauseInstanceOf(BulkheadFullException.class);
		then(logger).should(times(1)).info("CALL_REJECTED");
	}

	@Test
	public void shouldConsumeOnCallPermittedEvent() throws ExecutionException, InterruptedException {
		// Given
		ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("test", config);
		BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

		// When
		bulkhead.getEventPublisher()
				.onCallPermitted(event ->
						logger.info(event.getEventType().toString()));

		String result = bulkhead.executeSupplier(helloWorldService::returnHelloWorld).toCompletableFuture().get();

		// Then
		assertThat(result).isEqualTo("Hello world");
		then(logger).should(times(1)).info("CALL_PERMITTED");
	}

	@Test
	public void shouldConsumeOnCallFinishedEventWhenExecutionIsFinished() throws Exception {
		// Given
		ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("test", config);
		BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
		// When
		bulkhead.getEventPublisher()
				.onCallFinished(event ->
						logger.info(event.getEventType().toString()));
		bulkhead.executeSupplier(helloWorldService::returnHelloWorld).toCompletableFuture().get();
		// Then
		then(logger).should(times(1)).info("CALL_FINISHED");
	}
}
