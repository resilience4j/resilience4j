package io.github.resilience4j.bulkhead;

import java.time.Duration;

import static java.lang.Double.NaN;
import static java.lang.Double.isNaN;

public class BulkheadAdaptationConfig {
    private double desirableAverageThroughput = NaN; // in req/sec
    private double desirableOperationLatency = NaN; // in sec/op
    private double maxAcceptableRequestLatency = NaN; // in sec/op
    private Duration windowForAdaptation = null;
    private Duration windowForReconfiguration = null;

    private BulkheadAdaptationConfig() {
    }

    public double getDesirableAverageThroughput() {
        return desirableAverageThroughput;
    }

    public double getDesirableOperationLatency() {
        return desirableOperationLatency;
    }

    public double getMaxAcceptableRequestLatency() {
        return maxAcceptableRequestLatency;
    }

    public Duration getWindowForAdaptation() {
        return windowForAdaptation;
    }

    public Duration getWindowForReconfiguration() {
        return windowForReconfiguration;
    }

    public static class Builder {
        private BulkheadAdaptationConfig config = new BulkheadAdaptationConfig();

        /**
         * Desirable average throughput in op/second.
         * This param will provide us with initial configuration.
         * The closer it to the real value - faster we can figure out real concurrency limits.
         *
         * @param desirableAverageThroughput - in op/sec
         * @return a {@link Builder}
         */
        public Builder desirableAverageThroughput(double desirableAverageThroughput) {
            if (desirableAverageThroughput <= 0.0) {
                throw new IllegalArgumentException("desirableAverageThroughput must be a positive value greater than zero");
            }
            config.desirableAverageThroughput = desirableAverageThroughput;
            return this;
        }

        /**
         * Desirable operation latency in seconds/operation.
         * This is our foothold that we will circling around.
         * System will constantly measure actual average latency and compare it with "desirableOperationLatency".
         * If you actual latency will be lower than "desirableOperationLatency",
         * will calculate the difference and use it as {@link BulkheadConfig}.maxWaitTime
         * If you actual latency will be higher than "desirableOperationLatency" TODO: describe behaviour.
         *
         * @param desirableOperationLatency - in sec/op
         * @return a {@link Builder}
         */
        public Builder desirableOperationLatency(double desirableOperationLatency) {
            if (desirableOperationLatency <= 0.0) {
                throw new IllegalArgumentException("desirableOperationLatency must be a positive value greater than zero");
            }
            config.desirableOperationLatency = desirableOperationLatency;
            return this;
        }

        /**
         * Maximum acceptable operation latency in seconds/operation.
         * This number should be set wisely, because it can eliminate all adaptive capabilities,
         * system will do its best to never reach such latency,
         * so you can set it 20-30 % higher than your usual average latency.
         * If you actual latency will be higher than "maxAcceptableRequestLatency" TODO: describe behaviour.
         * <p>
         * Default value is {@link BulkheadAdaptationConfig}.desirableOperationLatency * 1.3
         *
         * @param maxAcceptableRequestLatency - in sec/op
         * @return a {@link Builder}
         */
        public Builder maxAcceptableRequestLatency(double maxAcceptableRequestLatency) {
            if (maxAcceptableRequestLatency <= 0.0) {
                throw new IllegalArgumentException("maxAcceptableRequestLatency must be a positive value greater than zero");
            }
            config.maxAcceptableRequestLatency = maxAcceptableRequestLatency;
            return this;
        }

        /**
         * Window size for adaptation.
         * After each cycle with duration of "windowForAdaptation" will calculate current average latency
         * from adaptation window and will try to adapt concurrency level.
         *
         * @param windowForAdaptation - duration
         * @return a {@link Builder}
         */
        public Builder windowForAdaptation(Duration windowForAdaptation) {
            config.windowForAdaptation = windowForAdaptation;
            return this;
        }

        /**
         * Window size for reconfiguration.
         * After each cycle with duration of "windowForReconfiguration" will calculate standard deviation of latencies
         * after different adaptations and will recalculate local "maxAcceptableRequestLatency" for the next cycle.
         * This will help us handle daily latency changes gracefully without reaching "maxAcceptableRequestLatency" often.
         *
         * @param windowForReconfiguration - duration
         * @return a {@link Builder}
         */
        public Builder windowForReconfiguration(Duration windowForReconfiguration) {
            config.windowForReconfiguration = windowForReconfiguration;
            return this;
        }

        /**
         * Builds a BulkheadAdaptationConfig
         *
         * @return the BulkheadAdaptationConfig
         */
        public BulkheadAdaptationConfig build() {
            if (isNaN(config.desirableAverageThroughput)) {
                throw new IllegalArgumentException("desirableAverageThroughput can't be NaN");
            }
            if (isNaN(config.maxAcceptableRequestLatency)) {
                throw new IllegalArgumentException("maxAcceptableRequestLatency can't be NaN");
            }
            if (isNaN(config.desirableOperationLatency)) {
                throw new IllegalArgumentException("desirableOperationLatency can't be NaN");
            }
            if (config.windowForReconfiguration == null) {
                throw new IllegalArgumentException("windowForReconfiguration can't be null");
            }
            if (config.windowForAdaptation == null) {
                throw new IllegalArgumentException("windowForAdaptation can't be null");
            }

            if (config.maxAcceptableRequestLatency < config.desirableOperationLatency) {
                throw new IllegalArgumentException("maxAcceptableRequestLatency can't be less" +
                        " than desirableOperationLatency");
            }

            if (config.windowForAdaptation.toNanos() < (long) (config.desirableAverageThroughput * 15 * 1000_000_000)) {
                throw new IllegalArgumentException("windowForAdaptation is too small. " +
                        "We wan't to make at least 15 measurements during this window.");
            }
            if (15 < config.windowForReconfiguration.toNanos() / config.windowForAdaptation.toNanos()) {
                throw new IllegalArgumentException("windowForReconfiguration is too small. " +
                        "windowForReconfiguration should be at least 15 times bigger than windowForAdaptation.");
            }

            return config;
        }
    }
}
