/*
 *
 *  Copyright 2019: Bohdan Storozhuk, Mahmoud Romeh, Tomasz Skowroński
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
import io.github.resilience4j.bulkhead.adaptive.event.*;
import io.github.resilience4j.bulkhead.internal.SemaphoreBulkhead;
import io.github.resilience4j.core.lang.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class AdaptiveBulkheadStateMachine implements AdaptiveBulkhead {

    private static final Logger LOG = LoggerFactory.getLogger(AdaptiveBulkheadStateMachine.class);
    // TODO remove
    public static final boolean RESET_METRICS_ON_TRANSITION = true;

    private final String name;
    private final AtomicReference<AdaptiveBulkheadState> stateReference;
    private final AdaptiveBulkheadConfig config;
    private final AdaptiveBulkheadEventProcessor eventProcessor;
    private final Clock clock;
    private final Function<Clock, Long> currentTimestampFunction;
    private final TimeUnit timestampUnit;
    private final AdaptiveBulkheadMetrics metrics;
    private final Bulkhead innerBulkhead;
    private final AdaptationCalculator adaptationCalculator;

    public AdaptiveBulkheadStateMachine(@NonNull String name,
                                        @NonNull AdaptiveBulkheadConfig config) {
        this(name, config, Clock.systemUTC());
    }

    public AdaptiveBulkheadStateMachine(@NonNull String name,
                                        @NonNull AdaptiveBulkheadConfig config,
                                        @NonNull Clock clock) {
        this.name = name;
        this.config = Objects.requireNonNull(config, "Config must not be null");
        this.innerBulkhead = innerBulkhead(name, config);
        this.clock = clock;
        this.currentTimestampFunction = config.getCurrentTimestampFunction();
        this.timestampUnit = config.getTimestampUnit();
        this.metrics = new AdaptiveBulkheadMetrics(config, innerBulkhead.getMetrics(), clock);
        this.stateReference = new AtomicReference<>(new SlowStartState(metrics));
        this.eventProcessor = new AdaptiveBulkheadEventProcessor();
        this.adaptationCalculator = new AdaptationCalculator(this);
    }

    private static SemaphoreBulkhead innerBulkhead(String name, AdaptiveBulkheadConfig config) {
        return new SemaphoreBulkhead(name + "-internal",
            BulkheadConfig.custom()
                .maxConcurrentCalls(config.getInitialConcurrentCalls())
                .maxWaitDuration(config.getMaxWaitDuration())
                .build());
    }

    @Override
    public long getCurrentTimestamp() {
        return currentTimestampFunction.apply(clock);
    }

    @Override
    public TimeUnit getTimestampUnit() {
        return timestampUnit;
    }

    private ZonedDateTime time() {
        return ZonedDateTime.now(clock);
    }

    @Override
    public boolean tryAcquirePermission() {
        return stateReference.get().tryAcquirePermission();
    }

    @Override
    public void acquirePermission() {
        stateReference.get().acquirePermission();
    }

    @Override
    public void releasePermission() {
        stateReference.get().releasePermission();
    }

    /**
     * @param duration     call time
     * @param durationUnit call time unit
     */
    @Override
    public void onSuccess(long duration, TimeUnit durationUnit) {
        releasePermission(); // ?
        stateReference.get().onSuccess(duration, durationUnit);
        tryPublishEvent(
            new BulkheadOnSuccessEvent(name, time()));
    }

    /**
     * @param startTime    call start time in millis or 0
     * @param durationUnit call time unit
     * @param throwable    an error
     */
    @Override
    public void onError(long startTime, TimeUnit durationUnit, Throwable throwable) {
        if (config.getIgnoreExceptionPredicate().test(throwable)) {
            releasePermission();
            tryPublishEvent(
                new BulkheadOnIgnoreEvent(name, time(), throwable));
        } else if (startTime != 0 && config.getRecordExceptionPredicate().test(throwable)) {
            releasePermission(); // ?
            stateReference.get().onError(timeUntilNow(startTime), durationUnit, throwable);
            tryPublishEvent(
                new BulkheadOnErrorEvent(name, time(), throwable));
        } else if (startTime != 0) {
            onSuccess(timeUntilNow(startTime), durationUnit);
        }
    }

    private long timeUntilNow(long start) {
        return Duration.between(Instant.ofEpochMilli(start), clock.instant()).toMillis();
    }

    @Override
    public AdaptiveBulkheadConfig getBulkheadConfig() {
        return config;
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

    @Override
    public void transitionToCongestionAvoidance() {
        stateTransition(State.CONGESTION_AVOIDANCE,
            current -> new CongestionAvoidance(current.getMetrics()));
    }

    @Override
    public void transitionToSlowStart() {
        stateTransition(State.SLOW_START,
            current -> new SlowStartState(current.getMetrics()));
    }

    private void stateTransition(State newState,
                                 UnaryOperator<AdaptiveBulkheadState> newStateGenerator) {
        LOG.debug("stateTransition to {}", newState);
        AdaptiveBulkheadState previous = stateReference.getAndUpdate(newStateGenerator);
        tryPublishEvent(
            new BulkheadOnStateTransitionEvent(name, time(), previous.getState(), newState));
    }

    private void changeConcurrencyLimit(int newValue) {
        int oldValue = innerBulkhead.getBulkheadConfig().getMaxConcurrentCalls();
        if (newValue != oldValue) {
            changeInternals(oldValue, newValue);
            tryPublishEvent(
                new BulkheadOnLimitChangedEvent(name, time(), oldValue, newValue));
        } else {
            LOG.trace("change of concurrency limit is ignored");
        }
    }

    private void changeInternals(int oldValue, int newValue) {
        LOG.debug("changeConcurrencyLimit from {} to {}", oldValue, newValue);
        innerBulkhead.changeConfig(
            BulkheadConfig.from(innerBulkhead.getBulkheadConfig())
                .maxConcurrentCalls(newValue)
                .build());
        if (RESET_METRICS_ON_TRANSITION) {
            metrics.resetRecords();
        }
    }

    /**
     * Although the strategy is referred to as slow start, its congestion window growth is quite
     * aggressive, more aggressive than the congestion avoidance phase.
     */
    private class SlowStartState implements AdaptiveBulkheadState {

        private final AdaptiveBulkheadMetrics adaptiveBulkheadMetrics;
        private final AtomicBoolean slowStart;

        SlowStartState(AdaptiveBulkheadMetrics adaptiveBulkheadMetrics) {
            this.adaptiveBulkheadMetrics = adaptiveBulkheadMetrics;
            this.slowStart = new AtomicBoolean(true);
        }

        @Override
        public boolean tryAcquirePermission() {
            return slowStart.get() && innerBulkhead.tryAcquirePermission();
        }

        @Override
        public void acquirePermission() {
            innerBulkhead.acquirePermission();
        }

        @Override
        public void releasePermission() {
            innerBulkhead.releasePermission();
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
            logStateDetails(result);
            if (slowStart.get()) {
                switch (result) {
                    case BELOW_THRESHOLDS:
                        changeConcurrencyLimit(adaptationCalculator.increase());
                        break;
                    case ABOVE_THRESHOLDS:
                        if (slowStart.compareAndSet(true, false)) {
                            changeConcurrencyLimit(adaptationCalculator.decrease());
                            transitionToCongestionAvoidance();
                        }
                        break;
                }
            }
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

    @Deprecated
    private void logStateDetails(AdaptiveBulkheadMetrics.Result result) {
        LOG.debug("calls:{}/{}, rate:{} result:{}",
            metrics.getNumberOfBufferedCalls(),
            config.getMinimumNumberOfCalls(),
            Math.max(metrics.getSnapshot().getFailureRate(), metrics.getSnapshot().getSlowCallRate()),
            result);
    }


    private class CongestionAvoidance implements AdaptiveBulkheadState {

        private final AdaptiveBulkheadMetrics adaptiveBulkheadMetrics;
        private final AtomicBoolean congestionAvoidance;

        CongestionAvoidance(AdaptiveBulkheadMetrics adaptiveBulkheadMetrics) {
            this.adaptiveBulkheadMetrics = adaptiveBulkheadMetrics;
            this.congestionAvoidance = new AtomicBoolean(true);
        }

        @Override
        public boolean tryAcquirePermission() {
            return congestionAvoidance.get() && innerBulkhead.tryAcquirePermission();
        }

        @Override
        public void acquirePermission() {
            innerBulkhead.acquirePermission();
        }

        @Override
        public void releasePermission() {
            innerBulkhead.releasePermission();
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
         * Transitions to SLOW_START state when Minimum Concurrency Limit have been reached.
         *
         * @param result the Result
         */
        private void checkIfThresholdsExceeded(AdaptiveBulkheadMetrics.Result result) {
            logStateDetails(result);
            if (congestionAvoidance.get()) {
                switch (result) {
                    case BELOW_THRESHOLDS:
                        if (isConcurrencyLimitTooLow()) {
                            if (congestionAvoidance.compareAndSet(true, false)) {
                                transitionToSlowStart();
                            }
                        } else {
                            changeConcurrencyLimit(adaptationCalculator.increment());
                        }
                        break;
                    case ABOVE_THRESHOLDS:
                        changeConcurrencyLimit(adaptationCalculator.decrease());
                        break;
                }
            }
        }

        private boolean isConcurrencyLimitTooLow() {
            return getMetrics().getMaxAllowedConcurrentCalls()
                == config.getMinConcurrentCalls();
        }

        /**
         * Get the state of the AdaptiveBulkhead
         */
        @Override
        public AdaptiveBulkhead.State getState() {
            return State.CONGESTION_AVOIDANCE;
        }

        /**
         * Get metrics of the AdaptiveBulkhead
         */
        @Override
        public AdaptiveBulkheadMetrics getMetrics() {
            return adaptiveBulkheadMetrics;
        }

    }

    @Override
    public String toString() {
        return String.format("AdaptiveBulkhead '%s'", this.name);
    }

    private void tryPublishEvent(AdaptiveBulkheadEvent event) {
        if (!eventProcessor.hasConsumers()) {
            LOG.debug("No consumers: Event {} not published", event.getEventType());
            return;
        }
        if (!stateReference.get().shouldPublishEvents(event)) {
            LOG.debug("Not allowed: Event {} not published", event.getEventType());
            return;
        }
        try {
            eventProcessor.consumeEvent(event);
            LOG.debug("Event {} published: {}", event.getEventType(), event);
        } catch (Throwable t) {
            LOG.warn("Failed to handle event {}", event.getEventType(), t);
        }
    }

}
