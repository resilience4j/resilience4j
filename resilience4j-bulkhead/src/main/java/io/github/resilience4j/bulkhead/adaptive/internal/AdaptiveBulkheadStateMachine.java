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
import io.github.resilience4j.bulkhead.adaptive.ResultRecordedAsFailureException;
import io.github.resilience4j.bulkhead.adaptive.event.*;
import io.github.resilience4j.bulkhead.internal.SemaphoreBulkhead;
import io.github.resilience4j.core.lang.NonNull;
import io.github.resilience4j.core.lang.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
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

    private ZonedDateTime now() {
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
     * This method must be invoked when a call returned a result
     * and the result predicate should decide if the call was successful or not.
     *
     * @param startTime The start time of the call
     * @param timeUnit  The time unit
     * @param result    The result of the protected function
     */
    public void onResult(long startTime, TimeUnit timeUnit, @Nullable Object result) {
        if (result != null && config.getRecordResultPredicate().test(result)) {
            ResultRecordedAsFailureException failure = new ResultRecordedAsFailureException(name, result);
            LOG.debug("'{}' recorded a result type '{}' as a failure", name, result.getClass());
            onError(startTime, timeUnit, failure);
        } else {
            onSuccess(startTime, timeUnit);
        }
    }

    /**
     * @param startTime The start time of the call
     * @param timeUnit  The time unit
     */
    @Override
    public void onSuccess(long startTime, TimeUnit timeUnit) {
        LOG.debug("'{}' recorded a success", name);
        releasePermission(); // ?
        stateReference.get().onSuccess(startTime, timeUnit);
        tryPublishEvent(
            new BulkheadOnSuccessEvent(name, now()));
    }

    /**
     * @param startTime The start time of the call
     * @param timeUnit  The time unit
     * @param throwable An error
     */
    @Override
    public void onError(long startTime, TimeUnit timeUnit, Throwable throwable) {
        if (config.getIgnoreExceptionPredicate().test(throwable)) {
            releasePermission();
            tryPublishEvent(
                new BulkheadOnIgnoreEvent(name, now(), throwable));
        } else if (config.getRecordExceptionPredicate().test(throwable)) {
            LOG.debug("'{}' recorded an error:", name, throwable);
            releasePermission(); // ?
            stateReference.get().onError(startTime, timeUnit, throwable);
            tryPublishEvent(
                new BulkheadOnErrorEvent(name, now(), throwable));
        } else {
            onSuccess(startTime, timeUnit);
        }
    }

    private long nanosUntilNow(long startTime, TimeUnit timeUnit) {
        return Math.max(0, getTimestampUnit().toNanos(getCurrentTimestamp()) - timeUnit.toNanos(startTime));
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
    public EventPublisher getEventPublisher() {
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
        LOG.debug("'{}' did a state transition to {}", name, newState);
        AdaptiveBulkheadState previous = stateReference.getAndUpdate(newStateGenerator);
        tryPublishEvent(
            new BulkheadOnStateTransitionEvent(name, now(), previous.getState(), newState));
    }

    private void changeConcurrencyLimit(int newValue) {
        int oldValue = innerBulkhead.getBulkheadConfig().getMaxConcurrentCalls();
        if (newValue != oldValue) {
            changeInternals(oldValue, newValue);
            tryPublishEvent(
                new BulkheadOnLimitChangedEvent(name, now(), oldValue, newValue));
        } else {
            LOG.trace("'{}' ignored a change of concurrency limit", name);
        }
    }

    private void changeInternals(int oldValue, int newValue) {
        LOG.debug("'{}' changed concurrency limit from {} to {}", name, oldValue, newValue);
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
        public void onError(long startTime, TimeUnit timeUnit, Throwable throwable) {
            checkIfThresholdsExceeded(adaptiveBulkheadMetrics.onError(
                nanosUntilNow(startTime, timeUnit)));
        }

        @Override
        public void onSuccess(long startTime, TimeUnit timeUnit) {
            checkIfThresholdsExceeded(adaptiveBulkheadMetrics.onSuccess(
                nanosUntilNow(startTime, timeUnit)));
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
        LOG.debug("'{}' buffered calls:{}/{}, rate:{} result:{}",
            name,
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
        public void onError(long startTime, TimeUnit timeUnit, Throwable throwable) {
            checkIfThresholdsExceeded(adaptiveBulkheadMetrics.onError(
                nanosUntilNow(startTime, timeUnit)));
        }

        @Override
        public void onSuccess(long startTime, TimeUnit timeUnit) {
            checkIfThresholdsExceeded(adaptiveBulkheadMetrics.onSuccess(
                nanosUntilNow(startTime, timeUnit)));
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
            LOG.debug("'{}' has no consumers: Event {} not published", name, event.getEventType());
            return;
        }
        if (!stateReference.get().shouldPublishEvents(event)) {
            LOG.debug("'{}' did not allow: Event {} not published", name, event.getEventType());
            return;
        }
        try {
            eventProcessor.consumeEvent(event);
            LOG.debug("'{}' published an event {}: {}", name, event.getEventType(), event);
        } catch (Throwable t) {
            LOG.debug("'{}' consumer failure: Event {} not published:", name, event.getEventType(), t);
        }
    }

}
