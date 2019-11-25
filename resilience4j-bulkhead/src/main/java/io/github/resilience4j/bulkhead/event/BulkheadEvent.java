/*
 *
 *  Copyright 2017 Robert Winkler, Lucas Lech
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
package io.github.resilience4j.bulkhead.event;

import java.time.ZonedDateTime;

/**
 * An event which is created by a bulkhead.
 */
public interface BulkheadEvent {

    /**
     * Returns the name of the bulkhead which has created the event.
     *
     * @return the name of the bulkhead which has created the event
     */
    String getBulkheadName();

    /**
     * Returns the type of the bulkhead event.
     *
     * @return the type of the bulkhead event
     */
    Type getEventType();

    /**
     * Returns the creation time of bulkhead event.
     *
     * @return the creation time of bulkhead event
     */
    ZonedDateTime getCreationTime();

    /**
     * Event types which are created by a bulkhead.
     */
    enum Type {
        /**
         * A BulkheadEvent which informs that a call has been permitted to proceed
         */
        CALL_PERMITTED,
        /**
         * A BulkheadEvent which informs that a call was rejected due to bulkhead being full
         */
        CALL_REJECTED,
        /**
         * A BulkheadEvent which informs that a call was finished(success and failure is
         * indistinguishable)
         */
        CALL_FINISHED
    }
}
