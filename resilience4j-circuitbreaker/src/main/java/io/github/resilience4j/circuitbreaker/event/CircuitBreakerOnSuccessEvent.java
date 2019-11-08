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

import java.time.Duration;

/**
 * A CircuitBreakerEvent which informs that a success has been recorded
 */
public class CircuitBreakerOnSuccessEvent extends AbstractCircuitBreakerEvent {

    private final Duration elapsedDuration;

    public CircuitBreakerOnSuccessEvent(String circuitBreakerName, Duration elapsedDuration) {
        super(circuitBreakerName);
        this.elapsedDuration = elapsedDuration;
    }

    public Duration getElapsedDuration() {
        return elapsedDuration;
    }

    @Override
    public Type getEventType() {
        return Type.SUCCESS;
    }

    @Override
    public String toString() {
        return String
            .format("%s: CircuitBreaker '%s' recorded a successful call. Elapsed time: %s ms",
                getCreationTime(),
                getCircuitBreakerName(),
                getElapsedDuration().toMillis());
    }
}
