/*
 *
 *  Copyright 2021: Tomasz Skowroński
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
package io.github.resilience4j.bulkhead.adaptive.internal;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;

/**
 * Calculates concurrency limit, a new value of inner bulkhead MaxConcurrentCalls.
 */
class AdaptationCalculator {

    private final AdaptiveBulkheadConfig adaptiveBulkheadConfig;
    private final Bulkhead innerBulkhead;

    AdaptationCalculator(AdaptiveBulkheadConfig adaptiveBulkheadConfig,
        Bulkhead innerBulkhead) {
        this.adaptiveBulkheadConfig = adaptiveBulkheadConfig;
        this.innerBulkhead = innerBulkhead;
    }

    int increment() {
        return fitToRange(
            getActualConcurrencyLimit() + adaptiveBulkheadConfig.getIncreaseSummand());
    }

    int increase() {
        return fitToRange(
            (int) (getActualConcurrencyLimit() * adaptiveBulkheadConfig.getIncreaseMultiplier()));
    }

    int decrease() {
        return fitToRange(
            (int) (getActualConcurrencyLimit() * adaptiveBulkheadConfig.getDecreaseMultiplier()));
    }

    private int fitToRange(int concurrencyLimitProposal) {
        return Math.min(adaptiveBulkheadConfig.getMaxConcurrentCalls(),
            Math.max(adaptiveBulkheadConfig.getMinConcurrentCalls(), concurrencyLimitProposal));
    }

    private int getActualConcurrencyLimit() {
        return innerBulkhead.getBulkheadConfig().getMaxConcurrentCalls();
    }
}
