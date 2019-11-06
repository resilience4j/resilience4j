/*
 *
 *  Copyright 2017: Robert Winkler
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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * A simple {@link StopWatch} to measure the processing duration of a call.
 */
public class StopWatch {

    private final Instant startTime;
    private Clock clock;

    StopWatch(Clock clock) {
        this.clock = clock;
        this.startTime = clock.instant();
    }

    public static StopWatch start() {
        return new StopWatch(Clock.systemUTC());
    }

    public Duration stop() {
        return Duration.between(startTime, clock.instant());
    }
}
