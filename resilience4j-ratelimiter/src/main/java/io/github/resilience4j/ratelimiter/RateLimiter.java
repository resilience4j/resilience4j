/*
 *
 *  Copyright 2016 Robert Winkler and Bohdan Storozhuk
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
package io.github.resilience4j.ratelimiter;

import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import io.github.resilience4j.ratelimiter.event.RateLimiterOnFailureEvent;
import io.github.resilience4j.ratelimiter.event.RateLimiterOnSuccessEvent;
import io.github.resilience4j.core.exception.AcquirePermissionCancelledException;
import io.github.resilience4j.ratelimiter.internal.AtomicRateLimiter;
import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedRunnable;
import io.vavr.control.Either;
import io.vavr.control.Try;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A RateLimiter instance is thread-safe can be used to decorate multiple requests.
 * <p>
 * A RateLimiter distributes permits at a configurable rate. {@link #acquirePermission()} blocks if necessary
 * until a permit is available, and then takes it. Once acquired, permits need not be released.
 */
public interface RateLimiter {

	/**
	 * Creates a RateLimiter with a custom RateLimiter configuration.
	 *
	 * @param name              the name of the RateLimiter
	 * @param rateLimiterConfig a custom RateLimiter configuration
	 * @return The {@link RateLimiter}
	 */
	static RateLimiter of(String name, RateLimiterConfig rateLimiterConfig) {
		return new AtomicRateLimiter(name, rateLimiterConfig);
	}

	/**
	 * Creates a RateLimiter with a custom RateLimiterConfig configuration.
	 *
	 * @param name                      the name of the RateLimiter
	 * @param rateLimiterConfigSupplier a supplier of a custom RateLimiterConfig configuration
	 * @return The {@link RateLimiter}
	 */
	static RateLimiter of(String name, Supplier<RateLimiterConfig> rateLimiterConfigSupplier) {
		return new AtomicRateLimiter(name, rateLimiterConfigSupplier.get());
	}

	/**
	 * Creates a RateLimiter with a default RateLimiterConfig configuration.
	 *
	 * @param name the name of the RateLimiter
	 * @return The {@link RateLimiter}
	 */
	static RateLimiter ofDefaults(String name) {
		return new AtomicRateLimiter(name, RateLimiterConfig.ofDefaults());
	}

	/**
	 * Decorates and executes the decorated CompletionStage.
	 *
	 * @param supplier the original CompletionStage
	 * @param <T> the type of results supplied by this supplier
	 * @return the decorated CompletionStage.
	 */
	default <T> CompletionStage<T> executeCompletionStage(Supplier<CompletionStage<T>> supplier){
		return decorateCompletionStage(this, supplier).get();
	}

	/**
	 * Returns a supplier which is decorated by a rateLimiter.
	 *
	 * @param rateLimiter the rateLimiter
	 * @param supplier    the original supplier
	 * @param <T>         the type of the returned CompletionStage's result
	 * @return a supplier which is decorated by a RateLimiter.
	 */
	static <T> Supplier<CompletionStage<T>> decorateCompletionStage(RateLimiter rateLimiter, Supplier<CompletionStage<T>> supplier) {
		return () -> {

			final CompletableFuture<T> promise = new CompletableFuture<>();
			try {
				waitForPermission(rateLimiter);
				supplier.get()
						.whenComplete(
								(result, throwable) -> {
									if (throwable != null) {
										promise.completeExceptionally(throwable);
									} else {
										promise.complete(result);
									}
								}
						);
			} catch (Exception exception) {
				promise.completeExceptionally(exception);
			}
			return promise;
		};
	}

	/**
	 * Creates a supplier which is restricted by a RateLimiter.
	 *
	 * @param rateLimiter the RateLimiter
	 * @param supplier    the original supplier
	 * @param <T>         the type of results supplied supplier
	 * @return a supplier which is restricted by a RateLimiter.
	 */
	static <T> CheckedFunction0<T> decorateCheckedSupplier(RateLimiter rateLimiter, CheckedFunction0<T> supplier) {
		return () -> {
			waitForPermission(rateLimiter);
			return supplier.apply();
		};
	}

	/**
	 * Creates a runnable which is restricted by a RateLimiter.
	 *
	 * @param rateLimiter the RateLimiter
	 * @param runnable    the original runnable
	 * @return a runnable which is restricted by a RateLimiter.
	 */
	static CheckedRunnable decorateCheckedRunnable(RateLimiter rateLimiter, CheckedRunnable runnable) {

		return () -> {
			waitForPermission(rateLimiter);
			runnable.run();
		};
	}

	/**
	 * Creates a function which is restricted by a RateLimiter.
	 *
	 * @param rateLimiter the RateLimiter
	 * @param function    the original function
	 * @param <T>         the type of function argument
	 * @param <R>         the type of function results
	 * @return a function which is restricted by a RateLimiter.
	 */
	static <T, R> CheckedFunction1<T, R> decorateCheckedFunction(RateLimiter rateLimiter, CheckedFunction1<T, R> function) {
		return (T t) -> {
			waitForPermission(rateLimiter);
			return function.apply(t);
		};
	}

