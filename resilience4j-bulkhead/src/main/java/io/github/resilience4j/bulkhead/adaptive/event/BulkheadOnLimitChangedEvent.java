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

import io.github.resilience4j.core.lang.NonNull;

/**
 * A BulkheadEvent which informs that a limit has been changed
 */
public class BulkheadOnLimitChangedEvent extends AbstractAdaptiveBulkheadEvent {

    private final int oldMaxConcurrentCalls;
    private final int newMaxConcurrentCalls;

    public BulkheadOnLimitChangedEvent(String bulkheadName, int oldMaxConcurrentCalls, int newMaxConcurrentCalls) {
        super(bulkheadName);
        this.oldMaxConcurrentCalls = oldMaxConcurrentCalls;
        this.newMaxConcurrentCalls = newMaxConcurrentCalls;
    }

    @NonNull
    @Override
    public Type getEventType() {
        return Type.LIMIT_CHANGED;
    }

    public int getNewMaxConcurrentCalls() {
        return newMaxConcurrentCalls;
    }

    public boolean isIncrease() {
        return newMaxConcurrentCalls > oldMaxConcurrentCalls;
    }

    @Override
    public String toString() {
        return String.format("%s: Bulkhead '%s' recorded a limit change from %s to %s",
            getCreationTime(),
            getBulkheadName(),
            oldMaxConcurrentCalls,
            newMaxConcurrentCalls
        );
    }

}
