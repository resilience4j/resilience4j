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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.LimitPolicy;
import io.github.resilience4j.bulkhead.adaptive.LimitResult;
import io.github.resilience4j.bulkhead.adaptive.internal.amid.AimdLimiter;
import io.github.resilience4j.bulkhead.adaptive.internal.config.AimdConfig;
import io.github.resilience4j.bulkhead.event.BulkheadLimit;
import io.github.resilience4j.bulkhead.event.BulkheadOnLimitDecreasedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnLimitIncreasedEvent;
import io.github.resilience4j.bulkhead.internal.SemaphoreBulkhead;
import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.core.EventPublisher;
import io.github.resilience4j.core.lang.NonNull;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.core.metrics.FixedSizeSlidingWindowMetrics;
import io.github.resilience4j.core.metrics.SlidingTimeWindowMetrics;
import io.github.resilience4j.core.metrics.Snapshot;

public class AdaptiveLimitBulkhead implements AdaptiveBulkhead {
	private static final Logger LOG = LoggerFactory.getLogger(AdaptiveLimitBulkhead.class);
	private static final AdaptiveBulkheadEventProcessor eventProcessor = new AdaptiveBulkheadEventProcessor();
	private final String name;
	private final AdaptiveBulkheadConfig adaptationConfig;
	private final InternalMetrics metrics;
	private final Bulkhead bulkhead;
	private final AtomicInteger inFlight = new AtomicInteger();
	// current settings and measurements that you can read concurrently to expose metrics
	private final BulkheadConfig currentConfig; // immutable object
	@NonNull
	private final LimitPolicy limitAdapter;
	@NonNull
	private final io.github.resilience4j.core.metrics.Metrics recordMetrics;

	private AdaptiveLimitBulkhead(@NonNull String name, @NonNull AdaptiveBulkheadConfig config, @NonNull LimitPolicy limitAdapter, @NonNull BulkheadConfig bulkheadConfig, io.github.resilience4j.core.metrics.Metrics recordMetrics) {
		this.name = name;
		this.limitAdapter = limitAdapter;
		this.adaptationConfig = config;
		this.currentConfig = bulkheadConfig;
		bulkhead = new SemaphoreBulkhead(name + "-internal", this.currentConfig);
		metrics = new InternalMetrics();
		this.recordMetrics = recordMetrics;
	}

	@Override
	public boolean tryAcquirePermission() {
		boolean isAcquire = bulkhead.tryAcquirePermission();
		if (isAcquire) {
			inFlight.incrementAndGet();
		}
		return isAcquire;
	}

	@Override
	public void acquirePermission() {
		bulkhead.acquirePermission();
		inFlight.incrementAndGet();
	}

	@Override
	public void releasePermission() {
		bulkhead.releasePermission();
		inFlight.decrementAndGet();
	}

	@Override
	public void onSuccess(long callTime, TimeUnit durationUnit) {
		bulkhead.onComplete();
		final LimitResult limitResult = record(callTime, true, inFlight.getAndDecrement());
		adoptLimit(bulkhead, limitResult.getLimit(), limitResult.waitTime());
	}

	@Override
	public void onError(long callTime, TimeUnit durationUnit) {
		bulkhead.onComplete();
		final LimitResult limitResult = record(callTime, false, inFlight.getAndDecrement());
		adoptLimit(bulkhead, limitResult.getLimit(), limitResult.waitTime());
	}


	@Override
	public AdaptiveBulkheadConfig getBulkheadConfig() {
		return adaptationConfig;
	}

	@Override
	public Metrics getMetrics() {
		return metrics;
	}

	@Override
	public AdaptiveEventPublisher getEventPublisher() {
		return eventProcessor;
	}

	@Override
	public String getName() {
		return name;
	}

	private final class InternalMetrics implements Metrics {

		@Override
		public double getFailureRate() {
			return recordMetrics.getSnapshot().getFailureRate();
		}

		@Override
		public double getSlowCallRate() {
			return recordMetrics.getSnapshot().getSlowCallRate();
		}

		@Override
		public int getNumberOfSlowCalls() {
			return recordMetrics.getSnapshot().getTotalNumberOfSlowCalls();
		}

		@Override
		public int getNumberOfFailedCalls() {
			return recordMetrics.getSnapshot().getNumberOfFailedCalls();
		}

		@Override
		public int getNumberOfSuccessfulCalls() {
			return recordMetrics.getSnapshot().getNumberOfSuccessfulCalls();
		}

