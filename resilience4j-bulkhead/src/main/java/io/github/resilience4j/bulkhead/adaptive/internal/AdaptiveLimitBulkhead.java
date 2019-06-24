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

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.LimitAdapter;
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
	private static final String CONFIG_MUST_NOT_BE_NULL = "Config must not be null";
	// initialization constants
	private final String name;
	private final AdaptiveBulkheadConfig adaptationConfig;

	private final InternalMetrics metrics;
	// internal bulkhead that is thread safe;
	private final SemaphoreBulkhead bulkhead;
	private final AdaptiveBulkheadEventProcessor eventProcessor;

	// current settings and measurements that you can read concurrently to expose metrics
	private volatile BulkheadConfig currentConfig; // immutable object
	// default limiter
	private final AtomicReference<MovingAverageLimitAdapter> DEFAULT_LIMITER;
	@Nullable
	private final AtomicReference<LimitAdapter> limitAdapter;


	public AdaptiveLimitBulkhead(@NonNull String name, @NonNull AdaptiveBulkheadConfig config) {
		this(name, config, null);
	}

	public AdaptiveLimitBulkhead(@NonNull String name, @NonNull AdaptiveBulkheadConfig config, @Nullable LimitAdapter limitAdapter) {
		this.name = name;
		if (limitAdapter != null) {
			this.limitAdapter = new AtomicReference<>(limitAdapter);
		} else {
			this.limitAdapter = null;
		}
		this.adaptationConfig = requireNonNull(config, CONFIG_MUST_NOT_BE_NULL);
		final long roundedValue = round(config.getDesirableAverageThroughput() * config.getDesirableOperationLatency());
		int initialConcurrency = ((int) roundedValue) > 0 ? (int) roundedValue : 1;
		this.currentConfig = BulkheadConfig.custom()
				.maxConcurrentCalls(initialConcurrency)
				.maxWaitDuration(Duration.ofMillis(0))
				.build();
		bulkhead = new SemaphoreBulkhead(name + "-internal", this.currentConfig);
		metrics = new InternalMetrics();
		eventProcessor = new AdaptiveBulkheadEventProcessor();
		DEFAULT_LIMITER = new AtomicReference<>(new MovingAverageLimitAdapter(adaptationConfig, this::publishBulkheadEvent));
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
		if (limitAdapter != null) {
			limitAdapter.getAndUpdate(limitAdapter1 -> {
				limitAdapter1.adaptLimitIfAny(bulkhead, callTime);
				return limitAdapter1;
			});
		} else {
			DEFAULT_LIMITER.getAndUpdate(limitAdapter1 -> {
				limitAdapter1.adaptLimitIfAny(bulkhead, callTime);
				return limitAdapter1;
			});
		}
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
			return limitAdapter != null ? limitAdapter.get().getMaxLatencyMillis() : DEFAULT_LIMITER.get().getMaxLatencyMillis();
		}

		@Override
		public double getAverageLatencyMillis() {
			return limitAdapter != null ? limitAdapter.get().getAverageLatencyMillis() / 1e6 : DEFAULT_LIMITER.get().getAverageLatencyMillis() / 1e6;
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


	private class AdaptiveBulkheadEventProcessor extends EventProcessor<BulkheadLimit> implements AdaptiveEventPublisher, EventConsumer<BulkheadLimit> {

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

	private void publishBulkheadEvent(BulkheadLimit eventSupplier) {
		if (eventProcessor.hasConsumers()) {
			eventProcessor.consumeEvent(eventSupplier);
		}
	}

}
