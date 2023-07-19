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

import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;

class AdaptationCalculator {

    private final AdaptiveBulkheadConfig config;
    private final AdaptiveBulkhead.Metrics metrics;

    AdaptationCalculator(AdaptiveBulkheadConfig config, AdaptiveBulkhead.Metrics metrics) {
        this.config = config;
        this.metrics = metrics;
    }

    /**
     * additive increase
     *
     * @return new concurrency limit to apply
     */
    int increment() {
        return fitToRange(
            metrics.getMaxAllowedConcurrentCalls() + config.getIncreaseSummand());
    }

    /**
     * multiplicative increase
     *
     * @return new concurrency limit to apply
     */
    int increase() {
        return fitToRange(
            (int) Math.ceil(metrics.getMaxAllowedConcurrentCalls() * config.getIncreaseMultiplier()));
    }

    /**
     * multiplicative decrease
     *
     * @return new concurrency limit to apply
     */
    int decrease() {
        return fitToRange(
            (int) Math.floor(metrics.getMaxAllowedConcurrentCalls() * config.getDecreaseMultiplier()));
    }

    private int fitToRange(int proposal) {
        if (proposal < config.getMinConcurrentCalls()) {
            return config.getMinConcurrentCalls();
        } else {
            return Math.min(proposal, config.getMaxConcurrentCalls());
        }
    }
}
