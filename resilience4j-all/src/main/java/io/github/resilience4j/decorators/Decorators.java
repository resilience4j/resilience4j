package io.github.resilience4j.decorators;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.cache.Cache;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.AsyncRetry;
import io.github.resilience4j.retry.Retry;
import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedRunnable;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A Decorator builder which can be used to apply multiple decorators to a (Checked-)Supplier, (Checked-)Function,
 * (Checked-)Runnable, (Checked-)CompletionStage or (Checked-)Consumer
 */
public interface Decorators {

	static <T> DecorateSupplier<T> ofSupplier(Supplier<T> supplier) {
		return new DecorateSupplier<>(supplier);
	}

	static <T, R> DecorateFunction<T, R> ofFunction(Function<T, R> function) {
		return new DecorateFunction<>(function);
	}

	static DecorateRunnable ofRunnable(Runnable runnable) {
		return new DecorateRunnable(runnable);
	}

	static <T> DecorateCheckedSupplier<T> ofCheckedSupplier(CheckedFunction0<T> supplier) {
		return new DecorateCheckedSupplier<>(supplier);
	}

	static <T, R> DecorateCheckedFunction<T, R> ofCheckedFunction(CheckedFunction1<T, R> function) {
		return new DecorateCheckedFunction<>(function);
	}

	static DecorateCheckedRunnable ofCheckedRunnable(CheckedRunnable supplier) {
		return new DecorateCheckedRunnable(supplier);
	}

	static <T> DecorateCompletionStage<T> ofCompletionStage(Supplier<CompletionStage<T>> stageSupplier) {
		return new DecorateCompletionStage<>(stageSupplier);
	}

	public static <T> DecorateConsumer<T> ofConsumer(Consumer<T> consumer) {
		return new DecorateConsumer<>(consumer);
	}

	class DecorateSupplier<T> {
		private Supplier<T> supplier;

		private DecorateSupplier(Supplier<T> supplier) {
			this.supplier = supplier;
		}


		public DecorateSupplier<T> withCircuitBreaker(CircuitBreaker circuitBreaker) {
			supplier = CircuitBreaker.decorateSupplier(circuitBreaker, supplier);
			return this;
		}

		public DecorateSupplier<T> withRetry(Retry retryContext) {
			supplier = Retry.decorateSupplier(retryContext, supplier);
			return this;
		}

		public <K> DecorateFunction<K, T> withCache(Cache<K, T> cache) {
			return Decorators.ofFunction(Cache.decorateSupplier(cache, supplier));
		}

		public DecorateSupplier<T> withRateLimiter(RateLimiter rateLimiter) {
			supplier = RateLimiter.decorateSupplier(rateLimiter, supplier);
			return this;
		}

		public DecorateSupplier<T> withBulkhead(Bulkhead bulkhead) {
			supplier = Bulkhead.decorateSupplier(bulkhead, supplier);
			return this;
		}

		public Supplier<T> decorate() {
			return supplier;
		}

		public T get() {
			return supplier.get();
		}
	}

	class DecorateFunction<T, R> {
		private Function<T, R> function;

		private DecorateFunction(Function<T, R> function) {
			this.function = function;
		}

		public DecorateFunction<T, R> withCircuitBreaker(CircuitBreaker circuitBreaker) {
			function = CircuitBreaker.decorateFunction(circuitBreaker, function);
			return this;
		}

		public DecorateFunction<T, R> withRetry(Retry retryContext) {
			function = Retry.decorateFunction(retryContext, function);
			return this;
		}

		public DecorateFunction<T, R> withRateLimiter(RateLimiter rateLimiter) {
			function = RateLimiter.decorateFunction(rateLimiter, function);
			return this;
		}

		public DecorateFunction<T, R> withBulkhead(Bulkhead bulkhead) {
			function = Bulkhead.decorateFunction(bulkhead, function);
			return this;
		}

		public Function<T, R> decorate() {
			return function;
		}

		public R apply(T t) {
			return function.apply(t);
		}
	}

	class DecorateRunnable {
		private Runnable runnable;

		private DecorateRunnable(Runnable runnable) {
			this.runnable = runnable;
		}

		public DecorateRunnable withCircuitBreaker(CircuitBreaker circuitBreaker) {
			runnable = CircuitBreaker.decorateRunnable(circuitBreaker, runnable);
			return this;
		}

		public DecorateRunnable withRetry(Retry retryContext) {
			runnable = Retry.decorateRunnable(retryContext, runnable);
			return this;
		}

		public DecorateRunnable withRateLimiter(RateLimiter rateLimiter) {
			runnable = RateLimiter.decorateRunnable(rateLimiter, runnable);
			return this;
		}

		public DecorateRunnable withBulkhead(Bulkhead bulkhead) {
			runnable = Bulkhead.decorateRunnable(bulkhead, runnable);
			return this;
		}

		public Runnable decorate() {
			return runnable;
		}

		public void run() {
			runnable.run();
		}
	}

	class DecorateCheckedSupplier<T> {
		private CheckedFunction0<T> supplier;

		private DecorateCheckedSupplier(CheckedFunction0<T> supplier) {
			this.supplier = supplier;
		}


		public DecorateCheckedSupplier<T> withCircuitBreaker(CircuitBreaker circuitBreaker) {
			supplier = CircuitBreaker.decorateCheckedSupplier(circuitBreaker, supplier);
			return this;
		}

		public DecorateCheckedSupplier<T> withRetry(Retry retryContext) {
			supplier = Retry.decorateCheckedSupplier(retryContext, supplier);
			return this;
		}

