/*
 *
 *  Copyright 2019 authors
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
package io.github.resilience4j.timelimiter.event;

import java.time.ZonedDateTime;

public abstract class AbstractTimeLimiterEvent implements TimeLimiterEvent {

    private final String timeLimiterName;
    private final ZonedDateTime creationTime;
    private Type eventType;

    AbstractTimeLimiterEvent(String timeLimiterName, Type eventType) {
        this.timeLimiterName = timeLimiterName;
        this.eventType = eventType;
        this.creationTime = ZonedDateTime.now();
    }

    @Override
    public String getTimeLimiterName() {
        return timeLimiterName;
    }

    @Override
    public ZonedDateTime getCreationTime() {
        return creationTime;
    }

    @Override
    public Type getEventType() {
        return eventType;
    }

    @Override
    public String toString() {
        return "TimeLimiterEvent{" +
            "type=" + getEventType() +
            ", timeLimiterName='" + getTimeLimiterName() + '\'' +
            ", creationTime=" + getCreationTime() +
            '}';
    }
}
