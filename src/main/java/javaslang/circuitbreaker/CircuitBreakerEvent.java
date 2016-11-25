/*
 *
 *  Copyright 2015 Robert Winkler
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
package javaslang.circuitbreaker;

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
     * @return tthe type of the CircuitBreaker event
     */
    Type getEventType();

    /**
     * Event types which are created by a CircuitBreaker.
     */
    enum Type {
        /** A CircuitBreakerEvent which informs that a failure has been recorded */
        RECORDED_FAILURE,
        /** A CircuitBreakerEvent which informs that a failure has been recorded */
        STATE_TRANSITION
    }
}
