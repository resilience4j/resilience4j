/*
 *
 *  Copyright 2019: Mahmoud Romeh
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
package io.github.resilience4j.bulkhead.adaptive;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.adaptive.internal.AdaptiveLimitBulkhead;
import io.github.resilience4j.bulkhead.event.BulkheadLimit;
import io.github.resilience4j.bulkhead.event.BulkheadOnLimitDecreasedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnLimitIncreasedEvent;
import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.EventPublisher;
import io.vavr.CheckedConsumer;
import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedRunnable;

/**
 * A Bulkhead instance is thread-safe can be used to decorate multiple requests.
 * <p>
 * A {@link AdaptiveBulkhead} represent an entity limiting the amount of parallel operations. It does not assume nor does it mandate usage
 * of any particular concurrency and/or io model. These details are left for the client to manage. This bulkhead, depending on the
 * underlying concurrency/io model can be used to shed load, and, where it makes sense, limit resource use (i.e. limit amount of
 * threads/actors involved in a particular flow, etc).
 * <p>
 * In order to execute an operation protected by this bulkhead, a permission must be obtained by calling {@link AdaptiveBulkhead#tryAcquirePermission()} ()}
 * If the bulkhead is full, no additional operations will be permitted to execute until space is available.
 * <p>
 * Once the operation is complete, regardless of the result, client needs to call {@link AdaptiveBulkhead#onComplete(Duration)} ()} in order to maintain
 * integrity of internal bulkhead state which is handled as the following :
 * <p>
 * Adaptive capacity management prerequisites
 * <p>
 * You should have a system with relatively stable response latency, because we will use latency measures to adapt concurrency limits.
 * To configure you system properly you should figure out two things:
 * 2.1 Desirable average throughput per second (later X) [Example: 30 req/sec]
 * 2.2 Desirable average request latency in seconds per operation (later R) [Example: 0.1 sec/op]
 * 2.3 Maximum acceptable request latency (later Rmax). This number should be set wisely, because it can eliminate all adaptive capabilities, system will do its best to never reach such latency, so you can set it 20-30 % higher than your usual average latency.
 * <p>
 * Implementation
 * <p>
 * Resilience4j will provide new Bulkhead implementation called AdaptiveBulkhead. It will have following config params:
 * <p>
 * Desirable average throughput = X
 * Desirable request latency = R
 * Maximum acceptable request latency = Rmax (default R * 1.3)
 * Window duration for adaptation = Wa (default 5 sec)
 * Window duration for reconfiguration = Wr (default 900 sec)
 * <p>
 * From this params we will calculate:
 * <p>
 * Initial average number of concurrent requests (later N).
 * Example: N = X * R = 30 * 0.1 = 3 [op]
 * Initial max latency of current window (later cRmax).
 * Example: cRmax = min(R * 1.2, Rmax) = min(0.1 * 1.2, 0.13) = 0.12 [op]
 * Size of adaptation sliding window (later WaN)
 * Example: WaN = Wa * X = 30 * 5 = 150
 * Size of reconfiguration sliding window (later WrN)
 * Example: WrN = Wr / Wa = 900 / 5 = 180
 */
public interface AdaptiveBulkhead {


	/**
	 * Acquires a permission to execute a call, only if one is available at the time of invocation.
	 *
	 * @return {@code true} if a permission was acquired and {@code false} otherwise
	 */

	boolean tryAcquirePermission();


	/**
	 * Acquires a permission to execute a call, only if one is available at the time of invocation
	 *
	 * @throws BulkheadFullException when the Bulkhead is full and no further calls are permitted.
	 */
	void acquirePermission();

	/**
	 * Releases a permission and increases the number of available permits by one.
	 * <p>
	 * Should only be used when a permission was acquired but not used. Otherwise use
	 * {@link AdaptiveBulkhead#onComplete(Duration)} ()} to signal a completed call and release a permission.
	 */
	void releasePermission();

	/**
	 * Records a completed call and releases a permission.
	 */
	void onComplete(Duration callTime);

	/**
	 * Returns the name of this bulkhead.
	 *
	 * @return the name of this bulkhead
	 */
	String getName();

