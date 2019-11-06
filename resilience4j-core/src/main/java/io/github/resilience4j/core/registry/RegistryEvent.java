/*
 *
 *  Copyright 2019: Robert Winkler
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
package io.github.resilience4j.core.registry;

import java.time.ZonedDateTime;

public interface RegistryEvent {

    /**
     * Returns the type of the Registry event.
     *
     * @return the type of the Registry event
     */
    Type getEventType();

    /**
     * Returns the creation time of Registry event.
     *
     * @return the creation time of Registry event
     */
    ZonedDateTime getCreationTime();

    /**
     * Event types which are created by a CircuitBreaker.
     */
    enum Type {
        /**
         * An Event which informs that an entry has been added
         */
        ADDED,
        /**
         * An Event which informs that an entry has been removed
         */
        REMOVED,
        /**
         * An Event which informs that an entry has been replaced
         */
        REPLACED
    }
}
