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
package io.github.resilience4j.cache.event;

import java.time.ZonedDateTime;

/**
 * An event which is created by a CircuitBreaker.
 */
public interface CacheEvent {

    /**
     * Returns the name of the Cache which has created the event.
     *
     * @return the name of the Cache which has created the event
     */
    String getCacheName();


    /**
     * Returns the creation time of Cache event.
     *
     * @return the creation time of Cache event
     */
    ZonedDateTime getCreationTime();

    /**
     * Returns the type of the Cache event.
     *
     * @return the type of the Cache event
     */
    Type getEventType();

    /**
     * Event types which are created by a CircuitBreaker.
     */
    enum Type {
        /**
         * A CacheEvent which informs that at Cache was not available
         */
        ERROR,
        /**
         * A CacheEvent which informs a cache hit
         */
        CACHE_HIT,
        /**
         * A CacheEvent which informs a cache miss
         */
        CACHE_MISS
    }
}
