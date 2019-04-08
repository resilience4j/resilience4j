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
import static org.mockito.Mockito.times;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;

import io.github.resilience4j.test.HelloWorldService;

public class ThreadPoolBulkheadTest {

	private HelloWorldService helloWorldService;
	private ThreadPoolBulkheadConfig config;

	@Before
	public void setUp() {
		Awaitility.reset();
		helloWorldService = Mockito.mock(HelloWorldService.class);
		config = ThreadPoolBulkheadConfig.custom()
				.maxThreadPoolSize(1)
				.coreThreadPoolSize(1)
				.queueCapacity(1)
				.build();
	}

	@Test
	public void shouldExecuteSupplierAndFailWithBulkHeadFull() throws InterruptedException {

		// Given
		ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("testSupplier", config);

		BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
		final Exception exception = new Exception();
		// When
		new Thread(() -> {
			try {
				final AtomicInteger counter = new AtomicInteger(0);
				bulkhead.executeRunnable(() -> {
					Awaitility.waitAtMost(Duration.TWO_HUNDRED_MILLISECONDS).until(() -> counter.incrementAndGet() > 1);
				});
			} catch (Exception e) {
				exception.initCause(e);
			}
		}).start();
		new Thread(() -> {
			try {
				bulkhead.executeSupplier(helloWorldService::returnHelloWorld);
			} catch (Exception e) {
				exception.initCause(e);
			}
		}).start();
		new Thread(() -> {
			try {
				bulkhead.executeSupplier(helloWorldService::returnHelloWorld);
			} catch (Exception e) {
				exception.initCause(e);
			}
		}).start();
		final AtomicInteger counter = new AtomicInteger(0);
		Awaitility.waitAtMost(Duration.FIVE_HUNDRED_MILLISECONDS).until(() -> counter.incrementAndGet() >= 2);
		// Then
		assertThat(exception.getCause().getMessage()).contains("ThreadPoolBulkhead 'testSupplier' is full");
	}


	@Test
	public void shouldExecuteCallableAndFailWithBulkHeadFull() throws InterruptedException {

		// Given
		ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("test", config);

		BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
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
	}


	@Test
	public void shouldExecuteSupplierAndReturnWithSuccess() throws ExecutionException, InterruptedException {

		// Given
		ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("test", config);

		BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

		// When
		CompletionStage<String> result = bulkhead.executeSupplier(helloWorldService::returnHelloWorld);


		// Then
		assertThat(result.toCompletableFuture().get()).isEqualTo("Hello world");
		BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorld();
	}


}
