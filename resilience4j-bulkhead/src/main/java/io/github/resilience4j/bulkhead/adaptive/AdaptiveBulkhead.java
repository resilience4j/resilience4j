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
import io.github.resilience4j.core.lang.Nullable;
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
 * Once the operation is complete, regardless of the result, client needs to call {@link AdaptiveBulkhead#onComplete(Duration, boolean)} ()} in order to maintain
 * integrity of internal bulkhead state which is handled as the following :
 * <p>
 * Adaptive capacity management by default use AIMD algorithm for limit control but the user can inject custom limiter implementation by implementing {@link LimitPolicy}
 * <p>
 */
public interface AdaptiveBulkhead {


	void increaseInProcessingRequestsCount();

	void decreaseInProcessingRequestsCount();

	int getCurrentInProcessingRequestsCount();

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
	 * {@link AdaptiveBulkhead#onComplete(Duration, boolean)} ()} to signal a completed call and release a permission.
	 */
	void releasePermission();

	/**
	 * Records a completed call and releases a permission.
	 */
	void onComplete(Duration callTime, boolean isSuccess);

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
			} catch (Exception e) {
				handleError(bulkhead, start, e);
				throw e;
			} finally {
				if (start != null) {
					Instant finish = Instant.now();
					bulkhead.onComplete(Duration.between(start, finish), true);
				}
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
										if (throwable != null) {
											bulkhead.onComplete(Duration.between(start, finish), false);
											promise.completeExceptionally(throwable);
										} else {
											bulkhead.onComplete(Duration.between(start, finish), true);
											promise.complete(result);
										}
									}
							);
				} catch (Exception e) {
					handleError(bulkhead, start, e);
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
			} catch (Exception e) {
				handleError(bulkhead, start, e);
				throw e;
			} finally {
				if (start != null) {
					Instant finish = Instant.now();
					bulkhead.onComplete(Duration.between(start, finish), true);
				}
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
			} catch (Exception e) {
				handleError(bulkhead, start, e);
				throw e;
			} finally {
				if (start != null) {
					Instant finish = Instant.now();
					bulkhead.onComplete(Duration.between(start, finish), true);
				}
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
			} catch (Exception e) {
				handleError(bulkhead, start, e);
				throw e;
			} finally {
				if (start != null) {
					Instant finish = Instant.now();
					bulkhead.onComplete(Duration.between(start, finish), true);
				}
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
			} catch (Exception e) {
				handleError(bulkhead, start, e);
				throw e;
			} finally {
				if (start != null) {
					Instant finish = Instant.now();
					bulkhead.onComplete(Duration.between(start, finish), true);
				}
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

			} catch (Exception e) {
				handleError(bulkhead, start, e);
				throw e;
			} finally {
				if (start != null) {
					Instant finish = Instant.now();
					bulkhead.onComplete(Duration.between(start, finish), true);
				}
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
			} catch (Exception e) {
				handleError(bulkhead, start, e);
				throw e;
			} finally {
				if (start != null) {
					Instant finish = Instant.now();
					bulkhead.onComplete(Duration.between(start, finish), true);
				}
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
			} catch (Exception e) {
				handleError(bulkhead, start, e);
				throw e;
			} finally {
				if (start != null) {
					Instant finish = Instant.now();
					bulkhead.onComplete(Duration.between(start, finish), true);
				}
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
			} catch (Exception e) {
				handleError(bulkhead, start, e);
				throw e;
			} finally {
				if (start != null) {
					Instant finish = Instant.now();
					bulkhead.onComplete(Duration.between(start, finish), true);
				}
			}
		};
	}

	static void handleError(AdaptiveBulkhead bulkhead, @Nullable Instant start, Exception e) {
		if (bulkhead.getBulkheadConfig().getAdaptIfError() != null) {
			//noinspection unchecked
			if (bulkhead.getBulkheadConfig().getAdaptIfError().test(e) && start != null) {
				Instant finish = Instant.now();
				bulkhead.onComplete(Duration.between(start, finish), false);

			}
		} else {
			bulkhead.releasePermission();
		}
	}

	/**
	 * Create a Bulkhead with a default configuration.
	 *
	 * @param name the name of the bulkhead
	 * @return a Bulkhead instance
	 */
	static AdaptiveBulkhead ofDefaults(String name) {
		return AdaptiveLimitBulkhead.factory().createAdaptiveLimitBulkhead(name, AdaptiveBulkheadConfig.ofDefaults());
	}


	/**
	 * Creates a bulkhead with a custom configuration and custom limiter
	 *
	 * @param name         the name of the bulkhead
	 * @param config       a custom BulkheadConfig configuration
	 * @param limitAdapter the custom limit adopter
	 * @return a Bulkhead instance
	 */
	static AdaptiveBulkhead of(String name, AdaptiveBulkheadConfig config, LimitPolicy<Bulkhead> limitAdapter) {
		return AdaptiveLimitBulkhead.factory().createAdaptiveLimitBulkhead(name, config, limitAdapter);
	}

	/**
	 * Creates a bulkhead with a custom configuration
	 *
	 * @param name   the name of the bulkhead
	 * @param config a custom BulkheadConfig configuration
	 * @return a Bulkhead instance
	 */
	static AdaptiveBulkhead of(String name, AdaptiveBulkheadConfig config) {
		return AdaptiveLimitBulkhead.factory().createAdaptiveLimitBulkhead(name, config);
	}

	/**
	 * Creates a bulkhead with a custom configuration
	 *
	 * @param name                   the name of the bulkhead
	 * @param bulkheadConfigSupplier custom configuration supplier
	 * @return a Bulkhead instance
	 */
	static AdaptiveBulkhead of(String name, Supplier<AdaptiveBulkheadConfig> bulkheadConfigSupplier) {
		return AdaptiveLimitBulkhead.factory().createAdaptiveLimitBulkhead(name, bulkheadConfigSupplier.get());
	}

	interface AdaptiveBulkheadMetrics extends Bulkhead.Metrics {

		/**
		 * @return average latency for service calls in millis
		 */
		double getAverageLatencyMillis();

		/**
		 * @return current failure rate for recorded calls if error check is enabled for related exceptions
		 */
		double getFailureRate();

		/**
		 * @return current slow call rate for the recorded calls
		 */
		double getSlowCallRate();
	}

	/**
	 * An EventPublisher which can be used to register event consumers.
	 */
	interface AdaptiveEventPublisher extends io.github.resilience4j.core.EventPublisher<BulkheadLimit> {

		EventPublisher onLimitIncreased(EventConsumer<BulkheadOnLimitDecreasedEvent> eventConsumer);

		EventPublisher onLimitDecreased(EventConsumer<BulkheadOnLimitIncreasedEvent> eventConsumer);
	}
}
