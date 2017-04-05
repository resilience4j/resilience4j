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

abstract class AbstractRetryEvent implements RetryEvent {

    private final String name;
    private final ZonedDateTime creationTime;
    private final int numberOfAttempts;
    private final Throwable lastThrowable;

    AbstractRetryEvent(String name, int numberOfAttempts, Throwable lastThrowable) {
        this.name = name;
        this.numberOfAttempts = numberOfAttempts;
        this.creationTime = ZonedDateTime.now();
        this.lastThrowable = lastThrowable;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ZonedDateTime getCreationTime() {
        return creationTime;
    }

    @Override
    public int getNumberOfAttempts() {
        return numberOfAttempts;
    }

    @Override
    public Throwable getLastThrowable() {
        return lastThrowable;
    }
}
