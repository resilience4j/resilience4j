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

/**
 * A wrapper used to adapt the Java 8 Clock used in the rest of the project to the new Clock interface.
 */
public class JavaClockWrapper implements Clock {
    private final java.time.Clock clock;

    public JavaClockWrapper(java.time.Clock clock) {
        this.clock = clock;
    }

    @Override
    public long wallTime() {
        return this.clock.millis();
    }

    @Override
    public long monotonicTime() {
        return TimeUnit.MILLISECONDS.toNanos(this.clock.millis());
    }
}
