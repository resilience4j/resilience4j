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

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.LimitPolicy;
import io.github.resilience4j.bulkhead.adaptive.LimitResult;
import io.github.resilience4j.bulkhead.adaptive.internal.amid.AimdLimiter;
import io.github.resilience4j.bulkhead.event.*;
import io.github.resilience4j.bulkhead.internal.SemaphoreBulkhead;
import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.core.EventPublisher;
import io.github.resilience4j.core.lang.NonNull;
import io.github.resilience4j.core.metrics.Snapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.resilience4j.core.metrics.Metrics.Outcome;

public class AdaptiveBulkheadStateMachine implements AdaptiveBulkhead {

	private static final Logger LOG = LoggerFactory.getLogger(AdaptiveBulkheadStateMachine.class);

	private final String name;
    private final AtomicReference<AdaptiveBulkheadState> stateReference;
    private final AdaptiveBulkheadConfig adaptiveBulkheadConfig;
    private final AdaptiveBulkheadEventProcessor eventProcessor;
	private final AdaptiveBulkheadMetrics metrics;
	private final Bulkhead bulkhead;
	private final AtomicInteger inFlight = new AtomicInteger();
    private final LimitPolicy limitAdapter;

    public AdaptiveBulkheadStateMachine(@NonNull String name, @NonNull AdaptiveBulkheadConfig adaptiveBulkheadConfig) {
		this.name = name;
        this.adaptiveBulkheadConfig = Objects
            .requireNonNull(adaptiveBulkheadConfig, "Config must not be null");
        this.metrics = new AdaptiveBulkheadMetrics(adaptiveBulkheadConfig);
        this.stateReference = new AtomicReference<>(new SlowStartState(metrics));
        this.eventProcessor = new AdaptiveBulkheadEventProcessor();

        BulkheadConfig internalBulkheadConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(adaptiveBulkheadConfig.getInitialConcurrentCalls())
            .maxWaitDuration(adaptiveBulkheadConfig.getMaxWaitDuration())
            .build();

        this.limitAdapter = new AimdLimiter(adaptiveBulkheadConfig);
        this.bulkhead = new SemaphoreBulkhead(name + "-internal", internalBulkheadConfig);
	}

	@Override
	public boolean tryAcquirePermission() {
		boolean isAcquire = stateReference.get().tryAcquirePermission();
		if (isAcquire) {
			inFlight.incrementAndGet();
		}
		return isAcquire;
	}

	@Override
	public void acquirePermission() {
        stateReference.get().acquirePermission();
		inFlight.incrementAndGet();
	}

	@Override
	public void releasePermission() {
        stateReference.get().releasePermission();
		inFlight.decrementAndGet();
	}

	@Override
	public void onSuccess(long duration, TimeUnit durationUnit) {
        stateReference.get().onSuccess(duration, durationUnit);

        // TODO
		publishBulkheadEvent(new BulkheadOnSuccessEvent(shortName(bulkhead), Collections.emptyMap()));
		final LimitResult limitResult = record(duration, true, inFlight.getAndDecrement());
		adoptLimit(bulkhead, limitResult.getLimit(), limitResult.waitTime());
	}

	@Override
	public void onError(long start, TimeUnit durationUnit, Throwable throwable) {
		//noinspection unchecked
		if (adaptiveBulkheadConfig.getIgnoreExceptionPredicate().test(throwable)) {
            releasePermission();
			publishBulkheadEvent(new BulkheadOnIgnoreEvent(shortName(bulkhead), errorData(throwable)));
		} else if (adaptiveBulkheadConfig.getRecordExceptionPredicate().test(throwable) && start != 0) {
			Instant finish = Instant.now();
			this.handleError(Duration.between(Instant.ofEpochMilli(start), finish).toMillis(), durationUnit, throwable);
		} else {
			if (start != 0) {
				Instant finish = Instant.now();
				this.onSuccess(Duration.between(Instant.ofEpochMilli(start), finish).toMillis(), durationUnit);
			}
		}
	}

