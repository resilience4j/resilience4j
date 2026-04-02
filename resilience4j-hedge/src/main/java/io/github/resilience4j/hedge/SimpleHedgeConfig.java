package io.github.resilience4j.hedge;

import io.github.resilience4j.hedge.HedgeConfig.HedgeDurationSupplierType;

import java.io.Serializable;
import java.time.Duration;

public class SimpleHedgeConfig implements Serializable {
    protected static final String HEDGE_DURATION_MUST_NOT_BE_NULL = "HedgeDuration must not be null";
    protected final int concurrentHedges;
    protected final HedgeDurationSupplierType durationSupplierType;
    protected final boolean shouldUseFactorAsPercentage;
    protected final int hedgeTimeFactor;
    protected final boolean shouldMeasureErrors;
    protected final int windowSize;
    protected final Duration cutoff;

    public SimpleHedgeConfig(int concurrentHedges, HedgeDurationSupplierType durationSupplierType, boolean shouldUseFactorAsPercentage, int hedgeTimeFactor, boolean shouldMeasureErrors, int windowSize, Duration cutoff) {
        this.concurrentHedges = concurrentHedges;
        this.durationSupplierType = durationSupplierType;
        this.shouldUseFactorAsPercentage = shouldUseFactorAsPercentage;
        this.hedgeTimeFactor = hedgeTimeFactor;
        this.shouldMeasureErrors = shouldMeasureErrors;
        this.windowSize = windowSize;
        this.cutoff = cutoff;
    }

    public static SimpleHedgeConfig ofDefaults() {
        return new Builder<>().build();
}

    public int getConcurrentHedges() {
        return concurrentHedges;
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

    @SuppressWarnings("unchecked")
    public static class Builder<T extends Builder<T>> {

        protected HedgeDurationSupplierType hedgeDurationSupplierType = HedgeDurationSupplierType.PRECONFIGURED;
        protected boolean shouldUseFactorAsPercentage = false;
        protected int hedgeTimeFactor = 0;
        protected boolean shouldMeasureErrors = true;
        protected int windowSize = 100;
        protected Duration cutoff;
        protected int concurrentHedges = 10;

        public Builder() {
        }

        public Builder(SimpleHedgeConfig baseConfig) {
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
        public SimpleHedgeConfig build() {
            return new SimpleHedgeConfig(
                concurrentHedges,
                hedgeDurationSupplierType,
                shouldUseFactorAsPercentage,
                hedgeTimeFactor,
                shouldMeasureErrors,
                windowSize,
                cutoff
            );
        }

        public T averagePlusPercentageDuration(int percentageAsInteger, boolean shouldMeasureErrors) {
            this.hedgeDurationSupplierType = HedgeDurationSupplierType.AVERAGE_PLUS;
            this.shouldUseFactorAsPercentage = true;
            this.hedgeTimeFactor = percentageAsInteger;
            this.shouldMeasureErrors = shouldMeasureErrors;
            return (T) this;
        }

        public T averagePlusAmountDuration(int amount, boolean shouldMeasureErrors, int windowSize) {
            this.hedgeDurationSupplierType = HedgeDurationSupplierType.AVERAGE_PLUS;
            this.shouldUseFactorAsPercentage = false;
            this.hedgeTimeFactor = amount;
            this.shouldMeasureErrors = shouldMeasureErrors;
            this.windowSize = windowSize;
            return (T) this;
        }

        public T preconfiguredDuration(Duration cutoff) {
            if (cutoff == null) {
                throw new NullPointerException(HEDGE_DURATION_MUST_NOT_BE_NULL);
            }
            this.hedgeDurationSupplierType = HedgeDurationSupplierType.PRECONFIGURED;
            this.cutoff = cutoff;
            return (T) this;
        }

        public T withMaxConcurrency(int concurrentHedges) {
            this.concurrentHedges = concurrentHedges;
            return (T) this;
        }
    }
}
