/*
 *
 *  Copyright 2016 Robert Winkler
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
package io.github.resilience4j.retry.internal;

import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.event.RetryEvent;
import io.github.resilience4j.retry.event.RetryOnErrorEvent;
import io.github.resilience4j.retry.event.RetryOnIgnoredErrorEvent;
import io.github.resilience4j.retry.event.RetryOnRetryEvent;
import io.github.resilience4j.retry.event.RetryOnSuccessEvent;
import io.vavr.CheckedConsumer;
import io.vavr.control.Option;
import io.vavr.control.Try;

public class RetryImpl<T> implements Retry {


	/*package*/ static CheckedConsumer<Long> sleepFunction = Thread::sleep;
	private final Metrics metrics;
	private final RetryEventProcessor eventProcessor;
	private final Predicate<T> resultPredicate;
	private String name;
	private RetryConfig config;
	private int maxAttempts;
	private Function<Integer, Long> intervalFunction;
	private Predicate<Throwable> exceptionPredicate;
	private LongAdder succeededAfterRetryCounter;
	private LongAdder failedAfterRetryCounter;
	private LongAdder succeededWithoutRetryCounter;
	private LongAdder failedWithoutRetryCounter;

	public RetryImpl(String name, RetryConfig config) {
		this.name = name;
		this.config = config;
		this.maxAttempts = config.getMaxAttempts();
		this.intervalFunction = config.getIntervalFunction();
		this.exceptionPredicate = config.getExceptionPredicate();
		this.resultPredicate = config.getResultPredicate();
		this.metrics = this.new RetryMetrics();
		this.eventProcessor = new RetryEventProcessor();
		succeededAfterRetryCounter = new LongAdder();
		failedAfterRetryCounter = new LongAdder();
		succeededWithoutRetryCounter = new LongAdder();
		failedWithoutRetryCounter = new LongAdder();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return name;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Context context() {
		return new ContextImpl();
	}

	@Override
	@SuppressWarnings("unchecked")
	public AsyncContext asyncContext() {
		return new AsyncContextImpl();
	}

	@Override
	public RetryConfig getRetryConfig() {
		return config;
	}

	private void publishRetryEvent(Supplier<RetryEvent> event) {
		if (eventProcessor.hasConsumers()) {
			eventProcessor.consumeEvent(event.get());
		}
	}

	@Override
	public EventPublisher getEventPublisher() {
		return eventProcessor;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Metrics getMetrics() {
		return this.metrics;
	}

	public final class ContextImpl implements Retry.Context<T> {

		private final AtomicInteger numOfAttempts = new AtomicInteger(0);
		private final AtomicReference<Exception> lastException = new AtomicReference<>();
		private final AtomicReference<RuntimeException> lastRuntimeException = new AtomicReference<>();

		private ContextImpl() {
		}

		public void onSuccess() {
			int currentNumOfAttempts = numOfAttempts.get();
			if (currentNumOfAttempts > 0) {
				succeededAfterRetryCounter.increment();
				Throwable throwable = Option.of(lastException.get()).getOrElse(lastRuntimeException.get());
				publishRetryEvent(() -> new RetryOnSuccessEvent(getName(), currentNumOfAttempts, throwable));
			} else {
				succeededWithoutRetryCounter.increment();
			}
		}

		public boolean onResult(T result) {
			if (null != resultPredicate && resultPredicate.test(result)) {
				int currentNumOfAttempts = numOfAttempts.incrementAndGet();
				if (currentNumOfAttempts >= maxAttempts) {
					return false;
				} else {
					waitIntervalAfterFailure(currentNumOfAttempts, null);
					return true;
				}
			}
			return false;
		}

		public void onError(Exception exception) throws Exception {
			if (exceptionPredicate.test(exception)) {
				lastException.set(exception);
				throwOrSleepAfterException();
			} else {
				failedWithoutRetryCounter.increment();
				publishRetryEvent(() -> new RetryOnIgnoredErrorEvent(getName(), exception));
				throw exception;
			}
		}

		public void onRuntimeError(RuntimeException runtimeException) {
			if (exceptionPredicate.test(runtimeException)) {
				lastRuntimeException.set(runtimeException);
				throwOrSleepAfterRuntimeException();
			} else {
				failedWithoutRetryCounter.increment();
				publishRetryEvent(() -> new RetryOnIgnoredErrorEvent(getName(), runtimeException));
				throw runtimeException;
			}
		}

		private void throwOrSleepAfterException() throws Exception {
			int currentNumOfAttempts = numOfAttempts.incrementAndGet();
			Exception throwable = lastException.get();
			if (currentNumOfAttempts >= maxAttempts) {
				failedAfterRetryCounter.increment();
				publishRetryEvent(() -> new RetryOnErrorEvent(getName(), currentNumOfAttempts, throwable));
				throw throwable;
			} else {
				waitIntervalAfterFailure(currentNumOfAttempts, throwable);
			}
		}

		private void throwOrSleepAfterRuntimeException() {
			int currentNumOfAttempts = numOfAttempts.incrementAndGet();
			RuntimeException throwable = lastRuntimeException.get();
			if (currentNumOfAttempts >= maxAttempts) {
				failedAfterRetryCounter.increment();
				publishRetryEvent(() -> new RetryOnErrorEvent(getName(), currentNumOfAttempts, throwable));
				throw throwable;
			} else {
				waitIntervalAfterFailure(currentNumOfAttempts, throwable);
			}
		}

		private void waitIntervalAfterFailure(int currentNumOfAttempts, Throwable throwable) {
			// wait interval until the next attempt should start
			long interval = intervalFunction.apply(numOfAttempts.get());
			publishRetryEvent(() -> new RetryOnRetryEvent(getName(), currentNumOfAttempts, throwable, interval));
			Try.run(() -> sleepFunction.accept(interval))
					.getOrElseThrow(ex -> lastRuntimeException.get());
		}

	}

	public final class AsyncContextImpl implements Retry.AsyncContext<T> {

		private final AtomicInteger numOfAttempts = new AtomicInteger(0);
		private final AtomicReference<Throwable> lastException = new AtomicReference<>();

		@Override
		public void onSuccess() {
			int currentNumOfAttempts = numOfAttempts.get();
			if (currentNumOfAttempts > 0) {
				succeededAfterRetryCounter.increment();
				publishRetryEvent(() -> new RetryOnSuccessEvent(name, currentNumOfAttempts, lastException.get()));
			} else {
				succeededWithoutRetryCounter.increment();
			}
		}

		@Override
		public long onError(Throwable throwable) {
			// handle the case if the completable future throw CompletionException wrapping the original exception
			// where original exception is the the one to retry not the CompletionException
			// for more information about exception handling in completable future check for example :
			//https://stackoverflow.com/questions/44409962/throwing-exception-from-completablefuture
			if (throwable instanceof CompletionException && !exceptionPredicate.test(throwable)) {
				if (!exceptionPredicate.test(throwable.getCause())) {
					failedWithoutRetryCounter.increment();
					publishRetryEvent(() -> new RetryOnIgnoredErrorEvent(getName(), throwable));
					return -1;
				}
				return handleOnError(throwable.getCause());
			}
			if (!exceptionPredicate.test(throwable)) {
				failedWithoutRetryCounter.increment();
				publishRetryEvent(() -> new RetryOnIgnoredErrorEvent(getName(), throwable));
				return -1;
			}
			return handleOnError(throwable);

		}

		private long handleOnError(Throwable throwable) {
			lastException.set(throwable);
			int attempt = numOfAttempts.incrementAndGet();
			if (attempt >= maxAttempts) {
				failedAfterRetryCounter.increment();
				publishRetryEvent(() -> new RetryOnErrorEvent(name, attempt, throwable));
				return -1;
			}

			long interval = intervalFunction.apply(attempt);
			publishRetryEvent(() -> new RetryOnRetryEvent(getName(), attempt, throwable, interval));
			return interval;
		}

		@Override
		public long onResult(T result) {
			if (null != resultPredicate && resultPredicate.test(result)) {
				int attempt = numOfAttempts.incrementAndGet();
				if (attempt >= maxAttempts) {
					return -1;
				}
				return intervalFunction.apply(attempt);
			} else {
				return -1;
			}
		}
	}

	public final class RetryMetrics implements Metrics {
		private RetryMetrics() {
		}

		@Override
		public long getNumberOfSuccessfulCallsWithoutRetryAttempt() {
			return succeededWithoutRetryCounter.longValue();
		}

		@Override
		public long getNumberOfFailedCallsWithoutRetryAttempt() {
			return failedWithoutRetryCounter.longValue();
		}

		@Override
		public long getNumberOfSuccessfulCallsWithRetryAttempt() {
			return succeededAfterRetryCounter.longValue();
		}

		@Override
		public long getNumberOfFailedCallsWithRetryAttempt() {
			return failedAfterRetryCounter.longValue();
		}
	}

	private class RetryEventProcessor extends EventProcessor<RetryEvent> implements EventConsumer<RetryEvent>, EventPublisher {

		@Override
		public void consumeEvent(RetryEvent event) {
			super.processEvent(event);
		}

		@Override
		public EventPublisher onRetry(EventConsumer<RetryOnRetryEvent> onRetryEventConsumer) {
			registerConsumer(RetryOnRetryEvent.class, onRetryEventConsumer);
			return this;
		}

		@Override
		public EventPublisher onSuccess(EventConsumer<RetryOnSuccessEvent> onSuccessEventConsumer) {
			registerConsumer(RetryOnSuccessEvent.class, onSuccessEventConsumer);
			return this;
		}

		@Override
		public EventPublisher onError(EventConsumer<RetryOnErrorEvent> onErrorEventConsumer) {
			registerConsumer(RetryOnErrorEvent.class, onErrorEventConsumer);
			return this;
		}

		@Override
		public EventPublisher onIgnoredError(EventConsumer<RetryOnIgnoredErrorEvent> onIgnoredErrorEventConsumer) {
			registerConsumer(RetryOnIgnoredErrorEvent.class, onIgnoredErrorEventConsumer);
			return this;
		}
	}
}