	@Override
	public AdaptiveBulkheadConfig getBulkheadConfig() {
		return adaptiveBulkheadConfig;
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


    private class SlowStartState implements AdaptiveBulkheadState {

        private final AdaptiveBulkheadMetrics adaptiveBulkheadMetrics;
        private final AtomicBoolean isSlowStart;

        SlowStartState(AdaptiveBulkheadMetrics adaptiveBulkheadMetrics) {
            this.adaptiveBulkheadMetrics = adaptiveBulkheadMetrics;
            this.isSlowStart = new AtomicBoolean(true);
        }

        /**
         * @return "always" true
         */
        @Override
        public boolean tryAcquirePermission() {
            return isSlowStart.get();
        }

        /**
         * Does not throw an exception, because the CircuitBreaker is closed.
         */
        @Override
        public void acquirePermission() {
            // noOp
        }

        @Override
        public void releasePermission() {
            // noOp
        }

        @Override
        public void onError(long duration, TimeUnit durationUnit, Throwable throwable) {
            // AdaptiveBulkheadMetrics is thread-safe
            checkIfThresholdsExceeded(adaptiveBulkheadMetrics.onError(duration, durationUnit));
        }

        @Override
        public void onSuccess(long duration, TimeUnit durationUnit) {
            // AdaptiveBulkheadMetrics is thread-safe
            checkIfThresholdsExceeded(adaptiveBulkheadMetrics.onSuccess(duration, durationUnit));
        }

        /**
         * Transitions to CONGESTION_AVOIDANCE state when thresholds have been exceeded.
         *
         * @param result the Result
         */
        private void checkIfThresholdsExceeded(AdaptiveBulkheadMetrics.Result result) {
            switch (result) {
                case BELOW_THRESHOLDS:
                    if (isSlowStart.get()) {
                        recalculateMaxConcurrentCalls(
                            adaptiveBulkheadConfig.getIncreaseMultiplier());
                    }
                    break;
                case ABOVE_THRESHOLDS:
                    if (isSlowStart.compareAndSet(true, false)) {
                        recalculateMaxConcurrentCalls(
                            adaptiveBulkheadConfig.getDecreaseMultiplier());
                        // TODO transition
                    }
                    break;
            }
        }

        private void recalculateMaxConcurrentCalls(float multiplier) {
            int maxConcurrentCalls = (int) Math.ceil(multiplier
                * bulkhead.getBulkheadConfig().getMaxConcurrentCalls());
            bulkhead.changeConfig(
                BulkheadConfig.from(bulkhead.getBulkheadConfig())
                    .maxConcurrentCalls(maxConcurrentCalls)
                    .build());
            if (multiplier > 1) {
                publishBulkheadOnLimitIncreasedEvent(maxConcurrentCalls);
            } else if (multiplier < 1) {
                publishBulkheadOnLimitDecreasedEvent(maxConcurrentCalls);
            }
        }

        private void publishBulkheadOnLimitIncreasedEvent(int maxConcurrentCalls) {
            publishBulkheadEvent(new BulkheadOnLimitIncreasedEvent(
                shortName(bulkhead),
                limitChangeEventData(
                    adaptiveBulkheadConfig.getMaxWaitDuration().toMillis(),
                    maxConcurrentCalls)));
        }

        private void publishBulkheadOnLimitDecreasedEvent(int maxConcurrentCalls) {
            publishBulkheadEvent(new BulkheadOnLimitDecreasedEvent(
                shortName(bulkhead),
                limitChangeEventData(
                    adaptiveBulkheadConfig.getMaxWaitDuration().toMillis(),
                    maxConcurrentCalls)));
        }

        /**
         * Get the state of the AdaptiveBulkhead
         */
        @Override
        public AdaptiveBulkhead.State getState() {
            return State.SLOW_START;
        }

        /**
         * Get metrics of the AdaptiveBulkhead
         */
        @Override
        public AdaptiveBulkheadMetrics getMetrics() {
            return adaptiveBulkheadMetrics;
        }
    }

    private static class AdaptiveBulkheadEventProcessor extends EventProcessor<AdaptiveBulkheadEvent> implements AdaptiveEventPublisher, EventConsumer<AdaptiveBulkheadEvent> {

		@Override
		public EventPublisher<?> onLimitIncreased(EventConsumer<BulkheadOnLimitDecreasedEvent> eventConsumer) {
			registerConsumer(BulkheadOnLimitIncreasedEvent.class.getSimpleName(), eventConsumer);
			return this;
		}

		@Override
		public EventPublisher<?> onLimitDecreased(EventConsumer<BulkheadOnLimitIncreasedEvent> eventConsumer) {
			registerConsumer(BulkheadOnLimitDecreasedEvent.class.getSimpleName(), eventConsumer);
			return this;
		}

		@Override
		public EventPublisher<?> onSuccess(EventConsumer<BulkheadOnSuccessEvent> eventConsumer) {
			registerConsumer(BulkheadOnSuccessEvent.class.getSimpleName(), eventConsumer);
			return this;
		}

		@Override
		public EventPublisher<?> onError(EventConsumer<BulkheadOnErrorEvent> eventConsumer) {
			registerConsumer(BulkheadOnErrorEvent.class.getSimpleName(), eventConsumer);
			return this;
		}

		@Override
		public EventPublisher<?> onIgnoredError(EventConsumer<BulkheadOnIgnoreEvent> eventConsumer) {
			registerConsumer(BulkheadOnIgnoreEvent.class.getSimpleName(), eventConsumer);
			return this;
		}

		@Override
        public void consumeEvent(AdaptiveBulkheadEvent event) {
			super.processEvent(event);
		}
	}

	@Override
	public String toString() {
		return String.format("AdaptiveBulkhead '%s'", this.name);
	}

    private interface AdaptiveBulkheadState {

        boolean tryAcquirePermission();

        void acquirePermission();

        void releasePermission();

        void onError(long duration, TimeUnit durationUnit, Throwable throwable);

        void onSuccess(long duration, TimeUnit durationUnit);

        AdaptiveBulkhead.State getState();

        AdaptiveBulkheadMetrics getMetrics();

        /**
         * Should the AdaptiveBulkhead in this state publish events
         *
         * @return a boolean signaling if the events should be published
         */
        default boolean shouldPublishEvents(AdaptiveBulkheadEvent event) {
            return event.getEventType().forcePublish || getState().allowPublish;
        }
    }

	/**
	 * @param eventSupplier the event supplier to be pushed to consumers
	 */
    private void publishBulkheadEvent(AdaptiveBulkheadEvent eventSupplier) {
		if (eventProcessor.hasConsumers()) {
			eventProcessor.consumeEvent(eventSupplier);
		}
	}

	/**
	 * @param callTime  the call duration in milliseconds
	 * @param success is the call successful or not
	 * @param inFlight  current in flight calls
	 * @return the update limit result DTO @{@link LimitResult}
	 */
	protected LimitResult record(@NonNull long callTime, boolean success, int inFlight) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("starting the adaption of the limit for callTime :{} , success: {}, inFlight: {}", callTime, success, inFlight);
		}
		long callTimeNanos = TimeUnit.MILLISECONDS.toNanos(callTime);
        boolean slow = callTimeNanos > adaptiveBulkheadConfig.getSlowCallDurationThreshold().toNanos();
        Snapshot snapshot = metrics.record(callTimeNanos, TimeUnit.NANOSECONDS, Outcome.of(slow, success));
		return limitAdapter.adaptLimitIfAny(snapshot, inFlight);
	}

    /**
	 * adopt the limit based into the new calculated average
	 *
	 * @param bulkhead       the target semaphore bulkhead
	 * @param updatedLimit   calculated new limit
	 * @param waitTimeMillis new wait time
	 */
    @Deprecated
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
			publishBulkheadEvent(new BulkheadOnLimitIncreasedEvent(shortName(bulkhead),
					limitChangeEventData(waitTimeMillis, updatedLimit)));
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
			publishBulkheadEvent(new BulkheadOnLimitDecreasedEvent(shortName(bulkhead),
					limitChangeEventData(waitTimeMillis, updatedLimit)));
		}
	}

    private String shortName(Bulkhead bulkhead) {
        int cut = bulkhead.getName().indexOf('-');
        return cut > 0 ? bulkhead.getName().substring(0, cut) : bulkhead.getName();
    }

    /**
	 * @param waitTimeMillis        new wait time
	 * @param newMaxConcurrentCalls new max concurrent data
	 * @return map of kep value string of the event properties
	 */
	private Map<String, String> limitChangeEventData(long waitTimeMillis, int newMaxConcurrentCalls) {
		Map<String, String> eventData = new HashMap<>();
		eventData.put("newMaxConcurrentCalls", String.valueOf(newMaxConcurrentCalls));
		eventData.put("newWaitTimeMillis", String.valueOf(waitTimeMillis));
		return eventData;
	}

	/**
	 * @param throwable error exception to be wrapped into the event data
	 * @return map of kep value string of the event properties
	 */
	private Map<String, String> errorData(Throwable throwable) {
		Map<String, String> eventData = new HashMap<>();
		eventData.put("exceptionMsg", throwable.getMessage());
		return eventData;
	}

	/**
	 * @param callTime the call duration time
	 * @param durationUnit the duration unit
	 * @param throwable the error exception
	 */
	private void handleError(long callTime, TimeUnit durationUnit, Throwable throwable) {
		bulkhead.onComplete();
        publishBulkheadEvent(new BulkheadOnErrorEvent(shortName(bulkhead), errorData(throwable)));
		final LimitResult limitResult = record(durationUnit.toMillis(callTime), false, inFlight.getAndDecrement());
		adoptLimit(bulkhead, limitResult.getLimit(), limitResult.waitTime());
	}

}