	/**
	 * Creates a supplier which is restricted by a RateLimiter.
	 *
	 * @param rateLimiter the RateLimiter
	 * @param supplier    the original supplier
	 * @param <T>         the type of results supplied supplier
	 * @return a supplier which is restricted by a RateLimiter.
	 */
	static <T> Supplier<T> decorateSupplier(RateLimiter rateLimiter, Supplier<T> supplier) {
		return () -> {
			waitForPermission(rateLimiter);
			return supplier.get();
		};
	}

	/**
	 * Creates a supplier which is restricted by a RateLimiter.
	 *
	 * @param rateLimiter the RateLimiter
	 * @param supplier    the original supplier
	 * @param <T>         the type of results supplied supplier
	 * @return a supplier which is restricted by a RateLimiter.
	 */
	static <T> Supplier<Try<T>> decorateTrySupplier(RateLimiter rateLimiter, Supplier<Try<T>> supplier){
		return () -> {
			try{
				waitForPermission(rateLimiter);
				return supplier.get();
			}catch (RequestNotPermitted requestNotPermitted){
				return Try.failure(requestNotPermitted);
			}
		};
	}

	/**
	 * Creates a supplier which is restricted by a RateLimiter.
	 *
	 * @param rateLimiter the RateLimiter
	 * @param supplier    the original supplier
	 * @param <T>         the type of results supplied supplier
	 * @return a supplier which is restricted by a RateLimiter.
	 */
	static <T> Supplier<Either<Exception, T>> decorateEitherSupplier(RateLimiter rateLimiter, Supplier<Either<? extends Exception, T>> supplier){
		return () -> {
			try{
				waitForPermission(rateLimiter);
				return Either.narrow(supplier.get());
			}catch (RequestNotPermitted requestNotPermitted){
				return Either.left(requestNotPermitted);
			}
		};
	}

	static <T> Callable<T> decorateCallable(RateLimiter rateLimiter, Callable<T> callable) {
		return () -> {
			waitForPermission(rateLimiter);
			return callable.call();
		};
	}

	/**
	 * Creates a consumer which is restricted by a RateLimiter.
	 *
	 * @param rateLimiter the RateLimiter
	 * @param consumer    the original consumer
	 * @param <T>         the type of the input to the consumer
	 * @return a consumer which is restricted by a RateLimiter.
	 */
	static <T> Consumer<T> decorateConsumer(RateLimiter rateLimiter, Consumer<T> consumer) {
		return (T t) -> {
			waitForPermission(rateLimiter);
			consumer.accept(t);
		};
	}

	/**
	 * Creates a runnable which is restricted by a RateLimiter.
	 *
	 * @param rateLimiter the RateLimiter
	 * @param runnable    the original runnable
	 * @return a runnable which is restricted by a RateLimiter.
	 */
	static Runnable decorateRunnable(RateLimiter rateLimiter, Runnable runnable) {
		return () -> {
			waitForPermission(rateLimiter);
			runnable.run();
		};
	}


	/**
	 * Creates a function which is restricted by a RateLimiter.
	 *
	 * @param rateLimiter the RateLimiter
	 * @param function    the original function
	 * @param <T>         the type of the input to the function
	 * @param <R>         the type of the result of the function
	 * @return a function which is restricted by a RateLimiter.
	 */
	static <T, R> Function<T, R> decorateFunction(RateLimiter rateLimiter, Function<T, R> function) {
		return (T t) -> {
			waitForPermission(rateLimiter);
			return function.apply(t);
		};
	}

	/**
	 * Will wait for permission within default timeout duration.
	 *
	 * @param rateLimiter the RateLimiter to get permission from
	 * @throws RequestNotPermitted   if waiting time elapsed before a permit was acquired.
	 * @throws AcquirePermissionCancelledException if thread was interrupted during permission wait
	 */
	static void waitForPermission(final RateLimiter rateLimiter) {
		boolean permission = rateLimiter.acquirePermission();
		if (Thread.currentThread().isInterrupted()) {
			throw new AcquirePermissionCancelledException();
		}
		if (!permission) {
			throw RequestNotPermitted.createRequestNotPermitted(rateLimiter);
		}
	}

	/**
	 * Dynamic rate limiter configuration change.
	 * This method allows to change timeout duration of current limiter.
	 * NOTE! New timeout duration won't affect threads that are currently waiting for permission.
	 *
	 * @param timeoutDuration new timeout duration
	 */
	void changeTimeoutDuration(Duration timeoutDuration);

