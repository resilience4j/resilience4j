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
 * A BulkheadEvent which informs that a call has been succeeded
 */
public class BulkheadOnSuccessEvent extends AbstractAdaptiveBulkheadEvent {

    public BulkheadOnSuccessEvent(String bulkheadName) {
        super(bulkheadName);
    }

    @NonNull
    @Override
    public Type getEventType() {
        return Type.SUCCESS;
    }

    @Override
    public String toString() {
        return String.format("%s: Bulkhead '%s' recorded a successful call",
            getCreationTime(),
            getBulkheadName()
        );
    }
}
