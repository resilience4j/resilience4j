/*
 *
 *  Copyright 2024 Florentin Simion and Rares Vlasceanu
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
package io.github.resilience4j.core;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A clock that can be manually advanced in time for testing purposes.
 */
public class ManualClock implements Clock {
    private final AtomicLong timeNanos = new AtomicLong(0);

    public void advanceByMillis(long millis) {
        timeNanos.addAndGet(TimeUnit.MILLISECONDS.toNanos(millis));
    }

    public void advanceBySeconds(long seconds) {
        timeNanos.addAndGet(TimeUnit.SECONDS.toNanos(seconds));
    }

    @Override
    public long wallTime() {
        return TimeUnit.NANOSECONDS.toMillis(timeNanos.get());
    }

    @Override
    public long monotonicTime() {
        return timeNanos.get();
    }
}
