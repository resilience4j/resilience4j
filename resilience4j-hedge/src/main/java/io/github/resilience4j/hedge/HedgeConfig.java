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


import io.github.resilience4j.hedge.internal.NamingThreadFactory;

import java.io.Serializable;
import java.time.Duration;
import java.util.concurrent.*;

/**
 * HedgeConfig manages the configuration of Hedges
 */
public class HedgeConfig implements Serializable {

    private static final long serialVersionUID = 2203981592465761602L;

    private static final String HEDGE_DURATION_MUST_NOT_BE_NULL = "HedgeDuration must not be null";
    private final MetricsType metricsType;
    private final boolean shouldUseFactorAsPercentage;
    private final int hedgeTimeFactor;
    private final boolean shouldMeasureErrors;
    private final int windowSize;
    private final Duration cutoff;
    private final ScheduledExecutorService hedgeExecutor;

    private HedgeConfig(MetricsType metricsType, boolean shouldUseFactorAsPercentage, int hedgeTimeFactor, boolean shouldMeasureErrors, int windowSize, Duration cutoff, ScheduledExecutorService executor) {
        this.metricsType = metricsType;
        this.shouldUseFactorAsPercentage = shouldUseFactorAsPercentage;
        this.hedgeTimeFactor = hedgeTimeFactor;
        this.shouldMeasureErrors = shouldMeasureErrors;
        this.windowSize = windowSize;
        this.cutoff = cutoff;
        this.hedgeExecutor = executor;
    }

    /**
     * Returns a builder to create a custom HedgeConfig.
     *
     * @return a {@link HedgeConfig.Builder}
     */
    public static Builder custom() {
        return new Builder();
    }

    public static Builder from(HedgeConfig baseConfig) {
        return new Builder(baseConfig);
    }

    /**
     * Creates a default Hedge configuration.
     *
     * @return a default Hedge configuration.
     */
    public static HedgeConfig ofDefaults() {
        return new Builder().build();
    }

    public HedgeMetrics newMetrics() {
        if (metricsType == MetricsType.PRECONFIGURED) {
            return HedgeMetrics.ofPreconfigured(cutoff);
        } else {
            return HedgeMetrics.ofAveragePlus(shouldUseFactorAsPercentage, hedgeTimeFactor,
                shouldMeasureErrors, windowSize);
        }
    }

    public ScheduledExecutorService getHedgeExecutor() {
        return hedgeExecutor;
    }

    @Override
    public String toString() {
        return "HedgeConfig{" +
            shouldUseFactorAsPercentage + "," +
            hedgeTimeFactor + "," +
            shouldMeasureErrors + "," +
            windowSize + "," +
            cutoff +
            "}";
    }


    public enum MetricsType {
        PRECONFIGURED, AVERAGE_PLUS
    }

    public static class Builder {

        private MetricsType metricsType = MetricsType.PRECONFIGURED;
        private boolean shouldUseFactorAsPercentage = false;
        private int hedgeTimeFactor = 0;
        private boolean shouldMeasureErrors = true;
        private int windowSize = 100;
        private Duration cutoff;
        private RejectedExecutionHandler rejectionHandler = new ThreadPoolExecutor.DiscardOldestPolicy();
        private int concurrentHedges = 10;
        private String threadsNamed = "HedgeThreadPool";
        private ScheduledExecutorService providedExecutor;

        public Builder() {
        }

        public Builder(HedgeConfig baseConfig) {
            this.metricsType = baseConfig.metricsType;
            this.shouldUseFactorAsPercentage = baseConfig.shouldUseFactorAsPercentage;
            this.hedgeTimeFactor = baseConfig.hedgeTimeFactor;
            this.shouldMeasureErrors = baseConfig.shouldMeasureErrors;
            this.windowSize = baseConfig.windowSize;
            this.cutoff = baseConfig.cutoff;
            this.providedExecutor = baseConfig.hedgeExecutor;
        }

        public static Builder fromConfig(HedgeConfig baseConfig) {
            return new Builder(baseConfig);
        }

        /**
         * Builds a HedgeConfig
         *
         * @return the HedgeConfig
         */
        public HedgeConfig build() {
            if (providedExecutor == null) {
                providedExecutor = new ScheduledThreadPoolExecutor(
                    concurrentHedges,
                    new NamingThreadFactory(threadsNamed),
                    rejectionHandler);
            }

            return new HedgeConfig(
                metricsType,
                shouldUseFactorAsPercentage,
                hedgeTimeFactor,
                shouldMeasureErrors,
                windowSize,
                cutoff,
                providedExecutor
            );
        }

        public Builder averagePlusPercentMetrics(int hedgeTimeFactor, boolean shouldMeasureErrors) {
            this.metricsType = MetricsType.AVERAGE_PLUS;
            this.shouldUseFactorAsPercentage = true;
            this.hedgeTimeFactor = hedgeTimeFactor;
            this.shouldMeasureErrors = shouldMeasureErrors;
            return this;
        }

        public Builder averagePlusAmountMetrics(int amount, boolean shouldMeasureErrors, int windowSize) {
            this.metricsType = MetricsType.AVERAGE_PLUS;
            this.shouldUseFactorAsPercentage = false;
            this.hedgeTimeFactor = amount;
            this.shouldMeasureErrors = shouldMeasureErrors;
            this.windowSize = windowSize;
            return this;
        }

        public Builder preconfiguredMetrics(Duration cutoff) {
            if (cutoff == null) {
                throw new NullPointerException(HEDGE_DURATION_MUST_NOT_BE_NULL);
            }
            this.metricsType = MetricsType.PRECONFIGURED;
            this.cutoff = cutoff;
            return this;
        }

        public Builder withProvidedExecutor(ScheduledExecutorService executor) {
            this.providedExecutor = executor;
            return this;
        }

        public Builder withConfiguredExecutor(int concurrentHedges, String threadsNamed, RejectedExecutionHandler rejectionHandler) {
            this.concurrentHedges = concurrentHedges;
            this.threadsNamed = threadsNamed;
            this.rejectionHandler = rejectionHandler;
            return this;
        }
    }
}
