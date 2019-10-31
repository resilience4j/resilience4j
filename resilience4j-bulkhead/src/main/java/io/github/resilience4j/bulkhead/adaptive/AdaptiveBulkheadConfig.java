/*
 *
 *  Copyright 2019: Bohdan Storozhuk, Mahmoud Romeh
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

import io.github.resilience4j.bulkhead.adaptive.internal.AdaptiveLimitBulkhead;
import io.github.resilience4j.bulkhead.adaptive.internal.config.AbstractConfig;
import io.github.resilience4j.bulkhead.adaptive.internal.config.AimdConfig;
import io.github.resilience4j.core.lang.NonNull;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.core.predicate.PredicateCreator;

import java.util.function.Predicate;

/**
 * A {@link AdaptiveBulkheadConfig} configures a adaptation capabilities of  {@link AdaptiveLimitBulkhead}
 */
public class AdaptiveBulkheadConfig<T extends AbstractConfig> {
	private static final Predicate<Throwable> DEFAULT_RECORD_EXCEPTION_PREDICATE = throwable -> true;
	private static final Predicate<Throwable> DEFAULT_IGNORE_EXCEPTION_PREDICATE = throwable -> false;
	// The default exception predicate counts all exceptions as failures.
	@NonNull
	private Predicate<Throwable> recordExceptionPredicate = DEFAULT_RECORD_EXCEPTION_PREDICATE;
	// The default exception predicate ignores no exceptions.
	@NonNull
	private Predicate<Throwable> ignoreExceptionPredicate = DEFAULT_IGNORE_EXCEPTION_PREDICATE;
	@Nullable
	private T config;
	private int initialConcurrency = 1;
	private static final boolean DEFAULT_WRITABLE_STACK_TRACE_ENABLED = true;
	private boolean writableStackTraceEnabled = DEFAULT_WRITABLE_STACK_TRACE_ENABLED;

	private AdaptiveBulkheadConfig() {
	}

	public Predicate<Throwable> getRecordExceptionPredicate() {
		return recordExceptionPredicate;
	}

	public Predicate<Throwable> getIgnoreExceptionPredicate() {
		return ignoreExceptionPredicate;
	}

	public int getInitialConcurrency() {
		return initialConcurrency;
	}

	public T getConfiguration() {
		return config;
	}

	public boolean isWritableStackTraceEnabled() {
		return writableStackTraceEnabled;
	}

	/**
	 * Returns a builder to create a custom AdaptiveBulkheadConfig.
	 *
	 * @return a {@link AdaptiveBulkheadConfig.Builder}
	 */
	public static <T extends AbstractConfig> Builder<T> from(AdaptiveBulkheadConfig<T> baseConfig) {
		return AdaptiveBulkheadConfig.builder(baseConfig);
	}

	/**
	 * Creates a default Bulkhead configuration.
	 *
	 * @return a default Bulkhead configuration.
	 */
	public static AdaptiveBulkheadConfig<AimdConfig> ofDefaults() {
		return AdaptiveBulkheadConfig.<AimdConfig>builder().config(AimdConfig.builder().build()).build();
	}

	/**
	 * Returns a builder to create a custom AdaptiveBulkheadConfig.
	 *
	 * @return a {@link AdaptiveBulkheadConfig.Builder}
	 */
	public static <T extends AbstractConfig> Builder<T> builder() {
		return new Builder<>();
	}

	/**
	 * Returns a builder to create a custom AdaptiveBulkheadConfig.
	 *
	 * @return a {@link AdaptiveBulkheadConfig.Builder}
	 */
	public static <T extends AbstractConfig> Builder<T> builder(AdaptiveBulkheadConfig<T> bulkheadConfig) {
		return new Builder<>(bulkheadConfig);
	}

	public static class Builder<T extends AbstractConfig> {
		private final AdaptiveBulkheadConfig<T> adaptiveBulkheadConfig;
		@Nullable
		private Predicate<Throwable> recordExceptionPredicate;
		@Nullable
		private Predicate<Throwable> ignoreExceptionPredicate;

		@SuppressWarnings("unchecked")
		private Class<? extends Throwable>[] recordExceptions = new Class[0];
		@SuppressWarnings("unchecked")
		private Class<? extends Throwable>[] ignoreExceptions = new Class[0];

		private Builder() {
			adaptiveBulkheadConfig = new AdaptiveBulkheadConfig<>();
		}

		private Builder(AdaptiveBulkheadConfig<T> adaptiveBulkheadConfig) {
			this.adaptiveBulkheadConfig = adaptiveBulkheadConfig;
		}

