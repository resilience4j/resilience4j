/*
 * Copyright 2017 Dan Maas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.resilience4j.ratpack.circuitbreaker;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.*;

public class CircuitBreakerConfig {
    private boolean defaults = false;
    private Integer waitIntervalInMillis = DEFAULT_WAIT_DURATION_IN_OPEN_STATE * 1000;
    private Integer failureRateThreshold = DEFAULT_MAX_FAILURE_THRESHOLD;
    private Integer ringBufferSizeInClosedState = DEFAULT_RING_BUFFER_SIZE_IN_CLOSED_STATE;
    private Integer ringBufferSizeInHalfOpenState = DEFAULT_RING_BUFFER_SIZE_IN_HALF_OPEN_STATE;
    // default is that all exceptions are recorded, and none are ignored
    private List<String> recordExceptions = Arrays.asList(Throwable.class.getName());
    private List<String> ignoreExceptions = Arrays.asList();
    private boolean automaticTransitionFromOpenToHalfOpen = false;

    /**
     * Use config provided by circuitbreaker registry instead of these config values.
     *
     * @param defaults
     * @return
     */
    public CircuitBreakerConfig defaults(boolean defaults) {
        this.defaults = defaults;
        return this;
    }

    public CircuitBreakerConfig waitIntervalInMillis(Integer waitInterval) {
        this.waitIntervalInMillis = waitInterval;
        return this;
    }

    public CircuitBreakerConfig failureRateThreshold(Integer failureRateThreshold) {
        this.failureRateThreshold = failureRateThreshold;
        return this;
    }

    public CircuitBreakerConfig ringBufferSizeInClosedState(Integer ringBufferSizeInClosedState) {
        this.ringBufferSizeInClosedState = ringBufferSizeInClosedState;
        return this;
    }

    public CircuitBreakerConfig ringBufferSizeInHalfOpenState(Integer ringBufferSizeInHalfOpenState) {
        this.ringBufferSizeInHalfOpenState = ringBufferSizeInHalfOpenState;
        return this;
    }

    /**
     * Each element must be the fully qualified string class name of an exception class.
     *
     * @param recordExceptions
     * @return
     */
    public CircuitBreakerConfig recordExceptions(List<String> recordExceptions) {
        this.recordExceptions = recordExceptions;
        return this;
    }

    /**
     * Each element must be the fully qualified string class name of an exception class.
     *
     * @param ignoreExceptions
     * @return
     */
    public CircuitBreakerConfig ignoreExceptions(List<String> ignoreExceptions) {
        this.ignoreExceptions = ignoreExceptions;
        return this;
    }

    public CircuitBreakerConfig automaticTransitionFromOpenToHalfOpen(boolean automaticTransitionFromOpenToHalfOpen) {
        this.automaticTransitionFromOpenToHalfOpen = automaticTransitionFromOpenToHalfOpen;
        return this;
    }

    public boolean getDefaults() {
        return defaults;
    }

    public Integer getWaitIntervalInMillis() {
        return waitIntervalInMillis;
    }

    public Integer getFailureRateThreshold() {
        return failureRateThreshold;
    }

    public Integer getRingBufferSizeInClosedState() {
        return ringBufferSizeInClosedState;
    }

    public Integer getRingBufferSizeInHalfOpenState() {
        return ringBufferSizeInHalfOpenState;
    }

    public List<String> getRecordExceptions() {
        return recordExceptions;
    }

    @SuppressWarnings("unchecked")
    public Class<? extends Throwable>[] getRecordExceptionClasses() {
        List<Class<? extends Throwable>> classList = buildArray(recordExceptions);
        Class<? extends Throwable>[] classes = new Class[classList.size() < 1 ? 1 : classList.size()];
        if (classes.length == 1 && classes[0] == null) {
            classes[0] = Throwable.class;
        }
        return classList.toArray(classes);
    }

    public List<String> getIgnoreExceptions() {
        return ignoreExceptions;
    }

    @SuppressWarnings("unchecked")
    public Class<? extends Throwable>[] getIgnoreExceptionClasses() {
        List<Class<? extends Throwable>> classList = buildArray(ignoreExceptions);
        Class<? extends Throwable>[] classes = new Class[classList.size()];
        return classList.toArray(classes);
    }

    public boolean isAutomaticTransitionFromOpenToHalfOpen() {
        return automaticTransitionFromOpenToHalfOpen;
    }

    @SuppressWarnings("unchecked")
    private List<Class<? extends Throwable>> buildArray(List<String> list) {
        return list.stream()
                .map(t -> {
                    try {
                        return (Class<? extends Throwable>)Class.forName(t);
                    } catch (ClassNotFoundException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

}
