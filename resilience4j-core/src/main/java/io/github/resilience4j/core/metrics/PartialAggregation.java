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

import java.util.concurrent.atomic.AtomicLong;

public class PartialAggregation extends AbstractAggregation {

    private final AtomicLong epochSecond;

    PartialAggregation(long epochSecond) {
        this.epochSecond = new AtomicLong(epochSecond);
    }

    void setEpochSecond(long epochSecond) {
        this.epochSecond.set(epochSecond);
    }

    public long getEpochSecond() {
        return epochSecond.get();
    }
}