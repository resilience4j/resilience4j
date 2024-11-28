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
package io.github.resilience4j.core.metrics;

import java.util.concurrent.TimeUnit;

/**
 * Interface for measurement implementations that accumulate calls durations and outcomes.
 */
public interface CumulativeMeasurement extends MeasurementData {

    /**
     * Records a call duration and its outcome.
     *
     * @param duration the call duration
     * @param durationUnit the time unit of the call duration
     * @param outcome the outcome of the call
     */
    void record(long duration, TimeUnit durationUnit, Metrics.Outcome outcome);
}
