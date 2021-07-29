/*
 *
 *  Copyright 2021: Matthew Sandoz
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
package io.github.resilience4j.hedge.metrics;

import io.github.resilience4j.hedge.HedgeMetrics;
import io.github.resilience4j.hedge.event.HedgeEvent;

import java.time.Duration;

/**
 * Handles preconfigured metrics advice. When initialized with a duration, the hedge always waits that duration before
 * beginning of the hedged execution
 */
public class PreconfiguredCutoffMetrics implements HedgeMetrics {

    private final Duration cutoff;

    /**
     * Creates a metrics that will wait a preconfigured amount of time before hedging
     *
     * @param cutoff the Duration of the wait before executing hedges.
     */
    public PreconfiguredCutoffMetrics(Duration cutoff) {
        this.cutoff = cutoff;
    }

    @Override
    public Duration getResponseTimeCutoff() {
        return cutoff;
    }

    @Override
    public void accept(HedgeEvent.Type type, Duration duration) {
        //do nothing here - we ignore events in this case.
    }
}
