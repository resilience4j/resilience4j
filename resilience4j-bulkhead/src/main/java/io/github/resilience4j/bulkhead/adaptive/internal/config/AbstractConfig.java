package io.github.resilience4j.bulkhead.adaptive.internal.config;

import java.util.Objects;

import io.github.resilience4j.core.lang.NonNull;

/**
 * @author romeh
 */
public class AbstractConfig {
	protected double desirableAverageThroughput = 3; // in req/sec
	protected double desirableOperationLatency = 0.1d; // in sec/op
	protected double maxAcceptableRequestLatency = desirableOperationLatency * 1.3d; // in sec/op
	protected int windowForAdaptation = 50;
	protected int windowForReconfiguration = 900;
	protected double lowLatencyMultiplier = 0.8d;
	protected double concurrencyDropMultiplier = 0.85d;


	public double getConcurrencyDropMultiplier() {
		return concurrencyDropMultiplier;
	}

	public double getLowLatencyMultiplier() {
		return lowLatencyMultiplier;
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

	@NonNull
	public int getWindowForAdaptation() {
		return windowForAdaptation;
	}

	@NonNull
	public int getWindowForReconfiguration() {
		return windowForReconfiguration;
	}


	@Override
	public String toString() {
		return "AbstractConfig{" +
				"desirableAverageThroughput=" + desirableAverageThroughput +
				", desirableOperationLatency=" + desirableOperationLatency +
				", maxAcceptableRequestLatency=" + maxAcceptableRequestLatency +
				", windowForAdaptation=" + windowForAdaptation +
				", windowForReconfiguration=" + windowForReconfiguration +
				", lowLatencyMultiplier=" + lowLatencyMultiplier +
				", concurrencyDropMultiplier=" + concurrencyDropMultiplier +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AbstractConfig that = (AbstractConfig) o;
		return Double.compare(that.desirableAverageThroughput, desirableAverageThroughput) == 0 &&
				Double.compare(that.desirableOperationLatency, desirableOperationLatency) == 0 &&
				Double.compare(that.maxAcceptableRequestLatency, maxAcceptableRequestLatency) == 0 &&
				Double.compare(that.lowLatencyMultiplier, lowLatencyMultiplier) == 0 &&
				Double.compare(that.concurrencyDropMultiplier, concurrencyDropMultiplier) == 0 &&
				Objects.equals(windowForAdaptation, that.windowForAdaptation) &&
				Objects.equals(windowForReconfiguration, that.windowForReconfiguration);

	}

	@Override
	public int hashCode() {
		return Objects.hash(desirableAverageThroughput, desirableOperationLatency, maxAcceptableRequestLatency, windowForAdaptation, windowForReconfiguration, lowLatencyMultiplier, concurrencyDropMultiplier);
	}
}
