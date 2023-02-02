/*
 *
 *  Copyright 2019 Robert Winkler and Bohdan Storozhuk
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

public interface Metrics {

    /**
     * Records a call.
     *
     * @param duration     the duration of the call
     * @param durationUnit the time unit of the duration
     * @param outcome      the outcome of the call
     */
    Snapshot record(long duration, TimeUnit durationUnit, Outcome outcome);

    /**
     * Returns a snapshot.
     *
     * @return a snapshot
     */
    Snapshot getSnapshot();

    void resetRecords();

    enum Outcome {
        SUCCESS, ERROR, SLOW_SUCCESS, SLOW_ERROR;

        public static Outcome of(boolean slow, boolean success) {
            if (success) {
                return slow ? Outcome.SLOW_SUCCESS : Outcome.SUCCESS;
            } else {
                return slow ? Outcome.SLOW_ERROR : Outcome.ERROR;
            }
        }
    }

}