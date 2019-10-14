/*
 *
 *  Copyright 2016 Robert Winkler and Bohdan Storozhuk
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
package io.github.resilience4j.ratelimiter.event;

import java.time.ZonedDateTime;

public abstract class AbstractRateLimiterEvent implements RateLimiterEvent {

    private final String rateLimiterName;
    private final int numberOfPermits;
    private final ZonedDateTime creationTime;

    public AbstractRateLimiterEvent(String rateLimiterName) {
        this(rateLimiterName, 1);
    }

    public AbstractRateLimiterEvent(String rateLimiterName, int numberOfPermits) {
        this.rateLimiterName = rateLimiterName;
        this.numberOfPermits = numberOfPermits;
        creationTime = ZonedDateTime.now();
    }

    @Override
    public String getRateLimiterName() {
        return rateLimiterName;
    }

    @Override
    public int getNumberOfPermits() {
        return numberOfPermits;
    }

    @Override
    public ZonedDateTime getCreationTime() {
        return creationTime;
    }

    @Override
    public String toString() {
        return "RateLimiterEvent{" +
            "type=" + getEventType() +
            ", rateLimiterName='" + getRateLimiterName() + '\'' +
            ", creationTime=" + getCreationTime() +
            '}';
    }
}
