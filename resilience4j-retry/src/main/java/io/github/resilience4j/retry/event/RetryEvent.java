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
package io.github.resilience4j.retry.event;

import java.time.ZonedDateTime;

/**
 * An event which is created by Retry.
 */
public interface RetryEvent {

    /**
     * Returns the ID of the Retry.
     *
     * @return the ID of the Retry
     */
    String getName();

    /**
     * Returns the number of retry attempts.
     *
     * @return the the number of retry attempts
     */
    int getNumberOfRetryAttempts();

    /**
     * Returns the type of the Retry event.
     *
     * @return the type of the Retry event
     */
    Type getEventType();

    /**
     * Returns the creation time of Retry event.
     *
     * @return the creation time of Retry event
     */
    ZonedDateTime getCreationTime();

    /**
     * Returns the last captured Throwable.
     *
     * @return the last captured Throwable
     */
    Throwable getLastThrowable();

    /**
     * Event types which are created by a Retry.
     */
    enum Type {
        /**
         * A RetryEvent which informs that a call has been tried, failed and will now be retried
         */
        RETRY,
        /**
         * A RetryEvent which informs that a call has been retried, but still failed
         */
        ERROR,
        /**
         * A RetryEvent which informs that a call has been successful
         */
        SUCCESS,
        /**
         * A RetryEvent which informs that an error has been ignored
         */
        IGNORED_ERROR
    }
}
