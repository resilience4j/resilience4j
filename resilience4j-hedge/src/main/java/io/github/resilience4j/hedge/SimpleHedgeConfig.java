package io.github.resilience4j.hedge;

import io.github.resilience4j.hedge.HedgeConfig.HedgeDurationSupplierType;

import java.io.Serializable;
import java.time.Duration;

/**
 * Base Hedge configuration.
 */
public class SimpleHedgeConfig implements Serializable {
    protected static final String HEDGE_DURATION_MUST_NOT_BE_NULL = "HedgeDuration must not be null";
    protected final int concurrentHedges;
    protected final HedgeDurationSupplierType durationSupplierType;
    protected final boolean shouldUseFactorAsPercentage;
    protected final int hedgeTimeFactor;
    protected final boolean shouldMeasureErrors;
    protected final int windowSize;
    protected final Duration cutoff;

    /**
     * Constructor for SimpleHedgeConfig.
     *
     * @param concurrentHedges          the number of concurrent hedges allowed
     * @param durationSupplierType      the type of duration supplier
     * @param shouldUseFactorAsPercentage whether to use factor as percentage
     * @param hedgeTimeFactor           the hedge time factor
     * @param shouldMeasureErrors       whether to measure errors
     * @param windowSize                the window size for error measurement
     * @param cutoff                    the cutoff duration
     */
    public SimpleHedgeConfig(int concurrentHedges, HedgeDurationSupplierType durationSupplierType, boolean shouldUseFactorAsPercentage, int hedgeTimeFactor, boolean shouldMeasureErrors, int windowSize, Duration cutoff) {
        this.concurrentHedges = concurrentHedges;
        this.durationSupplierType = durationSupplierType;
        this.shouldUseFactorAsPercentage = shouldUseFactorAsPercentage;
        this.hedgeTimeFactor = hedgeTimeFactor;
        this.shouldMeasureErrors = shouldMeasureErrors;
        this.windowSize = windowSize;
        this.cutoff = cutoff;
    }

    /**
     * Creates a default SimpleHedge configuration.
     *
     * @return a default SimpleHedge configuration.
     */
    public static SimpleHedgeConfig ofDefaults() {
        return new Builder<>().build();
    }

    /**
     * Returns the maximum number of concurrent hedges.
     *
     * @return the maximum number of concurrent hedges.
     */
    public int getConcurrentHedges() {
        return concurrentHedges;
    }

    /**
     * Returns the type of duration supplier.
     *
     * @return the duration supplier type.
     */
    public HedgeDurationSupplierType getDurationSupplier() {
        return durationSupplierType;
    }

    /**
     * Returns true if the hedge time factor should be used as a percentage, false otherwise.
     *
     * @return true if the hedge time factor should be used as a percentage.
     */
    public boolean isShouldUseFactorAsPercentage() {
        return shouldUseFactorAsPercentage;
    }

    /**
     * Returns the hedge time factor.
     *
     * @return the hedge time factor.
     */
    public int getHedgeTimeFactor() {
        return hedgeTimeFactor;
    }

    /**
     * Returns true if errors should be measured, false otherwise.
     *
     * @return true if errors should be measured.
     */
    public boolean isShouldMeasureErrors() {
        return shouldMeasureErrors;
    }

    /**
     * Returns the window size for error measurement.
     *
     * @return the window size.
     */
    public int getWindowSize() {
        return windowSize;
    }

    /**
     * Returns the cutoff duration.
     *
     * @return the cutoff duration.
     */
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

        /**
         * Creates a builder for a SimpleHedgeConfig.
         */
        public Builder() {
        }

        /**
         * Creates a builder for a SimpleHedgeConfig from an existing SimpleHedgeConfig.
         *
         * @param baseConfig the base configuration to copy from.
         */
        public Builder(SimpleHedgeConfig baseConfig) {
            this.shouldUseFactorAsPercentage = baseConfig.shouldUseFactorAsPercentage;
            this.hedgeTimeFactor = baseConfig.hedgeTimeFactor;
            this.shouldMeasureErrors = baseConfig.shouldMeasureErrors;
            this.windowSize = baseConfig.windowSize;
            this.cutoff = baseConfig.cutoff;
            this.concurrentHedges = baseConfig.concurrentHedges;
        }

        /**
         * Creates a builder for a SimpleHedgeConfig from an existing HedgeConfig.
         *
         * @param baseConfig the base configuration to copy from.
         * @return a Builder
         */
        public static Builder fromConfig(HedgeConfig baseConfig) {
            return new Builder(baseConfig);
        }

        /**
         * Builds a SimpleHedgeConfig.
         *
         * @return the SimpleHedgeConfig.
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

        /**
         * Configures the Hedge to use an average plus percentage duration.
         *
         * @param percentageAsInteger the percentage as an integer.
         * @param shouldMeasureErrors whether to measure errors.
         * @return the Builder.
         */
        public T averagePlusPercentageDuration(int percentageAsInteger, boolean shouldMeasureErrors) {
            this.hedgeDurationSupplierType = HedgeDurationSupplierType.AVERAGE_PLUS;
            this.shouldUseFactorAsPercentage = true;
            this.hedgeTimeFactor = percentageAsInteger;
            this.shouldMeasureErrors = shouldMeasureErrors;
            return (T) this;
        }

        /**
         * Configures the Hedge to use an average plus amount duration.
         *
         * @param amount            the amount to add to the average.
         * @param shouldMeasureErrors whether to measure errors.
         * @param windowSize        the window size for error measurement.
         * @return the Builder.
         */
        public T averagePlusAmountDuration(int amount, boolean shouldMeasureErrors, int windowSize) {
            this.hedgeDurationSupplierType = HedgeDurationSupplierType.AVERAGE_PLUS;
            this.shouldUseFactorAsPercentage = false;
            this.hedgeTimeFactor = amount;
            this.shouldMeasureErrors = shouldMeasureErrors;
            this.windowSize = windowSize;
            return (T) this;
        }

        /**
         * Configures the Hedge to use a preconfigured duration.
         *
         * @param cutoff the cutoff duration.
         * @return the Builder.
         * @throws NullPointerException if cutoff is null.
         */
        public T preconfiguredDuration(Duration cutoff) {
            if (cutoff == null) {
                throw new NullPointerException(HEDGE_DURATION_MUST_NOT_BE_NULL);
            }
            this.hedgeDurationSupplierType = HedgeDurationSupplierType.PRECONFIGURED;
            this.cutoff = cutoff;
            return (T) this;
        }

        /**
         * Configures the maximum number of concurrent hedges.
         *
         * @param concurrentHedges the maximum number of concurrent hedges.
         * @return the Builder.
         */
        public T withMaxConcurrency(int concurrentHedges) {
            this.concurrentHedges = concurrentHedges;
            return (T) this;
        }
    }
}
