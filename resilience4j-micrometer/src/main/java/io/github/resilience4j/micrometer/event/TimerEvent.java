/*
 *  Copyright 2023 Mariusz Kopylec
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
 */
package io.github.resilience4j.micrometer.event;

import java.time.ZonedDateTime;

/**
 * An event which is created by a Timer.
 */
public interface TimerEvent {

    /**
     * @return The name of the Timer which has created the event
     */
    String getTimerName();

    /**
     * @return The type of the Timer event
     */
    Type getEventType();

    /**
     * @return The creation time of Timer event
     */
    ZonedDateTime getCreationTime();

    /**
     * Event types which are created by a Timer.
     */
    enum Type {
        /**
         * A TimerEvent which informs that the decorated operation has started.
         */
        START,
        /**
         * A TimerEvent which informs that the decorated operation was successful.
         */
        SUCCESS,
        /**
         * A TimerEvent which informs that the decorated operation threw an exception.
         */
        FAILURE
    }
}