	/**
	 * Returns the AdaptiveBulkheadConfig of this Bulkhead.
	 *
	 * @return bulkhead config
	 */
	AdaptiveBulkheadConfig getBulkheadConfig();

	/**
	 * Get the Metrics of this Bulkhead.
	 *
	 * @return the Metrics of this Bulkhead
	 */
	AdaptiveBulkheadMetrics getMetrics();

	/**
	 * Returns an EventPublisher which subscribes to the reactive stream of BulkheadEvent/BulkheadLimit events and
	 * can be used to register event consumers.
	 *
	 * @return an AdaptiveEventPublisher
	 */
	AdaptiveEventPublisher getEventPublisher();

	/**
	 * Decorates and executes the decorated Supplier.
	 *
	 * @param supplier the original Supplier
	 * @param <T>      the type of results supplied by this supplier
	 * @return the result of the decorated Supplier.
	 */
	default <T> T executeSupplier(Supplier<T> supplier) {
		return decorateSupplier(this, supplier).get();
	}

	/**
	 * Decorates and executes the decorated Callable.
	 *
	 * @param callable the original Callable
	 * @param <T>      the result type of callable
	 * @return the result of the decorated Callable.
	 * @throws Exception if unable to compute a result
	 */
	default <T> T executeCallable(Callable<T> callable) throws Exception {
		return decorateCallable(this, callable).call();
	}

	/**
	 * Decorates and executes the decorated Runnable.
	 *
	 * @param runnable the original Runnable
	 */
	default void executeRunnable(Runnable runnable) {
		decorateRunnable(this, runnable).run();
	}

	/**
	 * Decorates and executes the decorated Supplier.
	 *
	 * @param checkedSupplier the original Supplier
	 * @param <T>             the type of results supplied by this supplier
	 * @return the result of the decorated Supplier.
	 * @throws Throwable if something goes wrong applying this function to the given arguments
	 */
	default <T> T executeCheckedSupplier(CheckedFunction0<T> checkedSupplier) throws Throwable {
		return decorateCheckedSupplier(this, checkedSupplier).apply();
	}

	/**
	 * Decorates and executes the decorated CompletionStage.
	 *
	 * @param supplier the original CompletionStage
	 * @param <T>      the type of results supplied by this supplier
	 * @return the decorated CompletionStage.
	 */
	default <T> CompletionStage<T> executeCompletionStage(Supplier<CompletionStage<T>> supplier) {
		return decorateCompletionStage(this, supplier).get();
	}

	/**
	 * Returns a supplier which is decorated by a bulkhead.
	 *
	 * @param bulkhead the Bulkhead
	 * @param supplier the original supplier
	 * @param <T>      the type of results supplied by this supplier
	 * @return a supplier which is decorated by a Bulkhead.
	 */
	static <T> CheckedFunction0<T> decorateCheckedSupplier(AdaptiveBulkhead bulkhead, CheckedFunction0<T> supplier) {
		return () -> {
			bulkhead.acquirePermission();
			Instant start = null;
			try {
				start = Instant.now();
				return supplier.apply();
			} finally {
				Instant finish = Instant.now();
				bulkhead.onComplete(Duration.between(start, finish));
			}
		};
	}

	/**
	 * Returns a supplier which is decorated by a bulkhead.
	 *
	 * @param bulkhead the bulkhead
	 * @param supplier the original supplier
	 * @param <T>      the type of the returned CompletionStage's result
	 * @return a supplier which is decorated by a Bulkhead.
	 */
	static <T> Supplier<CompletionStage<T>> decorateCompletionStage(AdaptiveBulkhead bulkhead, Supplier<CompletionStage<T>> supplier) {
		return () -> {

			final CompletableFuture<T> promise = new CompletableFuture<>();

			if (!bulkhead.tryAcquirePermission()) {
				promise.completeExceptionally(new BulkheadFullException(bulkhead));
			} else {
				Instant start = Instant.now();
				try {
					supplier.get()
							.whenComplete(
									(result, throwable) -> {
										Instant finish = Instant.now();
										bulkhead.onComplete(Duration.between(start, finish));
										if (throwable != null) {
											promise.completeExceptionally(throwable);
										} else {
											promise.complete(result);
										}
									}
							);
				} catch (Exception e) {
					Instant finish = Instant.now();
					bulkhead.onComplete(Duration.between(start, finish));
					promise.completeExceptionally(e);
				}
			}

			return promise;
		};
	}