		public DecorateCheckedSupplier<T> withRateLimiter(RateLimiter rateLimiter) {
			supplier = RateLimiter.decorateCheckedSupplier(rateLimiter, supplier);
			return this;
		}

		public <K> DecorateCheckedFunction<K, T> withCache(Cache<K, T> cache) {
			return Decorators.ofCheckedFunction(Cache.decorateCheckedSupplier(cache, supplier));
		}

		public DecorateCheckedSupplier<T> withBulkhead(Bulkhead bulkhead) {
			supplier = Bulkhead.decorateCheckedSupplier(bulkhead, supplier);
			return this;
		}

		public CheckedFunction0<T> decorate() {
			return supplier;
		}

		public T get() throws Throwable {
			return supplier.apply();
		}
	}

	class DecorateCheckedFunction<T, R> {
		private CheckedFunction1<T, R> function;

		private DecorateCheckedFunction(CheckedFunction1<T, R> function) {
			this.function = function;
		}

		public DecorateCheckedFunction<T, R> withCircuitBreaker(CircuitBreaker circuitBreaker) {
			function = CircuitBreaker.decorateCheckedFunction(circuitBreaker, function);
			return this;
		}

		public DecorateCheckedFunction<T, R> withRetry(Retry retryContext) {
			function = Retry.decorateCheckedFunction(retryContext, function);
			return this;
		}

		public DecorateCheckedFunction<T, R> withRateLimiter(RateLimiter rateLimiter) {
			function = RateLimiter.decorateCheckedFunction(rateLimiter, function);
			return this;
		}

		public DecorateCheckedFunction<T, R> withBulkhead(Bulkhead bulkhead) {
			function = Bulkhead.decorateCheckedFunction(bulkhead, function);
			return this;
		}

		public CheckedFunction1<T, R> decorate() {
			return function;
		}

		public R apply(T t) throws Throwable {
			return function.apply(t);
		}
	}

	class DecorateCheckedRunnable {
		private CheckedRunnable runnable;

		private DecorateCheckedRunnable(CheckedRunnable runnable) {
			this.runnable = runnable;
		}

		public DecorateCheckedRunnable withCircuitBreaker(CircuitBreaker circuitBreaker) {
			runnable = CircuitBreaker.decorateCheckedRunnable(circuitBreaker, runnable);
			return this;
		}

		public DecorateCheckedRunnable withRetry(Retry retryContext) {
			runnable = Retry.decorateCheckedRunnable(retryContext, runnable);
			return this;
		}

		public DecorateCheckedRunnable withRateLimiter(RateLimiter rateLimiter) {
			runnable = RateLimiter.decorateCheckedRunnable(rateLimiter, runnable);
			return this;
		}

		public DecorateCheckedRunnable withBulkhead(Bulkhead bulkhead) {
			runnable = Bulkhead.decorateCheckedRunnable(bulkhead, runnable);
			return this;
		}

		public CheckedRunnable decorate() {
			return runnable;
		}

		public void run() throws Throwable {
			runnable.run();
		}
	}

	class DecorateCompletionStage<T> {

		private Supplier<CompletionStage<T>> stageSupplier;

		public DecorateCompletionStage(Supplier<CompletionStage<T>> stageSupplier) {
			this.stageSupplier = stageSupplier;
		}

		public DecorateCompletionStage<T> withCircuitBreaker(CircuitBreaker circuitBreaker) {
			stageSupplier = CircuitBreaker.decorateCompletionStage(circuitBreaker, stageSupplier);
			return this;
		}

		/**
		 * @deprecated replaced by {@link #withRetry(Retry retryContext, ScheduledExecutorService scheduler)}
		 */
		@Deprecated()
		public DecorateCompletionStage<T> withRetry(AsyncRetry retryContext, ScheduledExecutorService scheduler) {
			stageSupplier = AsyncRetry.decorateCompletionStage(retryContext, scheduler, stageSupplier);
			return this;
		}

		public DecorateCompletionStage<T> withRetry(Retry retryContext, ScheduledExecutorService scheduler) {
			stageSupplier = Retry.decorateCompletionStage(retryContext, scheduler, stageSupplier);
			return this;
		}

		public DecorateCompletionStage<T> withBulkhead(Bulkhead bulkhead) {
			stageSupplier = Bulkhead.decorateCompletionStage(bulkhead, stageSupplier);
			return this;
		}

		public DecorateCompletionStage<T> withRateLimiter(RateLimiter rateLimiter) {
			stageSupplier = RateLimiter.decorateCompletionStage(rateLimiter, stageSupplier);
			return this;
		}

		public Supplier<CompletionStage<T>> decorate() {
			return stageSupplier;
		}

		public CompletionStage<T> get() {
			return stageSupplier.get();
		}
	}

	class DecorateConsumer<T> {

		private Consumer<T> consumer;

		private DecorateConsumer(Consumer<T> consumer) {
			this.consumer = consumer;
		}

		public DecorateConsumer<T> withCircuitBreaker(CircuitBreaker circuitBreaker) {
			consumer = CircuitBreaker.decorateConsumer(circuitBreaker, consumer);
			return this;
		}

		public DecorateConsumer<T> withRateLimiter(RateLimiter rateLimiter) {
			consumer = RateLimiter.decorateConsumer(rateLimiter, consumer);
			return this;
		}

		public DecorateConsumer<T> withBulkhead(Bulkhead bulkhead) {
			consumer = Bulkhead.decorateConsumer(bulkhead, consumer);
			return this;
		}

		public Consumer<T> decorate() {
			return consumer;
		}

		public void accept(T obj) {
			consumer.accept(obj);
		}
	}
}
