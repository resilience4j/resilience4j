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
package io.github.resilience4j.core.metrics

import io.github.resilience4j.core.Clock
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * A clock that can be manually advanced in time for testing purposes.
 */
class ManualClock : Clock {
    private val timeNanos = AtomicLong(0)

    fun advanceByMillis(millis: Long) {
        timeNanos.addAndGet(TimeUnit.MILLISECONDS.toNanos(millis))
    }

    fun advanceBySeconds(seconds: Long) {
        timeNanos.addAndGet(TimeUnit.SECONDS.toNanos(seconds))
    }

    override fun wallTime(): Long {
        return TimeUnit.NANOSECONDS.toMillis(timeNanos.get())
    }

    override fun monotonicTime(): Long {
        return timeNanos.get()
    }
}