	/**
	 * Returns a runnable which is decorated by a bulkhead.
	 *
	 * @param bulkhead the bulkhead
	 * @param runnable the original runnable
	 * @return a runnable which is decorated by a Bulkhead.
	 */
	static CheckedRunnable decorateCheckedRunnable(AdaptiveBulkhead bulkhead, CheckedRunnable runnable) {
		return () -> {
			bulkhead.acquirePermission();
			Instant start = null;
			try {
				start = Instant.now();
				runnable.run();
			} finally {
				Instant finish = Instant.now();
				bulkhead.onComplete(Duration.between(start, finish));
			}
		};
	}

	/**
	 * Returns a callable which is decorated by a bulkhead.
	 *
	 * @param bulkhead the bulkhead
	 * @param callable the original Callable
	 * @param <T>      the result type of callable
	 * @return a supplier which is decorated by a Bulkhead.
	 */
	static <T> Callable<T> decorateCallable(AdaptiveBulkhead bulkhead, Callable<T> callable) {
		return () -> {
			bulkhead.acquirePermission();
			Instant start = null;
			try {
				start = Instant.now();
				return callable.call();
			} finally {
				Instant finish = Instant.now();
				bulkhead.onComplete(Duration.between(start, finish));
			}
		};
	}

	/**
	 * Returns a supplier which is decorated by a bulkhead.
	 *
	 * @param bulkhead the bulkhead
	 * @param supplier the original supplier
	 * @param <T>      the type of results supplied by this supplier
	 * @return a supplier which is decorated by a Bulkhead.
	 */
	static <T> Supplier<T> decorateSupplier(AdaptiveBulkhead bulkhead, Supplier<T> supplier) {
		return () -> {
			bulkhead.acquirePermission();
			Instant start = null;
			try {
				start = Instant.now();
				return supplier.get();
			} finally {
				Instant finish = Instant.now();
				bulkhead.onComplete(Duration.between(start, finish));
			}
		};
	}

	/**
	 * Returns a consumer which is decorated by a bulkhead.
	 *
	 * @param bulkhead the bulkhead
	 * @param consumer the original consumer
	 * @param <T>      the type of the input to the consumer
	 * @return a consumer which is decorated by a Bulkhead.
	 */
	static <T> Consumer<T> decorateConsumer(AdaptiveBulkhead bulkhead, Consumer<T> consumer) {
		return (t) -> {
			bulkhead.acquirePermission();
			Instant start = null;
			try {
				start = Instant.now();
				consumer.accept(t);
			} finally {
				Instant finish = Instant.now();
				bulkhead.onComplete(Duration.between(start, finish));
			}
		};
	}

	/**
	 * Returns a consumer which is decorated by a bulkhead.
	 *
	 * @param bulkhead the bulkhead
	 * @param consumer the original consumer
	 * @param <T>      the type of the input to the consumer
	 * @return a consumer which is decorated by a Bulkhead.
	 */
	static <T> CheckedConsumer<T> decorateCheckedConsumer(AdaptiveBulkhead bulkhead, CheckedConsumer<T> consumer) {
		return (t) -> {
			bulkhead.acquirePermission();
			Instant start = null;
			try {
				start = Instant.now();
				consumer.accept(t);

			} finally {
				Instant finish = Instant.now();
				bulkhead.onComplete(Duration.between(start, finish));
			}
		};
	}

	/**
	 * Returns a runnable which is decorated by a bulkhead.
	 *
	 * @param bulkhead the bulkhead
	 * @param runnable the original runnable
	 * @return a runnable which is decorated by a bulkhead.
	 */
	static Runnable decorateRunnable(AdaptiveBulkhead bulkhead, Runnable runnable) {
		return () -> {
			bulkhead.acquirePermission();
			Instant start = null;
			try {
				start = Instant.now();
				runnable.run();
			} finally {
				Instant finish = Instant.now();
				bulkhead.onComplete(Duration.between(start, finish));
			}
		};
	}

