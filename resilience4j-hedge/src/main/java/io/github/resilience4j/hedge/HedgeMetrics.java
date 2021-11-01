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
package io.github.resilience4j.hedge;

import io.github.resilience4j.hedge.event.HedgeEvent;
import io.github.resilience4j.hedge.metrics.AveragePlusMetrics;
import io.github.resilience4j.hedge.metrics.PreconfiguredCutoffMetrics;

import java.time.Duration;

/**
 * HedgeMetrics can both track the aggregate status of previous calls and makes recommendations about how long to wait
 * before hedging the next request.
 */
public interface HedgeMetrics {

    /**
     * Creates a metric which tracks average response time and uses some increment over it for computing when to hedge.
     *
     * @param shouldUseFactorAsPercentage whether to use factor as an integer percent of average or an absolute number
     *                                    of milliseconds to add to the average ot determine hedge start time
     * @param factor                      the factor either as percentage or milliseconds
     * @param shouldMeasureErrors         whether to count errors in the averaging metrics
     * @param windowSize                  only supports fixed size window, not time-based
     * @return the configured HedgeMetrics
     */
    static HedgeMetrics ofAveragePlus(boolean shouldUseFactorAsPercentage, int factor, boolean shouldMeasureErrors, int windowSize) {
        return new AveragePlusMetrics(shouldUseFactorAsPercentage, factor, shouldMeasureErrors, windowSize);
    }

    /**
     * Creates a simple metric that returns a pre-defined hedging time
     *
     * @param cutoff the preconfigured cutoff time as a Duration
     * @return the configured HedgeMetrics
     */
    static HedgeMetrics ofPreconfigured(Duration cutoff) {
        return new PreconfiguredCutoffMetrics(cutoff);
    }

    /**
     * @return the duration that the hedge should wait before executing the call.
     */
    Duration getResponseTimeCutoff();

    /**
     * adds the events to the aggregated metrics
     * @param type     indicates whether the result is primary or not, and successful or not
     * @param duration duration it took to complete the hedged call
     */
    void accept(HedgeEvent.Type type, Duration duration);

}
