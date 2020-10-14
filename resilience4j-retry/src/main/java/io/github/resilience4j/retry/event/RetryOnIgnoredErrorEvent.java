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
 * A RetryEvent which informs that an error has been ignored. It will not be retried.
 * <p>
 * An error is ignored when the exception is determined to be non-retriable, as determined by the
 * {@link io.github.resilience4j.retry.RetryConfig}.
 */
public class RetryOnIgnoredErrorEvent extends AbstractRetryEvent {

    public RetryOnIgnoredErrorEvent(String name, @Nullable Throwable lastThrowable) {
        super(name, 0, lastThrowable);
    }

    @Override
    public Type getEventType() {
        return Type.IGNORED_ERROR;
    }

    @Override
    public String toString() {
        return String.format("%s: Retry '%s' recorded an error which has been ignored: '%s'.",
            getCreationTime(),
            getName(),
            getLastThrowable());
    }
}
