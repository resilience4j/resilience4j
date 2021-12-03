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
package io.github.resilience4j.hedge.internal;

import io.github.resilience4j.hedge.HedgeConfig;
import io.github.resilience4j.hedge.event.HedgeEvent;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * HedgeDurationSupplier makes recommendations about how long to wait before hedging the request.
 */
public interface HedgeDurationSupplier extends Supplier<Duration>{

    /**
     * Creates a HedgeDurationSupplier from the given HedgeConfig
     *
     * @param config - the given HedgeConfiguration
     * @return the configured HedgeDurationSupplier
     */
    static HedgeDurationSupplier fromConfig(HedgeConfig config) {
        if (config.getDurationSupplier() == HedgeConfig.HedgeDurationSupplierType.PRECONFIGURED) {
            return ofPreconfigured(config.getCutoff());
        } else {
            return ofAveragePlus(
                config.isShouldUseFactorAsPercentage(),
                config.getHedgeTimeFactor(),
                config.isShouldMeasureErrors(),
                config.getWindowSize());
        }
    }

    /**
     * Creates a tracker of average response time and uses some increment over it for computing when to hedge.
     *
     * @param shouldUseFactorAsPercentage whether to use factor as an integer percent of average or an absolute number
     *                                    of milliseconds to add to the average ot determine hedge start time
     * @param factor                      the factor either as percentage or milliseconds
     * @param shouldMeasureErrors         whether to count errors when calculating average time
     * @param windowSize                  only supports fixed size window, not time-based
     * @return the configured HedgeDurationSupplier
     */
    static HedgeDurationSupplier ofAveragePlus(boolean shouldUseFactorAsPercentage, int factor, boolean shouldMeasureErrors, int windowSize) {
        return new AverageDurationSupplier(shouldUseFactorAsPercentage, factor, shouldMeasureErrors, windowSize);
    }

    /**
     * Creates a simple Duration Supplier that returns a pre-defined delay before hedging.
     *
     * @param cutoff the preconfigured cutoff time as a Duration
     * @return the configured HedgeDurationSupplier
     */
    static HedgeDurationSupplier ofPreconfigured(Duration cutoff) {
        return new PreconfiguredDurationSupplier(cutoff);
    }

    /**
     * accepts events and uses them to perform computation, resulting in the proposed Duration.
     * @param type     indicates whether the result is primary or not, and successful or not
     * @param duration duration it took to complete the hedged call
     */
    void accept(HedgeEvent.Type type, Duration duration);

}