		/**
		 * Enables writable stack traces. When set to false, {@link Exception#getStackTrace()} returns a zero length array.
		 * This may be used to reduce log spam when the circuit breaker is open as the cause of the exceptions is already
		 * known (the circuit breaker is short-circuiting calls).
		 *
		 * @param writableStackTraceEnabled flag to control if stack trace is writable
		 * @return the BulkheadConfig.Builder
		 */
		public final Builder<T> writableStackTraceEnabled(boolean writableStackTraceEnabled) {
			adaptiveBulkheadConfig.writableStackTraceEnabled = writableStackTraceEnabled;
			return this;
		}


		/**
		 * @param config the custom config object
		 * @return the BulkheadConfig.Builder
		 */
		public final Builder<T> config(T config) {
			adaptiveBulkheadConfig.config = config;
			return this;
		}

		/**
		 * @param initialConcurrency initial bulkhead allowed concurrent calls
		 * @return the BulkheadConfig.Builder
		 */
		public final Builder<T> initialConcurrency(int initialConcurrency) {
			adaptiveBulkheadConfig.initialConcurrency = initialConcurrency;
			return this;
		}

		/**
		 * Configures a Predicate which evaluates if an exception should be ignored and neither count as a failure nor success.
		 * The Predicate must return true if the exception must be ignored .
		 * The Predicate must return false, if the exception must count as a failure.
		 *
		 * @param predicate the Predicate which checks if an exception should count as a failure
		 * @return the Builder
		 */
		public final Builder ignoreException(Predicate<Throwable> predicate) {
			this.ignoreExceptionPredicate = predicate;
			return this;
		}

		/**
		 * Configures a list of error classes that are recorded as a failure and thus increase the failure rate.
		 * an exception matching or inheriting from one of the list should count as a failure
		 *
		 * @param errorClasses the error classes which are recorded
		 * @return the Builder
		 * @see #ignoreExceptions(Class[]) ). Ignoring an exception has more priority over recording an exception.
		 */
		@SuppressWarnings("unchecked")
		@SafeVarargs
		public final Builder recordExceptions(@Nullable Class<? extends Throwable>... errorClasses) {
			this.recordExceptions = errorClasses != null ? errorClasses : new Class[0];
			return this;
		}

		/**
		 * Configures a Predicate which evaluates if an exception should be recorded as a failure and thus increase the failure rate.
		 * The Predicate must return true if the exception should count as a failure. The Predicate must return false, if the exception should count as a success
		 * ,unless the exception is explicitly ignored by {@link #ignoreExceptions(Class[])} or {@link #ignoreException(Predicate)}.
		 *
		 * @param predicate the Predicate which checks if an exception should count as a failure
		 * @return the Builder
		 */
		public final Builder recordException(Predicate<Throwable> predicate) {
			this.recordExceptionPredicate = predicate;
			return this;
		}

		/**
		 * Configures a list of error classes that are ignored and thus neither count as a failure nor success.
		 * an exception matching or inheriting from one of that list will not count as a failure nor success
		 *
		 * @param errorClasses the error classes which are ignored
		 * @return the Builder
		 * @see #recordExceptions(Class[]) . Ignoring an exception has priority over recording an exception.
		 */
		@SuppressWarnings("unchecked")
		@SafeVarargs
		public final Builder ignoreExceptions(@Nullable Class<? extends Throwable>... errorClasses) {
			this.ignoreExceptions = errorClasses != null ? errorClasses : new Class[0];
			return this;
		}

		public AdaptiveBulkheadConfig<T> build() {
			adaptiveBulkheadConfig.recordExceptionPredicate = createRecordExceptionPredicate();
			adaptiveBulkheadConfig.ignoreExceptionPredicate = createIgnoreFailurePredicate();
			if (adaptiveBulkheadConfig.config != null) {
				return adaptiveBulkheadConfig;
			} else {
				throw new IllegalArgumentException("target config object can not be NULL");
			}

		}


		private Predicate<Throwable> createIgnoreFailurePredicate() {
			return PredicateCreator.createExceptionsPredicate(ignoreExceptions)
					.map(predicate -> ignoreExceptionPredicate != null ? predicate.or(ignoreExceptionPredicate) : predicate)
					.orElseGet(() -> ignoreExceptionPredicate != null ? ignoreExceptionPredicate : DEFAULT_IGNORE_EXCEPTION_PREDICATE);
		}

		private Predicate<Throwable> createRecordExceptionPredicate() {
			return PredicateCreator.createExceptionsPredicate(recordExceptions)
					.map(predicate -> recordExceptionPredicate != null ? predicate.or(recordExceptionPredicate) : predicate)
					.orElseGet(() -> recordExceptionPredicate != null ? recordExceptionPredicate : DEFAULT_RECORD_EXCEPTION_PREDICATE);
		}

	}

}
