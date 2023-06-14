/*
 *
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
 *
 *
 */
package io.github.resilience4j.monitor.event;

import java.time.Duration;
import java.time.ZonedDateTime;

/**
 * An event which is created by a Monitor.
 */
public interface MonitorEvent {

    /**
     * @return The name of the Monitor which has created the event
     */
    String getMonitorName();

    /**
     * @return The type of the Monitor event
     */
    Type getEventType();

    /**
     * @return The creation time of Monitor event
     */
    ZonedDateTime getCreationTime();

    /**
     * Event types which are created by a Monitor.
     */
    enum Type {
        /**
         * A MonitorEvent which informs that the operation execution has started.
         */
        START,
        /**
         * A MonitorEvent which informs that the operation execution was successful.
         */
        SUCCESS,
        /**
         * A MonitorEvent which informs that the operation execution threw an exception.
         */
        FAILURE
    }
}