		@Override
		public double getAverageLatencyMillis() {
			return recordMetrics.getSnapshot().getAverageDuration().toMillis();
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


		public AdaptiveLimitBulkhead createAdaptiveLimitBulkhead(@NonNull String name, @NonNull AdaptiveBulkheadConfig config, @Nullable LimitPolicy customLimitAdapter) {
			LimitPolicy limitAdapter = null;
			io.github.resilience4j.core.metrics.Metrics metrics;
			requireNonNull(config, CONFIG_MUST_NOT_BE_NULL);
			int minLimit = 0;
			if (customLimitAdapter != null) {
				limitAdapter = customLimitAdapter;
			}
			if (config.getConfiguration().getSlidingWindowType() == AimdConfig.SlidingWindow.COUNT_BASED) {
				metrics = new FixedSizeSlidingWindowMetrics(config.getConfiguration().getSlidingWindowSize());
			} else {
				metrics = new SlidingTimeWindowMetrics(config.getConfiguration().getSlidingWindowTime());
			}
			if (limitAdapter == null) {
				// default to AIMD limiter
				@SuppressWarnings("unchecked")
				AdaptiveBulkheadConfig<AimdConfig> aimdConfig = (AdaptiveBulkheadConfig<AimdConfig>) config;
				minLimit = aimdConfig.getConfiguration().getMinLimit();
				limitAdapter = new AimdLimiter(aimdConfig);
			}

			int initialConcurrency = calculateConcurrency(minLimit, config);
			BulkheadConfig currentConfig = BulkheadConfig.custom()
					.maxConcurrentCalls(initialConcurrency)
					.maxWaitDuration(Duration.ofMillis(0))
					.build();

			return new AdaptiveLimitBulkhead(name, config, limitAdapter, currentConfig, metrics);
		}

		private int calculateConcurrency(int minLimit, AdaptiveBulkheadConfig config) {
			if (minLimit != 0) {
				return (minLimit) > 0 ? minLimit : 1;
			} else {
				return config.getInitialConcurrency();
			}
		}


	}

	private void publishBulkheadEvent(BulkheadLimit eventSupplier) {
		if (eventProcessor.hasConsumers()) {
			eventProcessor.consumeEvent(eventSupplier);
		}
	}


	/**
	 * @param callTime  the call duration
	 * @param isSuccess is the call successful or not
	 * @param inFlight  current in flight calls
	 * @return the update limit result DTO @{@link LimitResult}
	 */
	protected LimitResult record(@NonNull long callTime, boolean isSuccess, int inFlight) {

		if (LOG.isDebugEnabled()) {
			LOG.debug("starting the adation of the limit for callTime :{} , isSuccess: {}, inFlight: {}", callTime, isSuccess, inFlight);
		}
		Snapshot snapshot;
		final long callTimeNanos = TimeUnit.MILLISECONDS.toNanos(callTime);
		final long desirableLatency = adaptationConfig.getConfiguration().getDesirableLatency().toNanos();
		if (isSuccess) {
			if (callTimeNanos > desirableLatency) {
				snapshot = recordMetrics.record(callTimeNanos, TimeUnit.NANOSECONDS, io.github.resilience4j.core.metrics.Metrics.Outcome.SLOW_SUCCESS);
			} else {
				snapshot = recordMetrics.record(callTimeNanos, TimeUnit.NANOSECONDS, io.github.resilience4j.core.metrics.Metrics.Outcome.SUCCESS);
			}
		} else {
			if (callTimeNanos > desirableLatency) {
				snapshot = recordMetrics.record(callTimeNanos, TimeUnit.NANOSECONDS, io.github.resilience4j.core.metrics.Metrics.Outcome.SLOW_ERROR);
			} else {
				snapshot = recordMetrics.record(callTimeNanos, TimeUnit.NANOSECONDS, io.github.resilience4j.core.metrics.Metrics.Outcome.ERROR);
			}
		}
		return limitAdapter.adaptLimitIfAny(snapshot, inFlight);
	}

	/**
	 * adopt the limit based into the new calculated average
	 *
	 * @param bulkhead       the target semaphore bulkhead
	 * @param updatedLimit   calculated new limit
	 * @param waitTimeMillis new wait time
	 */
	private void adoptLimit(Bulkhead bulkhead, int updatedLimit, long waitTimeMillis) {
		if (bulkhead.getBulkheadConfig().getMaxConcurrentCalls() < updatedLimit) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("increasing bulkhead limit by increasing the max concurrent calls for {}", updatedLimit);
			}
			final BulkheadConfig updatedConfig = BulkheadConfig.custom()
					.maxConcurrentCalls(updatedLimit)
					.maxWaitDuration(Duration.ofMillis(waitTimeMillis))
					.build();
			bulkhead.changeConfig(updatedConfig);
			publishBulkheadEvent(new BulkheadOnLimitIncreasedEvent(bulkhead.getName().substring(0, bulkhead.getName().indexOf('-')),
					eventData(waitTimeMillis, updatedLimit)));
		} else if (bulkhead.getBulkheadConfig().getMaxConcurrentCalls() == updatedLimit) {
			// do nothing
		} else {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Dropping the bulkhead limit with new max concurrent calls {}", updatedLimit);
			}
			final BulkheadConfig updatedConfig = BulkheadConfig.custom()
					.maxConcurrentCalls(updatedLimit)
					.maxWaitDuration(Duration.ofMillis(waitTimeMillis))
					.build();
			bulkhead.changeConfig(updatedConfig);
			publishBulkheadEvent(new BulkheadOnLimitDecreasedEvent(bulkhead.getName().substring(0, bulkhead.getName().indexOf('-')),
					eventData(waitTimeMillis, updatedLimit)));
		}

	}

	/**
	 * @param waitTimeMillis        new wait time
	 * @param newMaxConcurrentCalls new max concurrent data
	 * @return map of kep value string of the event properties
	 */
	private Map<String, String> eventData(long waitTimeMillis, int newMaxConcurrentCalls) {
		Map<String, String> eventData = new HashMap<>();
		eventData.put("newMaxConcurrentCalls", String.valueOf(newMaxConcurrentCalls));
		eventData.put("newWaitTimeMillis", String.valueOf(waitTimeMillis));
		return eventData;
	}

}
