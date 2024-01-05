/*
 *
 *  Copyright 2019 Mahmoud Romeh
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
package io.github.resilience4j.bulkhead.adaptive.event;

import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;
import io.github.resilience4j.core.lang.NonNull;

import java.time.ZonedDateTime;

/**
 * A BulkheadEvent which informs about a state transition.
 */
public class BulkheadOnStateTransitionEvent extends AbstractAdaptiveBulkheadEvent {

    private final AdaptiveBulkhead.State oldState;
    private final AdaptiveBulkhead.State newState;

    public BulkheadOnStateTransitionEvent(String bulkheadName,
                                          ZonedDateTime creationTime,
                                          AdaptiveBulkhead.State oldState,
                                          AdaptiveBulkhead.State newState) {
        super(bulkheadName, creationTime);
        this.oldState = oldState;
        this.newState = newState;
    }

    @NonNull
    @Override
    public Type getEventType() {
        return Type.STATE_TRANSITION;
    }

    public AdaptiveBulkhead.State getNewState() {
        return newState;
    }

    @Override
    public String toString() {
        return String.format("%s: Bulkhead '%s' recorded a state transition from %s to %s",
            getCreationTime(),
            getBulkheadName(),
            oldState,
            newState
        );
    }
}
