/*
 *
 *  Copyright 2019 Robert Winkler, Mahmoud Romeh
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
package io.github.resilience4j.bulkhead.internal;


import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallFinishedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallPermittedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallRejectedEvent;
import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.core.lang.Nullable;

import java.util.concurrent.*;
import java.util.function.Supplier;

import static io.github.resilience4j.bulkhead.BulkheadFullException.getBulkheadFullException;
import static java.util.Objects.requireNonNull;

/**
 * A Bulkhead implementation based on a fixed ThreadPoolExecutor.
 * which is based into the thread pool execution handling :
 * 1- submit service call through bulk head thread pool
 * 2- if there is free thread from the thread pool or the queue is not yet full , it will be permitted
 * 3- otherwise the thread pool will throw RejectedExecutionException which mean is not permitted
 */
public class FixedThreadPoolBulkhead implements ThreadPoolBulkhead {

	private static final String CONFIG_MUST_NOT_BE_NULL = "Config must not be null";

	private final String name;
	private final ThreadPoolExecutor executorService;
	private final FixedThreadPoolBulkhead.BulkheadMetrics metrics;
	private final FixedThreadPoolBulkhead.BulkheadEventProcessor eventProcessor;
	private final ThreadPoolBulkheadConfig config;

	/**
	 * Creates a bulkhead using a configuration supplied
	 *
	 * @param name           the name of this bulkhead
	 * @param bulkheadConfig custom bulkhead configuration
	 */
	public FixedThreadPoolBulkhead(String name, @Nullable ThreadPoolBulkheadConfig bulkheadConfig) {
		this.name = name;
		this.config = requireNonNull(bulkheadConfig, CONFIG_MUST_NOT_BE_NULL);
		// init thread pool executor
		this.executorService = new ThreadPoolExecutor(config.getCoreThreadPoolSize(), config.getMaxThreadPoolSize(),
				config.getKeepAliveDuration().toMillis(), TimeUnit.MILLISECONDS,
				new ArrayBlockingQueue<>(config.getQueueCapacity()),
				new NamingThreadFactory(name));
		// adding prover jvm executor shutdown
		cleanup();
		this.metrics = new FixedThreadPoolBulkhead.BulkheadMetrics();
		this.eventProcessor = new FixedThreadPoolBulkhead.BulkheadEventProcessor();
	}

	/**
	 * Creates a bulkhead with a default config.
	 *
	 * @param name the name of this bulkhead
	 */
	public FixedThreadPoolBulkhead(String name) {
		this(name, ThreadPoolBulkheadConfig.ofDefaults());
	}

	/**
	 * Create a bulkhead using a configuration supplier
	 *
	 * @param name           the name of this bulkhead
	 * @param configSupplier BulkheadConfig supplier
	 */
	public FixedThreadPoolBulkhead(String name, Supplier<ThreadPoolBulkheadConfig> configSupplier) {
		this(name, configSupplier.get());
	}

	/**
	 * @param callable the callable to execute through bulk head thread pool
	 * @param <T>      the result type
	 * @return the callable returned result
	 */
	@Override
	public <T> CompletableFuture<T> submit(Callable<T> callable) {
		final CompletableFuture<T> promise = new CompletableFuture<>();
		try {
			CompletableFuture.supplyAsync(() -> {
				try {
					publishBulkheadEvent(() -> new BulkheadOnCallPermittedEvent(name));
					return callable.call();
				} catch (Exception e) {
					throw new CompletionException(e);
				}
			}, executorService).whenComplete((result, throwable) -> {
				publishBulkheadEvent(() -> new BulkheadOnCallFinishedEvent(name));
				if (throwable != null) {
					promise.completeExceptionally(throwable);
				} else {
					promise.complete(result);
				}
			});
		} catch (RejectedExecutionException rejected) {
			publishBulkheadEvent(() -> new BulkheadOnCallRejectedEvent(name));
			throw getBulkheadFullException(this);
		}
		return promise;
	}

	/**
	 * @param runnable the runnable to execute through bulk head thread pool
	 */
	@Override
	public void submit(Runnable runnable) {
		try {
			CompletableFuture.runAsync(() -> {
				try {
					publishBulkheadEvent(() -> new BulkheadOnCallPermittedEvent(name));
					runnable.run();
				} catch (Exception e) {
					throw new CompletionException(e);
				}
			}, executorService).whenComplete((voidResult, throwable) -> publishBulkheadEvent(() -> new BulkheadOnCallFinishedEvent(name)));
		} catch (RejectedExecutionException rejected) {
			publishBulkheadEvent(() -> new BulkheadOnCallRejectedEvent(name));
			throw getBulkheadFullException(this);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return this.name;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ThreadPoolBulkheadConfig getBulkheadConfig() {
		return config;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Metrics getMetrics() {
		return metrics;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ThreadPoolBulkheadEventPublisher getEventPublisher() {
		return eventProcessor;
	}

	private void publishBulkheadEvent(Supplier<BulkheadEvent> eventSupplier) {
		if (eventProcessor.hasConsumers()) {
			eventProcessor.consumeEvent(eventSupplier.get());
		}
	}

	@Override
	public String toString() {
		return String.format("FixedThreadPoolBulkhead '%s'", this.name);
	}

	private void cleanup() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			executorService.shutdown();
			try {
				if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
					executorService.shutdownNow();
				}
			} catch (InterruptedException e) {
				if (!executorService.isTerminated()) {
					executorService.shutdownNow();
				}
				Thread.currentThread().interrupt();
			}
		}));
	}

	private class BulkheadEventProcessor extends EventProcessor<BulkheadEvent> implements ThreadPoolBulkheadEventPublisher, EventConsumer<BulkheadEvent> {

		@Override
		public ThreadPoolBulkheadEventPublisher onCallPermitted(EventConsumer<BulkheadOnCallPermittedEvent> onCallPermittedEventConsumer) {
			registerConsumer(BulkheadOnCallPermittedEvent.class.getSimpleName(), onCallPermittedEventConsumer);
			return this;
		}

		@Override
		public ThreadPoolBulkheadEventPublisher onCallRejected(EventConsumer<BulkheadOnCallRejectedEvent> onCallRejectedEventConsumer) {
			registerConsumer(BulkheadOnCallRejectedEvent.class.getSimpleName(), onCallRejectedEventConsumer);
			return this;
		}

		@Override
		public ThreadPoolBulkheadEventPublisher onCallFinished(EventConsumer<BulkheadOnCallFinishedEvent> onCallFinishedEventConsumer) {
			registerConsumer(BulkheadOnCallFinishedEvent.class.getSimpleName(), onCallFinishedEventConsumer);
			return this;
		}

		@Override
		public void consumeEvent(BulkheadEvent event) {
			super.processEvent(event);
		}
	}

	/**
	 * the thread pool bulk head metrics
	 */
	private final class BulkheadMetrics implements Metrics {
		private BulkheadMetrics() {
		}

		@Override
		public int getCoreThreadPoolSize() {
			return executorService.getCorePoolSize();
		}

		@Override
		public int getThreadPoolSize() {
			return executorService.getPoolSize();
		}

		@Override
		public int getMaximumThreadPoolSize() {
			return executorService.getMaximumPoolSize();
		}

		@Override
		public int getQueueDepth() {
			return executorService.getQueue().size();
		}

		@Override
		public int getRemainingQueueCapacity() {
			return executorService.getQueue().remainingCapacity();
		}

		@Override
		public int getQueueCapacity() {
			return config.getQueueCapacity();
		}
	}
}
