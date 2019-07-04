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

import static java.lang.Math.round;
import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveStrategy;
import io.github.resilience4j.bulkhead.adaptive.LimitAdapter;
import io.github.resilience4j.bulkhead.adaptive.internal.config.MovingAverageConfig;
import io.github.resilience4j.bulkhead.adaptive.internal.config.PercentileConfig;
import io.github.resilience4j.bulkhead.adaptive.internal.movingAverage.MovingAverageLimitAdapter;
import io.github.resilience4j.bulkhead.adaptive.internal.percentile.PercentileLimitAdapter;
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
	// current settings and measurements that you can read concurrently to expose metrics
	private BulkheadConfig currentConfig; // immutable object
	@NonNull
	private final AtomicReference<? extends LimitAdapter<Bulkhead>> limitAdapter;

	private AdaptiveLimitBulkhead(@NonNull String name, @NonNull AdaptiveBulkheadConfig<?> config, @Nullable LimitAdapter<Bulkhead> limitAdapter, BulkheadConfig bulkheadConfig) {
		this.name = name;
		this.limitAdapter = new AtomicReference<>(limitAdapter);
		this.adaptationConfig = config;
		this.currentConfig = bulkheadConfig;
		bulkhead = new SemaphoreBulkhead(name + "-internal", this.currentConfig);
		metrics = new InternalMetrics();
	}

	@Override
	public boolean tryAcquirePermission() {
		return bulkhead.tryAcquirePermission();
	}

	@Override
	public void acquirePermission() {
		bulkhead.acquirePermission();
	}

	@Override
	public void releasePermission() {
		bulkhead.releasePermission();
	}

	@Override
	public void onComplete(Duration callTime) {
		bulkhead.onComplete();
		limitAdapter.getAndUpdate(limitAdapter1 -> {
			limitAdapter1.adaptLimitIfAny(bulkhead, callTime);
			return limitAdapter1;
		});

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
		public double getMaxLatencyMillis() {
			return limitAdapter.get().getMaxLatencyMillis();
		}

		@Override
		public double getAverageLatencyMillis() {
			return limitAdapter.get().getMeasuredLatencyMillis() / 1e6;
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
		private AdaptiveBulkheadConfig<?> adaptationConfig;
		// current settings and measurements that you can read concurrently to expose metrics
		private BulkheadConfig currentConfig; // immutable object
		@Nullable
		private LimitAdapter<Bulkhead> limitAdapter;

		private AdaptiveBulkheadFactory() {
		}

		public AdaptiveLimitBulkhead createAdaptiveLimitBulkhead(@NonNull String name, @NonNull AdaptiveBulkheadConfig<?> config, @NonNull AdaptiveStrategy adaptiveStrategy) {
			return createAdaptiveLimitBulkhead(name, config, null, adaptiveStrategy);
		}

		public AdaptiveLimitBulkhead createAdaptiveLimitBulkhead(@NonNull String name, @NonNull AdaptiveBulkheadConfig<?> config) {
			return createAdaptiveLimitBulkhead(name, config, AdaptiveStrategy.MOVING_AVERAGE);
		}

		public AdaptiveLimitBulkhead createAdaptiveLimitBulkhead(@NonNull String name, @NonNull AdaptiveBulkheadConfig<?> config, @NonNull LimitAdapter<Bulkhead> limitAdapter) {
			return createAdaptiveLimitBulkhead(name, config, limitAdapter, null);
		}

		public AdaptiveLimitBulkhead createAdaptiveLimitBulkhead(@NonNull String name, @NonNull AdaptiveBulkheadConfig<?> config, @Nullable LimitAdapter<Bulkhead> limitAdapter, @Nullable AdaptiveStrategy adaptiveStrategy) {
			long roundedValue = 0;
			if (limitAdapter != null) {
				this.limitAdapter = limitAdapter;
			} else {
				this.limitAdapter = null;
			}
			if (limitAdapter == null && adaptiveStrategy != null && adaptiveStrategy.equals(AdaptiveStrategy.PERCENTILE)) {
				@SuppressWarnings("unchecked")
				AdaptiveBulkheadConfig<PercentileConfig> percentileConfig = (AdaptiveBulkheadConfig<PercentileConfig>) config;
				roundedValue = round(percentileConfig.getConfiguration().getDesirableAverageThroughput() * percentileConfig.getConfiguration().getDesirableOperationLatency());
				this.limitAdapter = new PercentileLimitAdapter(percentileConfig, AdaptiveBulkheadFactory::publishBulkheadEvent);
			} else if (limitAdapter == null) {
				// default to moving average
				@SuppressWarnings("unchecked")
				AdaptiveBulkheadConfig<MovingAverageConfig> movingAverageConfig = (AdaptiveBulkheadConfig<MovingAverageConfig>) config;
				roundedValue = round(movingAverageConfig.getConfiguration().getDesirableAverageThroughput() * movingAverageConfig.getConfiguration().getDesirableOperationLatency());
				this.limitAdapter = new MovingAverageLimitAdapter(movingAverageConfig, AdaptiveBulkheadFactory::publishBulkheadEvent);
			}

			this.adaptationConfig = requireNonNull(config, CONFIG_MUST_NOT_BE_NULL);
			int initialConcurrency = roundedValue != 0 ? ((int) roundedValue) > 0 ? (int) roundedValue : 1 : adaptationConfig.getInitialConcurrency();
			this.currentConfig = BulkheadConfig.custom()
					.maxConcurrentCalls(initialConcurrency)
					.maxWaitDuration(Duration.ofMillis(0))
					.build();

			return new AdaptiveLimitBulkhead(name, config, this.limitAdapter, this.currentConfig);
		}

		private static void publishBulkheadEvent(BulkheadLimit eventSupplier) {
			if (eventProcessor.hasConsumers()) {
				eventProcessor.consumeEvent(eventSupplier);
			}
		}

	}


}