	/**
	 * Dynamic rate limiter configuration change.
	 * This method allows to change count of permissions available during refresh period.
	 * NOTE! New limit won't affect current period permissions and will apply only from next one.
	 *
	 * @param limitForPeriod new permissions limit
	 */
	void changeLimitForPeriod(int limitForPeriod);

	/**
	 * Acquires a permission from this rate limiter, blocking until one is available, or the thread is interrupted.
	 * Maximum wait time is {@link RateLimiterConfig#getTimeoutDuration()}
	 *
	 * <p>If the current thread is {@linkplain Thread#interrupt interrupted}
	 * while waiting for a permit then it won't throw {@linkplain InterruptedException},
	 * but its interrupt status will be set.
	 *
	 * @return {@code true} if a permit was acquired and {@code false}
	 * if waiting timeoutDuration elapsed before a permit was acquired
	 */
	default boolean acquirePermission() {
		return acquirePermission(1);
	}

	/**
	 * Acquires the given number of permits from this rate limiter, blocking until one is available, or the thread is interrupted.
	 * Maximum wait time is {@link RateLimiterConfig#getTimeoutDuration()}
	 *
	 * <p>If the current thread is {@linkplain Thread#interrupt interrupted}
	 * while waiting for a permit then it won't throw {@linkplain InterruptedException},
	 * but its interrupt status will be set.
	 * 
	 * @param permits number of permits - use for systems where 1 call != 1 permit
	 * @return {@code true} if a permit was acquired and {@code false}
	 * if waiting timeoutDuration elapsed before a permit was acquired
	 */
	boolean acquirePermission(int permits);

	/**
	 * Reserves a permission from this rate limiter and returns nanoseconds you should wait for it.
	 * If returned long is negative, it means that you failed to reserve permission,
	 * possibly your  {@link RateLimiterConfig#getTimeoutDuration()} is less then time to wait for permission.
	 *
	 * @return {@code long} amount of nanoseconds you should wait for reserved permissions. if negative, it means you failed to reserve.
	 */
	default long reservePermission() {
		return reservePermission(1);
	}

	/**
	 * Reserves the given number permits from this rate limiter and returns nanoseconds you should wait for it.
	 * If returned long is negative, it means that you failed to reserve permission,
	 * possibly your  {@link RateLimiterConfig#getTimeoutDuration()} is less then time to wait for permission.
	 *
	 * @param permits number of permits - use for systems where 1 call != 1 permit
	 * @return {@code long} amount of nanoseconds you should wait for reserved permissions. if negative, it means you failed to reserve.
	 */
	long reservePermission(int permits);

	/**
	 * Get the name of this RateLimiter
	 *
	 * @return the name of this RateLimiter
	 */
	String getName();

	/**
	 * Get the RateLimiterConfig of this RateLimiter.
	 *
	 * @return the RateLimiterConfig of this RateLimiter
	 */
	RateLimiterConfig getRateLimiterConfig();

	/**
	 * Get the Metrics of this RateLimiter.
	 *
	 * @return the Metrics of this RateLimiter
	 */
	Metrics getMetrics();

	/**
	 * Returns an EventPublisher which can be used to register event consumers.
	 *
	 * @return an EventPublisher
	 */
	EventPublisher getEventPublisher();

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
	 * Decorates and executes the decorated Supplier.
	 *
	 * @param supplier the original Supplier
	 * @param <T>      the type of results supplied by this supplier
	 * @return the result of the decorated Supplier.
	 */
	default <T> Try<T> executeTrySupplier(Supplier<Try<T>> supplier) {
		return decorateTrySupplier(this, supplier).get();
	}

	/**
	 * Decorates and executes the decorated Supplier.
	 *
	 * @param supplier the original Supplier
	 * @param <T>      the type of results supplied by this supplier
	 * @return the result of the decorated Supplier.
	 */
	default <T> Either<Exception, T> executeEitherSupplier(Supplier<Either<? extends Exception, T>> supplier) {
		return decorateEitherSupplier(this, supplier).get();
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


	interface Metrics {
		/**
		 * Returns an estimate of the number of threads waiting for permission
		 * in this JVM process.
		 * <p>This method is typically used for debugging and testing purposes.
		 *
		 * @return estimate of the number of threads waiting for permission.
		 */
		int getNumberOfWaitingThreads();

		/**
		 * Estimates count of available permissions.
		 * Can be negative if some permissions where reserved.
		 * <p>This method is typically used for debugging and testing purposes.
		 *
		 * @return estimated count of permissions
		 */
		int getAvailablePermissions();
	}

	/**
	 * An EventPublisher which can be used to register event consumers.
	 */
	interface EventPublisher extends io.github.resilience4j.core.EventPublisher<RateLimiterEvent> {

		EventPublisher onSuccess(EventConsumer<RateLimiterOnSuccessEvent> eventConsumer);

		EventPublisher onFailure(EventConsumer<RateLimiterOnFailureEvent> eventConsumer);

	}
}
