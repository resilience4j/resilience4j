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
import java.util.concurrent.atomic.LongAdder;

class AbstractAggregation {

    LongAdder totalDurationInMillis = new LongAdder();
    LongAdder numberOfSlowCalls = new LongAdder();
    LongAdder numberOfSlowFailedCalls = new LongAdder();
    LongAdder numberOfFailedCalls = new LongAdder();
    LongAdder numberOfCalls = new LongAdder();

    void record(long duration, TimeUnit durationUnit, Metrics.Outcome outcome) {
        this.numberOfCalls.add(1);
        this.totalDurationInMillis.add(durationUnit.toMillis(duration));
        switch (outcome) {
            case SLOW_SUCCESS:
                numberOfSlowCalls.add(1);
                break;

            case SLOW_ERROR:
                numberOfSlowCalls.add(1);
                numberOfFailedCalls.add(1);
                numberOfSlowFailedCalls.add(1);
                break;

            case ERROR:
                numberOfFailedCalls.add(1);
                break;
        }
    }
}
