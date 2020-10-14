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

import io.github.resilience4j.core.lang.Nullable;

/**
 * A RetryEvent which informs that a call has been retried, but still failed, such that the the
 * maximum number of attempts has been reached. It will not be retried any more.
 */
public class RetryOnErrorEvent extends AbstractRetryEvent {

    public RetryOnErrorEvent(String name, int numberOfAttempts, @Nullable Throwable lastThrowable) {
        super(name, numberOfAttempts, lastThrowable);
    }

    @Override
    public Type getEventType() {
        return Type.ERROR;
    }

    @Override
    public String toString() {
        return String.format(
            "%s: Retry '%s' recorded a failed retry attempt. Number of retry attempts: '%d'. Giving up. Last exception was: '%s'.",
            getCreationTime(),
            getName(),
            getNumberOfRetryAttempts(),
            getLastThrowable());
    }
}
