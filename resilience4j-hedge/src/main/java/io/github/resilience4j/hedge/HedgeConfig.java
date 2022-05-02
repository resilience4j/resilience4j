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

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.resilience4j.core.ClassUtils;
import io.github.resilience4j.core.ContextPropagator;
import io.github.resilience4j.core.lang.Nullable;

/**
 * HedgeConfig manages the configuration of Hedges
 */
public class HedgeConfig implements Serializable {

    private static final long serialVersionUID = 2203981592465761602L;

    private static final String HEDGE_DURATION_MUST_NOT_BE_NULL = "HedgeDuration must not be null";
    private final int concurrentHedges;
    private final HedgeDurationSupplierType durationSupplierType;
    private final boolean shouldUseFactorAsPercentage;
    private final int hedgeTimeFactor;
    private final boolean shouldMeasureErrors;
    private final int windowSize;
    private final Duration cutoff;
    private final transient ContextPropagator[] contextPropagators;

    private HedgeConfig(int concurrentHedges, HedgeDurationSupplierType durationSupplierType, boolean shouldUseFactorAsPercentage, int hedgeTimeFactor, boolean shouldMeasureErrors, int windowSize, Duration cutoff, ContextPropagator[] propagators) {
        this.concurrentHedges = concurrentHedges;
        this.durationSupplierType = durationSupplierType;
        this.shouldUseFactorAsPercentage = shouldUseFactorAsPercentage;
        this.hedgeTimeFactor = hedgeTimeFactor;
        this.shouldMeasureErrors = shouldMeasureErrors;
        this.windowSize = windowSize;
        this.cutoff = cutoff;
        this.contextPropagators = propagators;
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

    public ContextPropagator[] getContextPropagators() {
        return contextPropagators;
    }

    public int getConcurrentHedges() {
        return concurrentHedges;
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

    public HedgeDurationSupplierType getDurationSupplier() {
        return durationSupplierType;
    }

    public boolean isShouldUseFactorAsPercentage() {
        return shouldUseFactorAsPercentage;
    }

    public int getHedgeTimeFactor() {
        return hedgeTimeFactor;
    }

    public boolean isShouldMeasureErrors() {
        return shouldMeasureErrors;
    }

    public int getWindowSize() {
        return windowSize;
    }

    public Duration getCutoff() {
        return cutoff;
    }

    public enum HedgeDurationSupplierType {
        PRECONFIGURED, AVERAGE_PLUS
    }

    public static class Builder {

        private HedgeDurationSupplierType hedgeDurationSupplierType = HedgeDurationSupplierType.PRECONFIGURED;
        private boolean shouldUseFactorAsPercentage = false;
        private int hedgeTimeFactor = 0;
        private boolean shouldMeasureErrors = true;
        private int windowSize = 100;
        private Duration cutoff;
        private int concurrentHedges = 10;
        private Class<? extends ContextPropagator>[] contextPropagatorClasses = new Class[0];
        private List<? extends ContextPropagator> contextPropagators = new ArrayList<>();

        public Builder() {
        }

        public Builder(HedgeConfig baseConfig) {
            this.shouldUseFactorAsPercentage = baseConfig.shouldUseFactorAsPercentage;
            this.hedgeTimeFactor = baseConfig.hedgeTimeFactor;
            this.shouldMeasureErrors = baseConfig.shouldMeasureErrors;
            this.windowSize = baseConfig.windowSize;
            this.cutoff = baseConfig.cutoff;
            this.concurrentHedges = baseConfig.concurrentHedges;
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
            final List<ContextPropagator> propagators = new ArrayList<>();

            if (contextPropagatorClasses.length > 0) {
                propagators.addAll(stream(contextPropagatorClasses)
                    .map(ClassUtils::instantiateClassDefConstructor)
                    .collect(toList()));
            }
            //setting bean of type context propagator overrides the class type.
            if (!contextPropagators.isEmpty()) {
                propagators.addAll(this.contextPropagators);
            }
            ContextPropagator[] propArray = new ContextPropagator[propagators.size()];
            propagators.toArray(propArray);
            return new HedgeConfig(
                concurrentHedges,
                hedgeDurationSupplierType,
                shouldUseFactorAsPercentage,
                hedgeTimeFactor,
                shouldMeasureErrors,
                windowSize,
                cutoff,
                propArray
            );
        }

        public Builder averagePlusPercentageDuration(int percentageAsInteger, boolean shouldMeasureErrors) {
            this.hedgeDurationSupplierType = HedgeDurationSupplierType.AVERAGE_PLUS;
            this.shouldUseFactorAsPercentage = true;
            this.hedgeTimeFactor = percentageAsInteger;
            this.shouldMeasureErrors = shouldMeasureErrors;
            return this;
        }

        public Builder averagePlusAmountDuration(int amount, boolean shouldMeasureErrors, int windowSize) {
            this.hedgeDurationSupplierType = HedgeDurationSupplierType.AVERAGE_PLUS;
            this.shouldUseFactorAsPercentage = false;
            this.hedgeTimeFactor = amount;
            this.shouldMeasureErrors = shouldMeasureErrors;
            this.windowSize = windowSize;
            return this;
        }

        public Builder preconfiguredDuration(Duration cutoff) {
            if (cutoff == null) {
                throw new NullPointerException(HEDGE_DURATION_MUST_NOT_BE_NULL);
            }
            this.hedgeDurationSupplierType = HedgeDurationSupplierType.PRECONFIGURED;
            this.cutoff = cutoff;
            return this;
        }

        /**
         * Configures the context propagator classes.
         *
         * @param contextPropagatorClasses the context propagators to pass to the {@link io.github.resilience4j.core.ContextAwareScheduledThreadPoolExecutor}
         * @return the HedgeConfig.Builder
         */
        public final Builder withContextPropagators(
            @Nullable Class<? extends ContextPropagator>... contextPropagatorClasses) {
            this.contextPropagatorClasses = contextPropagatorClasses != null
                ? contextPropagatorClasses
                : new Class[0];
            return this;
        }

        public final Builder withContextPropagators(ContextPropagator... contextPropagators) {
            this.contextPropagators = contextPropagators != null ?
                Arrays.stream(contextPropagators).collect(toList()) :
                new ArrayList<>();
            return this;
        }

        public Builder withMaxConcurrency(int concurrentHedges) {
            this.concurrentHedges = concurrentHedges;
            return this;
        }
    }
}