	/**
	 * Returns a function which is decorated by a bulkhead.
	 *
	 * @param bulkhead the bulkhead
	 * @param function the original function
	 * @param <T>      the type of the input to the function
	 * @param <R>      the type of the result of the function
	 * @return a function which is decorated by a bulkhead.
	 */
	static <T, R> Function<T, R> decorateFunction(AdaptiveBulkhead bulkhead, Function<T, R> function) {
		return (T t) -> {
			bulkhead.acquirePermission();
			Instant start = null;
			try {
				start = Instant.now();
				return function.apply(t);
			} finally {
				Instant finish = Instant.now();
				bulkhead.onComplete(Duration.between(start, finish));
			}
		};
	}

	/**
	 * Returns a function which is decorated by a bulkhead.
	 *
	 * @param bulkhead the bulkhead
	 * @param function the original function
	 * @param <T>      the type of the input to the function
	 * @param <R>      the type of the result of the function
	 * @return a function which is decorated by a bulkhead.
	 */
	static <T, R> CheckedFunction1<T, R> decorateCheckedFunction(AdaptiveBulkhead bulkhead, CheckedFunction1<T, R> function) {
		return (T t) -> {
			bulkhead.acquirePermission();
			Instant start = null;
			try {
				start = Instant.now();
				return function.apply(t);
			} finally {
				Instant finish = Instant.now();
				bulkhead.onComplete(Duration.between(start, finish));
			}
		};
	}

	/**
	 * Create a Bulkhead with a default configuration.
	 *
	 * @param name the name of the bulkhead
	 * @return a Bulkhead instance
	 */
	static AdaptiveBulkhead ofDefaults(String name) {
		return new AdaptiveLimitBulkhead(name, AdaptiveBulkheadConfig.builder().build());
	}


	/**
	 * Creates a bulkhead with a custom configuration and custom limiter
	 *
	 * @param name         the name of the bulkhead
	 * @param config       a custom BulkheadConfig configuration
	 * @param limitAdapter the custom limit adopter
	 * @return a Bulkhead instance
	 */
	static AdaptiveBulkhead of(String name, AdaptiveBulkheadConfig config, LimitAdapter limitAdapter) {
		return new AdaptiveLimitBulkhead(name, config, limitAdapter);
	}

	/**
	 * Creates a bulkhead with a custom configuration
	 *
	 * @param name   the name of the bulkhead
	 * @param config a custom BulkheadConfig configuration
	 * @return a Bulkhead instance
	 */
	static AdaptiveBulkhead of(String name, AdaptiveBulkheadConfig config) {
		return new AdaptiveLimitBulkhead(name, config);
	}

	/**
	 * Creates a bulkhead with a custom configuration
	 *
	 * @param name                   the name of the bulkhead
	 * @param bulkheadConfigSupplier custom configuration supplier
	 * @return a Bulkhead instance
	 */
	static AdaptiveBulkhead of(String name, Supplier<AdaptiveBulkheadConfig> bulkheadConfigSupplier) {
		return new AdaptiveLimitBulkhead(name, bulkheadConfigSupplier.get());
	}

	interface AdaptiveBulkheadMetrics extends Bulkhead.Metrics {

		/**
		 * @return max latency for service calls in millis
		 */
		double getMaxLatencyMillis();

		/**
		 * @return average latency for service calls in millis
		 */
		double getAverageLatencyMillis();
	}

	/**
	 * An EventPublisher which can be used to register event consumers.
	 */
	interface AdaptiveEventPublisher extends io.github.resilience4j.core.EventPublisher<BulkheadLimit> {

		EventPublisher onLimitIncreased(EventConsumer<BulkheadOnLimitDecreasedEvent> eventConsumer);

		EventPublisher onLimitDecreased(EventConsumer<BulkheadOnLimitIncreasedEvent> eventConsumer);
	}
}
