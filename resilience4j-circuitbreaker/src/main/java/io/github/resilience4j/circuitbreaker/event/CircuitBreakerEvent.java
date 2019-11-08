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
package io.github.resilience4j.circuitbreaker.event;

import java.time.ZonedDateTime;

/**
 * An event which is created by a CircuitBreaker.
 */
public interface CircuitBreakerEvent {

    /**
     * Returns the name of the CircuitBreaker which has created the event.
     *
     * @return the name of the CircuitBreaker which has created the event
     */
    String getCircuitBreakerName();

    /**
     * Returns the type of the CircuitBreaker event.
     *
     * @return the type of the CircuitBreaker event
     */
    Type getEventType();

    /**
     * Returns the creation time of CircuitBreaker event.
     *
     * @return the creation time of CircuitBreaker event
     */
    ZonedDateTime getCreationTime();

    /**
     * Event types which are created by a CircuitBreaker.
     */
    enum Type {
        /**
         * A CircuitBreakerEvent which informs that an error has been recorded
         */
        ERROR(false),
        /**
         * A CircuitBreakerEvent which informs that an error has been ignored
         */
        IGNORED_ERROR(false),
        /**
         * A CircuitBreakerEvent which informs that a success has been recorded
         */
        SUCCESS(false),
        /**
         * A CircuitBreakerEvent which informs that a call was not permitted because the
         * CircuitBreaker state is OPEN
         */
        NOT_PERMITTED(false),
        /**
         * A CircuitBreakerEvent which informs the state of the CircuitBreaker has been changed
         */
        STATE_TRANSITION(true),
        /**
         * A CircuitBreakerEvent which informs the CircuitBreaker has been reset
         */
        RESET(true),
        /**
         * A CircuitBreakerEvent which informs the CircuitBreaker has been forced open
         */
        FORCED_OPEN(false),
        /**
         * A CircuitBreakerEvent which informs the CircuitBreaker has been disabled
         */
        DISABLED(false);

        public final boolean forcePublish;

        Type(boolean forcePublish) {
            this.forcePublish = forcePublish;
        }
    }
}
