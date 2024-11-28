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

/**
 * A clock abstraction used to measure absolute and relative time.
 */
public interface Clock {
    /**
     * Current time expressed as milliseconds since the Unix epoch. Should not be used for measuring time,
     * just for timestamps.
     *
     * @return wall time in milliseconds
     */
    long wallTime();

    /**
     * Current monotonic time in nanoseconds. Should be used for measuring time.
     * The value is not related to the wall time and is not subject to system clock changes.
     *
     * @return monotonic time in nanoseconds
     */
    long monotonicTime();

    Clock SYSTEM = new Clock() {
        @Override
        public long wallTime() {
            return System.currentTimeMillis();
        }

        @Override
        public long monotonicTime() {
            return System.nanoTime();
        }
    };
}
