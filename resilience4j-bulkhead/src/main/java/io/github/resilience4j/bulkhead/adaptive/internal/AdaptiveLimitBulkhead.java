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
package io.github.resilience4j.bulkhead.adaptive.internal;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.LimitPolicy;
import io.github.resilience4j.bulkhead.adaptive.internal.amid.AIMDLimiter;
import io.github.resilience4j.bulkhead.adaptive.internal.config.AIMDConfig;
import io.github.resilience4j.bulkhead.event.BulkheadLimit;
import io.github.resilience4j.bulkhead.event.BulkheadOnLimitDecreasedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnLimitIncreasedEvent;
import io.github.resilience4j.bulkhead.internal.SemaphoreBulkhead;
import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.core.EventPublisher;
import io.github.resilience4j.core.lang.NonNull;
import io.github.resilience4j.core.lang.Nullable;

public class AdaptiveLimitBulkhead implements AdaptiveBulkhead {
	private final static AdaptiveBulkheadEventProcessor eventProcessor = new AdaptiveBulkheadEventProcessor();
	private final String name;
	private final AdaptiveBulkheadConfig adaptationConfig;
	private final InternalMetrics metrics;
	private final SemaphoreBulkhead bulkhead;
	private final AtomicInteger inFlight = new AtomicInteger();
	// current settings and measurements that you can read concurrently to expose metrics
	private final BulkheadConfig currentConfig; // immutable object
	@NonNull
	private final LimitPolicy<Bulkhead> limitAdapter;

	private AdaptiveLimitBulkhead(@NonNull String name, @NonNull AdaptiveBulkheadConfig config, @NonNull LimitPolicy<Bulkhead> limitAdapter, BulkheadConfig bulkheadConfig) {
		this.name = name;
		this.limitAdapter = limitAdapter;
		this.adaptationConfig = config;
		this.currentConfig = bulkheadConfig;
		bulkhead = new SemaphoreBulkhead(name + "-internal", this.currentConfig);
		metrics = new InternalMetrics();
	}

	@Override
	public void increaseInProcessingRequestsCount() {
		inFlight.incrementAndGet();
	}

	@Override
	public void decreaseInProcessingRequestsCount() {
		inFlight.decrementAndGet();
	}

	@Override
	public int getCurrentInProcessingRequestsCount() {
		return inFlight.get();
	}

	@Override
	public boolean tryAcquirePermission() {
		boolean isAcquire = bulkhead.tryAcquirePermission();
		if (isAcquire) {
			increaseInProcessingRequestsCount();
		}
		return isAcquire;
	}

	@Override
	public void acquirePermission() {
		bulkhead.acquirePermission();
		increaseInProcessingRequestsCount();
	}

	@Override
	public void releasePermission() {
		bulkhead.releasePermission();
		decreaseInProcessingRequestsCount();
	}

	@Override
	public void onComplete(Duration callTime, boolean isSuccess) {
		bulkhead.onComplete();
		limitAdapter.adaptLimitIfAny(bulkhead, callTime, isSuccess, inFlight.getAndDecrement());
	}

	public String getName() {
		return name;
	}

	@Override
	public AdaptiveBulkheadConfig getBulkheadConfig() {
		return adaptationConfig;
	}

	@Override
	public AdaptiveBulkheadMetrics getMetrics() {
		return metrics;
	}

	@Override
	public AdaptiveEventPublisher getEventPublisher() {
		return eventProcessor;
	}


	private final class InternalMetrics implements AdaptiveBulkheadMetrics {

		@Override
		public double getFailureRate() {
			return limitAdapter.getMetrics().getSnapshot().getFailureRate();
		}

		@Override
		public double getSlowCallRate() {
			return limitAdapter.getMetrics().getSnapshot().getSlowCallRate();
		}

		@Override
		public double getAverageLatencyMillis() {
			return limitAdapter.getMetrics().getSnapshot().getAverageDuration().toMillis();
		}

		@Override
		public int getAvailableConcurrentCalls() {
			return bulkhead.getMetrics().getAvailableConcurrentCalls();
		}

		@Override
		public int getMaxAllowedConcurrentCalls() {
			return currentConfig.getMaxConcurrentCalls();
		}
	}


	private static class AdaptiveBulkheadEventProcessor extends EventProcessor<BulkheadLimit> implements AdaptiveEventPublisher, EventConsumer<BulkheadLimit> {

		@Override
		public EventPublisher onLimitIncreased(EventConsumer<BulkheadOnLimitDecreasedEvent> eventConsumer) {
			registerConsumer(BulkheadOnLimitIncreasedEvent.class.getSimpleName(), eventConsumer);
			return this;
		}

		@Override
		public EventPublisher onLimitDecreased(EventConsumer<BulkheadOnLimitIncreasedEvent> eventConsumer) {
			registerConsumer(BulkheadOnLimitDecreasedEvent.class.getSimpleName(), eventConsumer);
			return this;
		}

		@Override
		public void consumeEvent(BulkheadLimit event) {
			super.processEvent(event);
		}
	}

	@Override
	public String toString() {
		return String.format("AdaptiveBulkhead '%s'", this.name);
	}


	public static AdaptiveBulkheadFactory factory() {
		return new AdaptiveBulkheadFactory();
	}


	/**
	 * the adaptive bulkhead factory class
	 */
	public static class AdaptiveBulkheadFactory {
		private static final String CONFIG_MUST_NOT_BE_NULL = "Config must not be null";

		private AdaptiveBulkheadFactory() {
		}

		public AdaptiveLimitBulkhead createAdaptiveLimitBulkhead(@NonNull String name, @NonNull AdaptiveBulkheadConfig config) {
			return createAdaptiveLimitBulkhead(name, config, null);
		}


		public AdaptiveLimitBulkhead createAdaptiveLimitBulkhead(@NonNull String name, @NonNull AdaptiveBulkheadConfig config, @Nullable LimitPolicy<Bulkhead> customLimitAdapter) {
			LimitPolicy<Bulkhead> limitAdapter = null;
			requireNonNull(config, CONFIG_MUST_NOT_BE_NULL);
			int minLimit = 0;
			if (customLimitAdapter != null) {
				limitAdapter = customLimitAdapter;
			}
			if (limitAdapter == null) {
				// default to AIMD limiter
				@SuppressWarnings("unchecked")
				AdaptiveBulkheadConfig<AIMDConfig> aimdConfig = (AdaptiveBulkheadConfig<AIMDConfig>) config;
				minLimit = aimdConfig.getConfiguration().getMinLimit();
				limitAdapter = new AIMDLimiter(aimdConfig, AdaptiveLimitBulkhead::publishBulkheadEvent);
			}

			int initialConcurrency = calculateConcurrency(minLimit, config);
			BulkheadConfig currentConfig = BulkheadConfig.custom()
					.maxConcurrentCalls(initialConcurrency)
					.maxWaitDuration(Duration.ofMillis(0))
					.build();

			return new AdaptiveLimitBulkhead(name, config, limitAdapter, currentConfig);
		}

		private int calculateConcurrency(int minLimit, AdaptiveBulkheadConfig config) {
			if (minLimit != 0) {
				return (minLimit) > 0 ? minLimit : 1;
			} else {
				return config.getInitialConcurrency();
			}
		}


	}

	public static void publishBulkheadEvent(BulkheadLimit eventSupplier) {
		if (eventProcessor.hasConsumers()) {
			eventProcessor.consumeEvent(eventSupplier);
		}
	}

}
